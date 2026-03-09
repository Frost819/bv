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
import androidx.compose.runtime.LaunchedEffect
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

        // 自动开启规则：需要“可展示的 options”
       // - 当前视频有的轨道要展示
       // - 已经选中的规则 token，即使当前视频没有对应轨道，也要展示（用于取消）
        var selectedTokens by remember {
            mutableStateOf(Prefs.autoSubtitleRuleTokens.toSet())
        }
        var selectedTokenLabels by remember {
            mutableStateOf(Prefs.autoSubtitleRuleTokenLabels)
        }

        // 自动开启规则的可选项（为空则右侧面板应禁用）
        val autoRuleBuildResult = remember(availableSubtitleTracks, selectedTokens, selectedTokenLabels) {
            buildAutoRuleOptions(availableSubtitleTracks, selectedTokens, selectedTokenLabels)
        }
        val autoRuleOptions = autoRuleBuildResult.options

        // 开启“真实轨道名覆盖映射”：当前视频有真实轨道时，更新已选规则对应展示名
        LaunchedEffect(autoRuleBuildResult.selectedTokenLabels) {
            if (autoRuleBuildResult.selectedTokenLabels != selectedTokenLabels) {
                selectedTokenLabels = autoRuleBuildResult.selectedTokenLabels
                Prefs.autoSubtitleRuleTokenLabels = autoRuleBuildResult.selectedTokenLabels
            }
        }
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedClosedCaptionMenuItem) {
                VideoPlayerClosedCaptionMenuItem.AutoEnableRules -> {
                    // 规则项：按当前视频真实存在的字幕轨道生成（CC 显示“中文/English”，AI 显示“中文（AI）/English（AI）”）
                    // options = 当前视频可用轨道 + 已选中的规则 token（即使当前视频无此轨道也展示，便于取消）
                    val options = autoRuleOptions

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

                            val newTokenLabels = selectedTokenLabels
                                .filterKeys { it in newTokens }
                                .toMutableMap()

                            indexes.forEach { i ->
                                options.getOrNull(i)?.let { opt ->
                                    newTokenLabels[opt.token] = opt.persistLabel.ifBlank { opt.token }
                                }
                            }

                            selectedTokens = newTokens
                            selectedTokenLabels = newTokenLabels
                            Prefs.autoSubtitleRuleTokens = newTokens.sorted()
                            Prefs.autoSubtitleRuleTokenLabels = newTokenLabels
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
                        return@onPreviewKeyEvent it.key != Key.Enter && it.key != Key.DirectionCenter
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
                    VideoPlayerClosedCaptionMenuItem.AutoEnableRules ->
                        autoRuleOptions.isNotEmpty() || selectedTokens.isNotEmpty()
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
    val token: String,       // "CC|zh" / "AI|en"
    val label: String,       // 显示给 UI 的文本（缺失时带 ! 前缀）
    val persistLabel: String // 实际持久化文本（不带 ! 前缀）
)

private data class AutoRuleBuildResult(
    val options: List<AutoRuleOption>,
    val selectedTokenLabels: Map<String, String>
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

private fun buildAutoRuleOptions(
    tracks: List<Subtitle>,
    selectedTokens: Set<String>,
    selectedTokenLabels: Map<String, String>
): AutoRuleBuildResult {
    // options = 当前视频轨道 + 已选 token（即使当前视频缺失对应语言/类型，也要给用户一个取消入口）
    data class LangInfo(
        var ccLabel: String? = null,
        var aiLabel: String? = null,
        var hasSelectedCC: Boolean = false,
        var hasSelectedAI: Boolean = false
    )

    val infoByLang = linkedMapOf<String, LangInfo>()

    // 仅保留“当前已选 token”对应的 label，避免历史垃圾数据累积
    val normalizedSelectedTokenLabels = selectedTokenLabels
        .filterKeys { it in selectedTokens }
        .toMutableMap()

    // 先从当前视频真实轨道收集语言信息
    tracks.filter { it.id != -1L }.forEach { t ->
        val key = t.normalizedLangKey()
        if (key.isBlank()) return@forEach

        val info = infoByLang.getOrPut(key) { LangInfo() }
        val baseName = cleanLangDocForDisplay(t.langDoc)

        when (t.type) {
            SubtitleType.CC -> {
                if (info.ccLabel == null) info.ccLabel = baseName
                val token = "CC|$key"
                // 开启“真实轨道名覆盖映射”：仅覆盖当前已选规则
                if (selectedTokens.contains(token)) {
                    normalizedSelectedTokenLabels[token] = baseName
                }
            }

            SubtitleType.AI -> {
                val aiName = "$baseName(AI)"
                if (info.aiLabel == null) info.aiLabel = aiName
                val token = "AI|$key"
                // 开启“真实轨道名覆盖映射”：仅覆盖当前已选规则
                if (selectedTokens.contains(token)) {
                    normalizedSelectedTokenLabels[token] = aiName
                }
            }
        }
    }

    // 合并已选 token（保证跨视频可见并可取消）
    selectedTokens.forEach { token ->
        val parts = token.split("|", limit = 2)
        if (parts.size != 2) return@forEach

        val typePart = parts[0].trim()
        val langKey = parts[1].trim().lowercase()
        if (langKey.isBlank()) return@forEach

        val info = infoByLang.getOrPut(langKey) { LangInfo() }
        when (typePart) {
            "CC" -> info.hasSelectedCC = true
            "AI" -> info.hasSelectedAI = true
            else -> return@forEach
        }
    }

    fun langSortKey(langKey: String): Pair<Int, String> = when (langKey) {
        "zh" -> 0 to ""
        "en" -> 1 to ""
        else -> 2 to langKey
    }

    val orderedLangKeys = infoByLang.keys.sortedWith(
        compareBy({ langSortKey(it).first }, { langSortKey(it).second })
    )

    val result = mutableListOf<AutoRuleOption>()
    orderedLangKeys.forEach { langKey ->
        val info = infoByLang[langKey] ?: return@forEach

        val ccToken = "CC|$langKey"
        val ccPersistedLabel = info.ccLabel ?: normalizedSelectedTokenLabels[ccToken] ?: ccToken
        if (info.ccLabel != null || info.hasSelectedCC) {
            result.add(
                AutoRuleOption(
                    token = ccToken,
                    label = if (info.ccLabel != null) ccPersistedLabel else "！$ccPersistedLabel",
                    persistLabel = ccPersistedLabel
                )
            )
        }

        val aiToken = "AI|$langKey"
        val aiPersistedLabel = info.aiLabel ?: normalizedSelectedTokenLabels[aiToken] ?: aiToken
        if (info.aiLabel != null || info.hasSelectedAI) {
            result.add(
                AutoRuleOption(
                    token = aiToken,
                    label = if (info.aiLabel != null) aiPersistedLabel else "！$aiPersistedLabel",
                    persistLabel = aiPersistedLabel
                )
            )
        }
    }

    return AutoRuleBuildResult(
        options = result,
        selectedTokenLabels = normalizedSelectedTokenLabels.toMap()
    )
}