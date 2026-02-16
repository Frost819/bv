package dev.aaa1115910.bv.util

import android.widget.Toast
import dev.aaa1115910.biliapi.entity.live.LiveCodec
import dev.aaa1115910.biliapi.entity.live.LiveRoomPlayInfoResponse
import dev.aaa1115910.biliapi.entity.live.LiveStream
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

object LiveStreamUrlFetcher {
    private val logger = KotlinLogging.logger("LiveStreamUrlFetcher")
    private val liveRepository: LiveRepository by inject(LiveRepository::class.java)

    // Codec 优先级: HEVC > AV1 > AVC
    private val codecPriority = listOf("hevc", "av1", "avc")

    /**
     * 获取直播流URL
     * @param roomId 直播间ID
     * @param qn 画质编号，默认30000（杜比，最高值），服务端会自动降级到实际最高可用画质
     * @return 直播播放信息，包含流URL和可用画质列表，如果未开播或获取失败则返回null
     */
    suspend fun fetchLiveStreamUrl(roomId: Int, qn: Int = 30000): LivePlayInfo? = withContext(Dispatchers.IO) {
        try {
            val sessData = Prefs.sessData
            val response = liveRepository.getLiveRoomPlayInfo(roomId, qn, sessData)
            
            if (response.code != 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "获取直播流失败: ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            val data = response.data
            if (data == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "获取直播流失败: 数据为空",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            // 检查直播状态
            if (data.liveStatus != 1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "主播未开播",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            // 构建画质描述映射表
            val qnDescMap = mutableMapOf<Int, String>()
            data.playUrlInfo?.playurl?.gQnDesc?.forEach { desc ->
                qnDescMap[desc.qn] = desc.desc
            }

            // 解析播放URL和画质信息
            val result = parsePlayUrl(response)
            if (result == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "无法获取播放地址",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            logger.info { "Successfully fetched live stream URL for room $roomId: ${result.url}" }
            logger.info { "Current qn: ${result.currentQn}, accept_qn: ${result.acceptQn}" }
            LivePlayInfo(
                roomId = roomId,
                streamUrl = result.url,
                isLive = true,
                currentQn = result.currentQn,
                acceptQn = result.acceptQn,
                qnDescMap = qnDescMap
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch live stream URL for room $roomId" }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    BVApp.context,
                    "获取直播流失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            null
        }
    }

    private data class ParseResult(
        val url: String,
        val currentQn: Int,
        val acceptQn: List<Int>
    )

    /**
     * 解析播放URL，优先级: HLS > FLV
     * 注意：必须优先 HLS，因为 FLV 协议只有 flv 格式，而 ExoPlayer 的 FLV 提取器
     * 不支持 HEVC (H.265)。HLS 协议提供 fmp4/ts 容器，可正常播放 HEVC。
     */
    private fun parsePlayUrl(response: LiveRoomPlayInfoResponse): ParseResult? {
        val streams = response.data?.playUrlInfo?.playurl?.stream ?: return null
        
        // 优先查找 http_hls (HLS) — 支持 HEVC/AV1 等现代编码
        val hlsStream = streams.find { it.protocolName == "http_hls" }
        if (hlsStream != null) {
            val result = buildUrlFromStream(hlsStream)
            if (result != null) {
                logger.info { "Using HLS stream" }
                return result
            }
        }

        // 其次查找 http_stream (FLV) — 仅支持 AVC (H.264)
        val flvStream = streams.find { it.protocolName == "http_stream" }
        if (flvStream != null) {
            val result = buildUrlFromStream(flvStream)
            if (result != null) {
                logger.info { "Using FLV stream" }
                return result
            }
        }

        // 使用第一个可用的流
        for (stream in streams) {
            val result = buildUrlFromStream(stream)
            if (result != null) {
                logger.info { "Using fallback stream: ${stream.protocolName}" }
                return result
            }
        }

        return null
    }

    /**
     * 从编解码器列表中按优先级选择最佳编解码器 (HEVC > AV1 > AVC)
     */
    private fun selectBestCodec(codecs: List<LiveCodec>): LiveCodec? {
        for (preferredCodec in codecPriority) {
            val codec = codecs.find { it.codecName == preferredCodec }
            if (codec != null && codec.urlInfo.isNotEmpty()) {
                logger.debug { "Selected codec: ${codec.codecName}" }
                return codec
            }
        }
        // 如果没有匹配的，取第一个有效的
        return codecs.firstOrNull { it.urlInfo.isNotEmpty() }
    }

    /**
     * 从流信息构建URL，按 codec 优先级选择编解码器
     */
    private fun buildUrlFromStream(stream: LiveStream): ParseResult? {
        // 优先选择 fmp4，其次 ts，最后 flv
        val formatOrder = listOf("fmp4", "ts", "flv")
        
        for (formatName in formatOrder) {
            val format = stream.format.find { it.formatName == formatName }
            if (format != null && format.codec.isNotEmpty()) {
                val codec = selectBestCodec(format.codec) ?: continue
                val urlInfo = codec.urlInfo.first()
                val fullUrl = "${urlInfo.host}${codec.baseUrl}${urlInfo.extra}"
                logger.debug { "Built URL with format $formatName, codec ${codec.codecName}: $fullUrl" }
                return ParseResult(
                    url = fullUrl,
                    currentQn = codec.currentQn,
                    acceptQn = codec.acceptQn
                )
            }
        }

        // 如果没有找到特定格式，使用第一个可用的
        for (format in stream.format) {
            if (format.codec.isNotEmpty()) {
                val codec = selectBestCodec(format.codec) ?: continue
                val urlInfo = codec.urlInfo.first()
                val fullUrl = "${urlInfo.host}${codec.baseUrl}${urlInfo.extra}"
                logger.debug { "Built URL with fallback format ${format.formatName}, codec ${codec.codecName}: $fullUrl" }
                return ParseResult(
                    url = fullUrl,
                    currentQn = codec.currentQn,
                    acceptQn = codec.acceptQn
                )
            }
        }

        return null
    }
}

/**
 * 直播播放信息
 */
data class LivePlayInfo(
    val roomId: Int,
    val streamUrl: String,
    val isLive: Boolean = true,
    val currentQn: Int = 0,
    val acceptQn: List<Int> = emptyList(),
    val qnDescMap: Map<Int, String> = emptyMap()
)
