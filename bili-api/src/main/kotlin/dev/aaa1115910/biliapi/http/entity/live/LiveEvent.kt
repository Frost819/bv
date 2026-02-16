package dev.aaa1115910.biliapi.http.entity.live

interface LiveEvent

data class DanmakuEvent(
    val content: String,
    val mid: Long,
    val username: String,
    val medalName: String? = null,
    val medalLevel: Int? = null,
    val mode: Int = 1,              // 弹幕模式：1=滚动，4=顶部，5=底部
    val fontSize: Int = 25,         // 字号大小
    val color: Int = 0xFFFFFF       // 颜色（十进制RGB整数）
) : LiveEvent

data class PopularityChangeEvent(
    val popularity: Int,
    val popularityText: String
) : LiveEvent

data class OnlineRankCountEvent(
    val count: Int
) : LiveEvent
