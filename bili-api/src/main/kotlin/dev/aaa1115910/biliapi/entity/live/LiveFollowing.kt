package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 关注的直播间列表响应
 * API: https://api.live.bilibili.com/xlive/web-ucenter/user/following
 */
@Serializable
data class LiveFollowingResponse(
    val code: Int,
    val message: String,
    val data: LiveFollowingData? = null
)

@Serializable
data class LiveFollowingData(
    @SerialName("totalPage") val totalPage: Int = 0,
    val count: Int = 0,
    val list: List<LiveFollowingRoom> = emptyList()
)

@Serializable
data class LiveFollowingRoom(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    @SerialName("room_cover") val roomCover: String = "",
    @SerialName("cover_from_user") val coverFromUser: String = "",
    val face: String = "",
    @SerialName("text_small") val textSmall: String = "",
    @SerialName("live_status") val liveStatus: Int = 0,
    @SerialName("parent_area_id") val parentAreaId: Int = 0,
    @SerialName("area_v2_parent_name") val parentAreaName: String = "",
    @SerialName("area_id") val areaId: Int = 0,
    @SerialName("area_name_v2") val areaNameV2: String = "",
    @SerialName("area_name") val areaName: String = "",
    @SerialName("watched_show") val watchedShow: WatchedShow? = null
) {
    /** 是否正在直播 */
    val isLive: Boolean get() = liveStatus == 1

    /** 封面优先使用 room_cover，降级到 cover_from_user */
    val coverUrl: String
        get() = roomCover.ifBlank { coverFromUser }

    /** 解析中文数字格式（如 "1.2万"）为整数 */
    val onlineCount: Int
        get() = parseCnCount(textSmall)

    fun toLiveRoomItem(): LiveRoomItem = LiveRoomItem(
        roomId = roomId,
        uid = uid,
        title = title,
        uname = uname,
        online = onlineCount,
        userCover = coverUrl,
        cover = coverUrl,
        face = face,
        parentId = parentAreaId,
        parentName = parentAreaName,
        areaId = areaId,
        areaName = areaNameV2.ifBlank { areaName },
        watchedShow = watchedShow,
        liveStatus = liveStatus
    )

    companion object {
        /**
         * 解析中文计数格式（如 "1.2万"）为整数
         */
        private fun parseCnCount(text: String): Int {
            if (text.isBlank()) return 0
            val trimmed = text.trim()
            return when {
                trimmed.endsWith("万") -> {
                    val num = trimmed.removeSuffix("万").toDoubleOrNull() ?: return 0
                    (num * 10000).toInt()
                }
                trimmed.endsWith("亿") -> {
                    val num = trimmed.removeSuffix("亿").toDoubleOrNull() ?: return 0
                    (num * 100000000).toInt()
                }
                else -> trimmed.toIntOrNull() ?: 0
            }
        }
    }
}
