package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 推荐直播列表响应（主端点）
 * API: https://api.live.bilibili.com/xlive/web-interface/v1/webMain/getMoreRecList
 */
@Serializable
data class LiveRecommendResponse(
    val code: Int,
    val message: String,
    val data: LiveRecommendData? = null
)

@Serializable
data class LiveRecommendData(
    @SerialName("recommend_room_list") val recommendRoomList: List<LiveRecommendRoom> = emptyList()
)

@Serializable
data class LiveRecommendRoom(
    @SerialName("roomid") val roomId: Int = 0,
    val uid: Long = 0,
    val title: String = "",
    val uname: String = "",
    val online: Int = 0,
    @SerialName("user_cover") val userCover: String = "",
    val cover: String = "",
    val face: String = "",
    @SerialName("area_v2_parent_id") val parentAreaId: Int = 0,
    @SerialName("area_v2_parent_name") val parentAreaName: String = "",
    @SerialName("area_v2_id") val areaId: Int = 0,
    @SerialName("area_v2_name") val areaName: String = "",
    val keyframe: String = "",
    @SerialName("watched_show") val watchedShow: WatchedShow? = null
) {
    fun toLiveRoomItem(): LiveRoomItem = LiveRoomItem(
        roomId = roomId,
        uid = uid,
        title = title,
        uname = uname,
        online = online,
        userCover = userCover,
        cover = cover,
        face = face,
        parentId = parentAreaId,
        parentName = parentAreaName,
        areaId = areaId,
        areaName = areaName,
        watchedShow = watchedShow
    )
}
