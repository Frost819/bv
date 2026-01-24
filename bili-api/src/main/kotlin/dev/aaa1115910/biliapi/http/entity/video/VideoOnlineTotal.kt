package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 视频在线观看人数
 *
 * @param total 在线观看人数（字符串格式）
 */
@Serializable
data class VideoOnlineTotal(
    @SerialName("total")
    val total: String
)
