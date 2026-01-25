package dev.aaa1115910.bilisubtitle.entity

data class SubtitleItem(
    val from: Timestamp,
    val to: Timestamp,
    val content: String,
    val isAI: Boolean = false  // 是否为 AI 生成字幕
) {
    fun isShowing(time: Long) = from.totalMills <= time && to.totalMills >= time
}
