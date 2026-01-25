package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveRoomListResponse(
    val code: Int,
    val message: String,
    val data: List<LiveRoomItem> = emptyList()
)

@Serializable
data class LiveRoomItem(
    @SerialName("roomid") val roomId: Int,
    val uid: Long,
    val title: String,
    val uname: String,
    val online: Int,
    @SerialName("user_cover") val userCover: String,
    @SerialName("system_cover") val systemCover: String,
    val cover: String,
    val face: String,
    @SerialName("parent_id") val parentId: Int,
    @SerialName("parent_name") val parentName: String,
    @SerialName("area_id") val areaId: Int,
    @SerialName("area_name") val areaName: String
)
