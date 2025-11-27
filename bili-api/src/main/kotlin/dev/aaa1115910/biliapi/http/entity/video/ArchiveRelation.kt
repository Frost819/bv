package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 稿件互动状态
 */
@Serializable
data class ArchiveRelation(
    val coin: Int = 0,
    val dislike: Boolean = false,
    val favorite: Boolean = false,
    val like: Boolean = false,
    @SerialName("season_fav")
    val seasonFav: Boolean = false
)