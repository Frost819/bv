package dev.aaa1115910.biliapi.entity.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveAreaResponse(
    val code: Int,
    val msg: String,
    val message: String,
    val data: List<LiveAreaGroup> = emptyList()
)

@Serializable
data class LiveAreaGroup(
    val id: Int,
    val name: String,
    val list: List<LiveAreaItem> = emptyList()
)

@Serializable
data class LiveAreaItem(
    val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("old_area_id") val oldAreaId: String,
    val name: String,
    val pic: String,
    @SerialName("parent_name") val parentName: String,
    @SerialName("area_type") val areaType: Int
)
