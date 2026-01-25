package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.util.NetworkUtil
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object GithubApi {
    private var endPoint = "api.github.com"
    private const val OWNER = "fantasytyx"
    private const val REPO = "bv"
    private const val PROXY_URL = "https://ghfast.top/"
    private lateinit var client: HttpClient
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val isDebug get() = BuildConfig.DEBUG
    private val isAlpha get() = Prefs.updateAlpha
    private val logger = KotlinLogging.logger("GithubApi")

    init {
        createClient()
    }

    private fun createClient() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        client = HttpClient(OkHttp) {
            engine {
                this.preconfigured = okHttpClient
            }
            install(UserAgent) {
                agent = dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = endPoint
                }
            }
        }
    }

    private suspend fun getReleases(
        owner: String = OWNER,
        repo: String = REPO,
        pageSize: Int = 30,
        page: Int = 1
    ): List<Release> {
        val response = client.get("repos/$owner/$repo/releases") {
            parameter("per_page", pageSize)
            parameter("page", page)
        }.bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<List<Release>>(response)
    }

    private suspend fun getLatestRelease(
        owner: String = OWNER,
        repo: String = REPO
    ): Release {
        val response = client.get("repos/$owner/$repo/releases/latest").bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<Release>(response)
    }

    suspend fun getLatestPreReleaseBuild(): Release {
        var release: Release? = null
        var page = 1
        while (release == null) {
            val releases = getReleases(page = page)
            if (releases.isEmpty()) break
            release = releases.firstOrNull { it.isPreRelease }
            page++
        }
        return release ?: throw IllegalStateException("No pre-release found")
    }

    suspend fun getLatestReleaseBuild(): Release = getLatestRelease()

    suspend fun getLatestBuild(): Release =
        if (isAlpha) getLatestPreReleaseBuild() else getLatestReleaseBuild()

    private fun checkErrorMessage(data: String) {
        val responseElement = json.parseToJsonElement(data)
        if (responseElement !is JsonObject) return
        val responseObject = responseElement.jsonObject
        check(responseObject.size != 2 && responseObject["message"] == null) { responseObject["message"]!!.jsonPrimitive.content }
    }

    /**
     * 下载更新文件
     * 策略：大陆用户优先使用 ghfast.top 代理，非大陆用户优先使用直连
     * 代理格式：https://ghfast.top/{原下载地址}
     */
    suspend fun downloadUpdate(
        release: Release,
        file: File,
        downloadListener: ProgressListener
    ) {
        val downloadUrl =
            if (isDebug) release.assets.firstOrNull { it.name.contains("debug") }?.browserDownloadUrl
            else release.assets.firstOrNull { it.name.contains("alpha") || it.name.contains("release") }?.browserDownloadUrl
        downloadUrl ?: throw IllegalStateException("Didn't find download url")
        val isMainlandChina = NetworkUtil.isMainlandChina()

        if (isMainlandChina) {
            // 先尝试代理
            val proxyUrl = PROXY_URL + downloadUrl
            val proxyResult = runCatching {
                downloadFile(proxyUrl, file, downloadListener)
            }
            if (proxyResult.isSuccess) {
                logger.info { "Download successful via proxy" }
                return
            }
            logger.warn(proxyResult.exceptionOrNull()) { "Proxy download failed, trying direct connection" }

            // 代理失败，使用直连
            try {
                downloadFile(downloadUrl, file, downloadListener)
                logger.info { "Download successful via direct connection" }
            } catch (e: Exception) {
                logger.error(e) { "Direct download failed" }
                throw e
            }
        } else {
            // 先尝试直连
            val directResult = runCatching {
                downloadFile(downloadUrl, file, downloadListener)
            }

            if (directResult.isSuccess) {
                logger.info { "Download successful via direct connection" }
                return
            }

            logger.warn(directResult.exceptionOrNull()) { "Direct download failed, trying proxy" }

            // 直连失败，使用代理
            val proxyUrl = PROXY_URL + downloadUrl
            logger.info { "Trying proxy: $proxyUrl" }
            try {
                downloadFile(proxyUrl, file, downloadListener)
                logger.info { "Download successful via proxy" }
            } catch (e: Exception) {
                logger.error(e) { "Proxy download failed" }
                throw e
            }
        }
    }

    /**
     * 执行实际的文件下载
     */
    private suspend fun downloadFile(
        url: String,
        file: File,
        downloadListener: ProgressListener
    ) {
        client.prepareRequest {
            url(url)
            onDownload(downloadListener)
        }.execute { response ->
            response.bodyAsChannel().copyAndClose(file.writeChannel())
        }
    }

    /**
     * 测试代理连接
     * @return 测试结果信息
     */
    suspend fun testProxyConnection(): String {
        val results = StringBuilder()
        results.appendLine("=== 代理连接测试 ===")

        // 1. 测试直连 GitHub API
        results.appendLine("\n1. 测试直连 GitHub API:")
        var release: Release? = null
        try {
            release = getLatestBuild()
            results.appendLine("   成功! 最新版本: ${release.name}")
        } catch (e: Exception) {
            results.appendLine("   失败: ${e.message}")
        }

        // 2. 测试代理下载（使用实际的 APK 下载链接）
        results.appendLine("\n2. 测试代理下载 (ghfast.top):")
        try {
            // 获取实际的 APK 下载链接
            val downloadUrl = release?.assets
                ?.firstOrNull { it.name.endsWith(".apk") }
                ?.browserDownloadUrl
            
            if (downloadUrl == null) {
                results.appendLine("   未找到 APK 下载链接")
            } else {
                val proxyUrl = PROXY_URL + downloadUrl
                results.appendLine("   代理链接: $proxyUrl")
                
                val tempClient = HttpClient(OkHttp) {
                    engine {
                        val okHttp = OkHttpClient.Builder()
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .build()
                        this.preconfigured = okHttp
                    }
                    install(UserAgent) {
                        agent = dev.aaa1115910.biliapi.BiliApiConstants.USER_AGENT_WEB
                    }
                }
                // 使用 HEAD 请求测试代理是否可用（不下载整个文件）
                val response = tempClient.prepareRequest {
                    url(proxyUrl)
                    method = io.ktor.http.HttpMethod.Head
                }.execute { it }
                tempClient.close()
                
                if (response.status.value in 200..399) {
                    results.appendLine("   代理可用 ✓ (状态码: ${response.status.value})")
                } else {
                    results.appendLine("   代理响应异常 ✗ (状态码: ${response.status.value})")
                }
            }
        } catch (e: Exception) {
            results.appendLine("   代理失败: ${e.message}")
        }

        return results.toString()
    }
}
