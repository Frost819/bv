package dev.aaa1115910.bv.entity

import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleLanguagePreferenceTest {
    @Test
    fun `selects preferred language instead of list order`() {
        val subtitles = listOf(
            subtitle(1, "en-US"),
            subtitle(2, "zh-Hans")
        )

        val selected = SubtitleLanguagePreference.SimplifiedChinese.select(subtitles)

        assertEquals(2L, selected?.id)
    }

    @Test
    fun `recognizes ai subtitle language codes`() {
        val subtitles = listOf(
            subtitle(1, "ai-en"),
            subtitle(2, "ai-zh")
        )

        assertEquals(
            2L,
            SubtitleLanguagePreference.SimplifiedChinese.select(subtitles)?.id
        )
    }

    @Test
    fun `keeps simplified and traditional ai subtitles distinct`() {
        val subtitles = listOf(subtitle(1, "ai-zh-Hant"))

        assertNull(SubtitleLanguagePreference.SimplifiedChinese.select(subtitles))
        assertEquals(
            1L,
            SubtitleLanguagePreference.TraditionalChinese.select(subtitles)?.id
        )
    }

    @Test
    fun `returns no subtitle when preferred language is unavailable`() {
        val subtitles = listOf(
            subtitle(-1, "off"),
            subtitle(1, "en-US"),
            subtitle(2, "ja-JP")
        )

        val selected = SubtitleLanguagePreference.TraditionalChinese.select(subtitles)

        assertNull(selected)
    }

    @Test
    fun `restores unknown stored value safely`() {
        assertEquals(
            SubtitleLanguagePreference.FirstAvailable,
            SubtitleLanguagePreference.fromCode("unknown")
        )
    }

    private fun subtitle(id: Long, languageCode: String) = Subtitle(
        id = id,
        lang = languageCode,
        langDoc = languageCode,
        url = "https://example.com/$id",
        type = SubtitleType.CC,
        aiType = SubtitleAiType.Normal,
        aiStatus = SubtitleAiStatus.None
    )
}
