package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveRoomListResponse(
    val code: Int,
    val message: String,
    val data: LiveRoomListData = LiveRoomListData()
)

@Serializable
data class LiveRoomListData(
    val list: List<LiveRoomItem> = emptyList()
)

@Serializable
data class LiveRoomItem(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val online: Int = 0,
    @SerialName("user_cover") val userCover: String = "",
    @SerialName("system_cover") val systemCover: String = "",
    val cover: String = "",
    val face: String = "",
    @SerialName("parent_id") val parentId: Int = 0,
    @SerialName("parent_name") val parentName: String = "",
    @SerialName("area_id") val areaId: Int = 0,
    @SerialName("area_name") val areaName: String = "",
    @SerialName("watched_show") val watchedShow: WatchedShow? = null,
    /** 直播状态：1=直播中, 0=未开播。非序列化字段，由 toLiveRoomItem 注入 */
    @kotlinx.serialization.Transient val liveStatus: Int = 1
)

@Serializable
data class WatchedShow(
    val switch: Boolean = false,
    val num: Int = 0,
    @SerialName("text_small") val textSmall: String = "",
    @SerialName("text_large") val textLarge: String = "",
    val icon: String = "",
    @SerialName("icon_location") val iconLocation: Int = 0,
    @SerialName("icon_web") val iconWeb: String = ""
)
