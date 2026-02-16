package dev.aaa1115910.bv.player.entity

/**
 * 默认字幕语言
 */
enum class DefaultSubtitle(val value: Int) {
    /** 关闭 */
    Off(0),
    /** 中文 */
    Chinese(1),
    /** English */
    English(2);

    fun displayName(): String = when (this) {
        Off -> "关闭"
        Chinese -> "中文"
        English -> "English"
    }

    companion object {
        fun fromValue(value: Int): DefaultSubtitle = entries.find { it.value == value } ?: Off
    }
}
