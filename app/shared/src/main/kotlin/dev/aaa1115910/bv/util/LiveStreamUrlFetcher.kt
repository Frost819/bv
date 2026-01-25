package dev.aaa1115910.bv.util

import android.widget.Toast
import dev.aaa1115910.biliapi.entity.live.LiveRoomPlayInfoResponse
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

object LiveStreamUrlFetcher {
    private val logger = KotlinLogging.logger("LiveStreamUrlFetcher")
    private val liveRepository: LiveRepository by inject(LiveRepository::class.java)

    /**
     * 获取直播流URL
     * @param roomId 直播间ID
     * @return 直播流URL，如果未开播或获取失败则返回null
     */
    suspend fun fetchLiveStreamUrl(roomId: Int): LivePlayInfo? = withContext(Dispatchers.IO) {
        try {
            val response = liveRepository.getLiveRoomPlayInfo(roomId)
            
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

            // 解析播放URL
            val playUrl = parsePlayUrl(response)
            if (playUrl == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BVApp.context,
                        "无法获取播放地址",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext null
            }

            logger.info { "Successfully fetched live stream URL for room $roomId: $playUrl" }
            LivePlayInfo(
                roomId = roomId,
                streamUrl = playUrl,
                isLive = true
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

    /**
     * 解析播放URL，优先级: FLV > HLS
     */
    private fun parsePlayUrl(response: LiveRoomPlayInfoResponse): String? {
        val streams = response.data?.playUrlInfo?.playurl?.stream ?: return null
        
        // 优先查找 http_stream (FLV)
        val flvStream = streams.find { it.protocolName == "http_stream" }
        if (flvStream != null) {
            val url = buildUrlFromStream(flvStream)
            if (url != null) {
                logger.info { "Using FLV stream" }
                return url
            }
        }

        // 其次查找 http_hls (HLS)
        val hlsStream = streams.find { it.protocolName == "http_hls" }
        if (hlsStream != null) {
            val url = buildUrlFromStream(hlsStream)
            if (url != null) {
                logger.info { "Using HLS stream" }
                return url
            }
        }

        // 使用第一个可用的流
        for (stream in streams) {
            val url = buildUrlFromStream(stream)
            if (url != null) {
                logger.info { "Using fallback stream: ${stream.protocolName}" }
                return url
            }
        }

        return null
    }

    /**
     * 从流信息构建URL
     */
    private fun buildUrlFromStream(stream: dev.aaa1115910.biliapi.entity.live.LiveStream): String? {
        // 优先选择 fmp4，其次 ts，最后 flv
        val formatOrder = listOf("fmp4", "ts", "flv")
        
        for (formatName in formatOrder) {
            val format = stream.format.find { it.formatName == formatName }
            if (format != null && format.codec.isNotEmpty()) {
                val codec = format.codec.first()
                if (codec.urlInfo.isNotEmpty()) {
                    val urlInfo = codec.urlInfo.first()
                    val fullUrl = "${urlInfo.host}${codec.baseUrl}${urlInfo.extra}"
                    logger.debug { "Built URL with format $formatName: $fullUrl" }
                    return fullUrl
                }
            }
        }

        // 如果没有找到特定格式，使用第一个可用的
        for (format in stream.format) {
            if (format.codec.isNotEmpty()) {
                val codec = format.codec.first()
                if (codec.urlInfo.isNotEmpty()) {
                    val urlInfo = codec.urlInfo.first()
                    val fullUrl = "${urlInfo.host}${codec.baseUrl}${urlInfo.extra}"
                    logger.debug { "Built URL with fallback format ${format.formatName}: $fullUrl" }
                    return fullUrl
                }
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
    val isLive: Boolean = true
)
