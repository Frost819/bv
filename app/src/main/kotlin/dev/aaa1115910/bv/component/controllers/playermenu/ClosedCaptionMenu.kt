package dev.aaa1115910.bv.component.controllers.playermenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.component.controllers.playermenu.component.CheckBoxMenuList
import dev.aaa1115910.bv.component.controllers.LocalMenuFocusStateData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.VideoPlayerClosedCaptionMenuItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.controllers.playermenu.component.RadioMenuList
import dev.aaa1115910.bv.component.controllers.playermenu.component.StepLessMenuItem
import dev.aaa1115910.bv.component.ifElse
import java.text.NumberFormat

@Composable
fun ClosedCaptionMenuList(
    modifier: Modifier = Modifier,
    currentSubtitleId: Long,
    availableSubtitleTracks: List<Subtitle>,
    currentFontSize: TextUnit,
    currentOpacity: Float,
    currentPadding: Dp,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedClosedCaptionMenuItem by remember { mutableStateOf(VideoPlayerClosedCaptionMenuItem.Switch) }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        // 真实字幕轨道是否存在（排除 id=-1 的“关闭”）
        val hasRealSubtitleTrack = remember(availableSubtitleTracks) {
            availableSubtitleTracks.any { it.id != -1L }
        }

        // 自动开启规则的可选项（为空则右侧面板应禁用）
        val autoRuleOptions = remember(availableSubtitleTracks) {
            buildAutoRuleOptions(availableSubtitleTracks)
        }
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedClosedCaptionMenuItem) {
                VideoPlayerClosedCaptionMenuItem.AutoEnableRules -> {
                    // 规则项：按当前视频真实存在的字幕轨道生成（CC 显示“中文/English”，AI 显示“中文（AI）/English（AI）”）
                    val options = remember(availableSubtitleTracks) {
                        buildAutoRuleOptions(availableSubtitleTracks)
                    }

                    var selectedTokens by remember {
                        mutableStateOf(Prefs.autoSubtitleRuleTokens.toSet())
                    }

                    val selectedIndexes = remember(options, selectedTokens) {
                        options.mapIndexedNotNull { index, opt ->
                            if (selectedTokens.contains(opt.token)) index else null
                        }
                    }

                    CheckBoxMenuList(
                        modifier = menuItemsModifier,
                        items = options.map { it.label },
                        selected = selectedIndexes,
                        onSelectedChanged = { indexes ->
                            val newTokens = indexes.mapNotNull { i ->
                                options.getOrNull(i)?.token
                            }.toSet()

                            selectedTokens = newTokens
                            Prefs.autoSubtitleRuleTokens = newTokens.sorted()
                        },
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        }
                    )
                }

                VideoPlayerClosedCaptionMenuItem.ContinuePlay -> {
                    var enabled by remember { mutableStateOf(Prefs.continuePlayAutoSubtitleEnabled) }

                    RadioMenuList(
                        modifier = menuItemsModifier,
                        items = listOf("关闭", "开启"),
                        selected = if (enabled) 1 else 0,
                        onSelectedChanged = {
                            enabled = it == 1
                            Prefs.continuePlayAutoSubtitleEnabled = enabled
                        },
                        onFocusBackToParent = {
                            onFocusStateChange(MenuFocusState.Menu)
                            focusRequester.requestFocus()
                        },
                    )
                }

                VideoPlayerClosedCaptionMenuItem.Switch -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = availableSubtitleTracks.map {
                        it.langDoc
                            .replace("（自动生成）", "")
                            .replace("（自动翻译）", "")
                            .trim() + if (it.type == SubtitleType.AI) "(AI)" else ""
                    },
                    selected = availableSubtitleTracks.indexOfFirst { it.id == currentSubtitleId },
                    onSelectedChanged = { onSubtitleChange(availableSubtitleTracks[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerClosedCaptionMenuItem.Size -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentFontSize.value.toInt(),
                    step = 1,
                    range = 12..48,
                    text = "${currentFontSize.value.toInt()} SP",
                    onValueChange = { onSubtitleSizeChange(it.sp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Opacity -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    step = 0.01f,
                    range = 0f..1f,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    onValueChange = onSubtitleBackgroundOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Padding -> StepLessMenuItem(
                    modifier = menuItemsModifier,
                    value = currentPadding.value.toInt(),
                    step = 1,
                    range = 0..48,
                    text = "${currentPadding.value.toInt()} DP",
                    onValueChange = { onSubtitleBottomPadding(it.dp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    when (it.key) {
                        Key.DirectionRight -> onFocusStateChange(MenuFocusState.MenuNav)
                        Key.DirectionLeft -> onFocusStateChange(MenuFocusState.Items)
                        else -> {}
                    }
                    false
                }
                .focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(VideoPlayerClosedCaptionMenuItem.entries) { index, item ->
                val enabled = when (item) {
                    VideoPlayerClosedCaptionMenuItem.Switch -> hasRealSubtitleTrack
                    VideoPlayerClosedCaptionMenuItem.AutoEnableRules -> autoRuleOptions.isNotEmpty()
                    else -> true
                }

                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester))
                        .focusProperties { canFocus = enabled }
                        .alpha(if (enabled) 1f else 0.45f),
                    text = item.getDisplayName(context),
                    selected = selectedClosedCaptionMenuItem == item,
                    onClick = {},
                    onFocus = { if (enabled) selectedClosedCaptionMenuItem = item },
                )
            }
        }
    }
}

private data class AutoRuleOption(
    val token: String, // "CC|zh" / "AI|en"
    val label: String  // "中文" / "中文（AI）"
)

private fun Subtitle.normalizedLangKey(): String {
    // 优先使用 lang，兜底 langDoc
    val raw = (lang.ifBlank { langDoc }).trim()
    if (raw.isEmpty()) return ""

    val noAiPrefix = if (raw.startsWith("ai-", ignoreCase = true)) raw.substring(3) else raw
    val primary = noAiPrefix.substringBefore("-")
    return primary.lowercase()
}

private fun cleanLangDocForDisplay(doc: String): String {
    return doc
        .replace("（自动生成）", "")
        .replace("（自动翻译）", "")
        .trim()
}

private fun buildAutoRuleOptions(tracks: List<Subtitle>): List<AutoRuleOption> {
    // 仅基于“当前视频存在的轨道”生成可选项：某语言没有 CC 就不生成 CC 选项；同理 AI。
    data class LangInfo(
        var baseName: String = "",
        var hasCC: Boolean = false,
        var hasAI: Boolean = false,
    )

    val infoByLang = linkedMapOf<String, LangInfo>()
    tracks.filter { it.id != -1L }.forEach { t ->
        val key = t.normalizedLangKey()
        if (key.isBlank()) return@forEach

        val info = infoByLang.getOrPut(key) { LangInfo() }
        val name = cleanLangDocForDisplay(t.langDoc)

        // baseName 优先取 CC 的显示名
        if (info.baseName.isBlank() || (t.type == SubtitleType.CC && !info.hasCC)) {
            info.baseName = name
        }

        when (t.type) {
            SubtitleType.CC -> info.hasCC = true
            SubtitleType.AI -> info.hasAI = true
        }
    }

    fun langSortKey(langKey: String): Pair<Int, String> = when (langKey) {
        "zh" -> 0 to ""
        "en" -> 1 to ""
        else -> 2 to langKey
    }

    val orderedLangKeys = infoByLang.keys.sortedWith(compareBy({ langSortKey(it).first }, { langSortKey(it).second }))

    val result = mutableListOf<AutoRuleOption>()
    orderedLangKeys.forEach { langKey ->
        val info = infoByLang[langKey] ?: return@forEach
        val name = info.baseName.ifBlank { langKey }

        if (info.hasCC) {
            result.add(
                AutoRuleOption(
                    token = "CC|$langKey",
                    label = name
                )
            )
        }
        if (info.hasAI) {
            result.add(
                AutoRuleOption(
                    token = "AI|$langKey",
                    label = "$name(AI)"
                )
            )
        }
    }
    return result
}