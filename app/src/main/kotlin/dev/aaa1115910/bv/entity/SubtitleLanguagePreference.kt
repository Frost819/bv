package dev.aaa1115910.bv.entity

import dev.aaa1115910.biliapi.entity.video.Subtitle

enum class SubtitleLanguagePreference(
    val code: String,
    private val displayName: String
) {
    FirstAvailable("first", "首个可用字幕"),
    SimplifiedChinese("zh-hans", "简体中文"),
    TraditionalChinese("zh-hant", "繁体中文"),
    English("en", "English"),
    Japanese("ja", "日本語"),
    Korean("ko", "한국어");

    fun getDisplayName() = displayName

    fun select(subtitles: List<Subtitle>): Subtitle? {
        val availableSubtitles = subtitles.filter { it.id != -1L }
        if (this == FirstAvailable) return availableSubtitles.firstOrNull()

        return availableSubtitles.firstOrNull { matches(it.lang) }
    }

    internal fun matches(languageCode: String): Boolean {
        val normalizedCode = languageCode.trim().lowercase().removePrefix("ai-")
        return when (this) {
            FirstAvailable -> true
            SimplifiedChinese -> normalizedCode == "zh" ||
                normalizedCode.matches("zh-hans") ||
                normalizedCode.matches("zh-cn") ||
                normalizedCode.matches("zh-sg")
            TraditionalChinese -> normalizedCode.matches("zh-hant") ||
                normalizedCode.matches("zh-tw") ||
                normalizedCode.matches("zh-hk") ||
                normalizedCode.matches("zh-mo")
            English -> normalizedCode.matches("en")
            Japanese -> normalizedCode.matches("ja")
            Korean -> normalizedCode.matches("ko")
        }
    }

    companion object {
        fun fromCode(code: String): SubtitleLanguagePreference {
            return entries.find { it.code == code } ?: FirstAvailable
        }
    }
}

private fun String.matches(languageCode: String): Boolean {
    return this == languageCode || startsWith("$languageCode-")
}
