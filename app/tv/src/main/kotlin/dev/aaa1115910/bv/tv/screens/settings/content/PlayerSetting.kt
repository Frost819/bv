package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.PlayerLoadNextAction
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.ControllerButtonConfig
import dev.aaa1115910.bv.player.entity.DefaultSubtitle
import dev.aaa1115910.bv.player.entity.getControllerButtonConfigsForEditing
import dev.aaa1115910.bv.player.entity.getControllerButtonDisplayName
import dev.aaa1115910.bv.player.entity.serializeControllerButtonsOrder
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.tv.component.settings.SettingListItemWithDialog
import dev.aaa1115910.bv.tv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.tv.component.settings.SettingNumberListItem
import dev.aaa1115910.bv.tv.screens.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun PlayerSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var selectedVideoCodec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var selectedAudio by remember { mutableStateOf(Prefs.defaultAudio) }
    var enableFfmpegAudioRenderer by remember { mutableStateOf(Prefs.enableFfmpegAudioRenderer) }
    var playerShowBottomProgressBar by remember { mutableStateOf(Prefs.playerShowBottomProgressBar) }
    var playerShowDebugInfo by remember { mutableStateOf(Prefs.playerShowDebugInfo) }
    var playerExitWhenAllIsPlayed by remember { mutableStateOf(Prefs.playerExitWhenAllIsPlayed) }
    var playerLoadNextAction by remember { mutableStateOf(Prefs.playerLoadNextAction) }
    var playerDefaultStartPosition by remember { mutableStateOf(Prefs.playerDefaultStartPosition) }
    var defaultPlaybackSpeed by remember { mutableDoubleStateOf(Prefs.defaultPlaySpeed.toDouble()) }
    var playerSeekForwardStep by remember { mutableDoubleStateOf(Prefs.playerSeekForwardStep.toDouble()) }
    var playerSeekBackwardStep by remember { mutableDoubleStateOf(Prefs.playerSeekBackwardStep.toDouble()) }
    var portraitVideoFixMode by remember { mutableStateOf(Prefs.portraitVideoFixMode) }
    var showOnlineViewerCountDialog by remember { mutableStateOf(false) }
    val showOnlineViewerCount by Prefs.showOnlineViewerCountFlow.collectAsState(Prefs.showOnlineViewerCount)
    var showLiveViewerCountTipDialog by remember { mutableStateOf(false) }
    val showLiveViewerCountTip by Prefs.showLiveViewerCountTipFlow.collectAsState(Prefs.showLiveViewerCountTip)
    var enableTunneling by remember { mutableStateOf(Prefs.enableTunneling) }
    var enableAsyncQueueing by remember { mutableStateOf(Prefs.enableAsyncQueueing) }
    var skipPgcIntroOutro by remember { mutableStateOf(Prefs.skipPgcIntroOutro) }
    var showControllerButtonDialog by remember { mutableStateOf(false) }
    var defaultSubtitle by remember { mutableStateOf(Prefs.defaultSubtitle) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.Player.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_resolution),
                    supportText = stringResource(R.string.settings_item_resolution),
                    options = Resolution.entries.reversed(),
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedResolution,
                    onValueChange = {
                        Prefs.defaultQuality = it
                        selectedResolution = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_codec),
                    supportText = stringResource(R.string.settings_item_codec),
                    options = VideoCodec.entries.filter { it != VideoCodec.DVH1 && it != VideoCodec.HVC1 },
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedVideoCodec,
                    onValueChange = {
                        Prefs.defaultVideoCodec = it
                        selectedVideoCodec = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_item_audio),
                    supportText = stringResource(R.string.settings_item_codec),
                    options = Audio.entries,
                    getDisplayName = { item, ctx -> item.getDisplayName(ctx) },
                    value = selectedAudio,
                    onValueChange = {
                        Prefs.defaultAudio = it
                        selectedAudio = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_other_ffmpeg_audio_renderer_title),
                    supportText = stringResource(R.string.settings_other_ffmpeg_audio_renderer_text),
                    checked = enableFfmpegAudioRenderer,
                    onCheckedChange = {
                        enableFfmpegAudioRenderer = it
                        Prefs.enableFfmpegAudioRenderer = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = "启用异步缓冲队列",
                    supportText = "减少丢帧和音频欠载，提升高帧率视频播放性能（Android 6.0-11 有效）",
                    checked = enableAsyncQueueing,
                    onCheckedChange = {
                        enableAsyncQueueing = it
                        Prefs.enableAsyncQueueing = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = "启用隧道播放模式",
                    supportText = "隧道播放模式能改善播放性能，部分设备可能出现播放异常",
                    checked = enableTunneling,
                    onCheckedChange = {
                        enableTunneling = it
                        Prefs.enableTunneling = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_show_bottom_progress_bar_title),
                    supportText = stringResource(R.string.settings_player_show_bottom_progress_bar_text),
                    checked = playerShowBottomProgressBar,
                    onCheckedChange = {
                        playerShowBottomProgressBar = it
                        Prefs.playerShowBottomProgressBar = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_show_debug_info_title),
                    supportText = stringResource(R.string.settings_player_show_debug_info_text),
                    checked = playerShowDebugInfo,
                    onCheckedChange = {
                        playerShowDebugInfo = it
                        Prefs.playerShowDebugInfo = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_portrait_video_fix_mode_title),
                    supportText = stringResource(R.string.settings_portrait_video_fix_mode_text),
                    options = PortraitVideoFixMode.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = portraitVideoFixMode,
                    onValueChange = {
                        portraitVideoFixMode = it
                        Prefs.portraitVideoFixMode = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_player_load_next_action_title),
                    supportText = stringResource(R.string.settings_player_load_next_action_text),
                    options = PlayerLoadNextAction.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = playerLoadNextAction,
                    onValueChange = {
                        playerLoadNextAction = it
                        Prefs.playerLoadNextAction = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = stringResource(R.string.settings_player_default_start_position_title),
                    supportText = stringResource(R.string.settings_player_default_start_position_text),
                    options = PlayerDefaultStartPosition.entries,
                    getDisplayName = { item, ctx -> item.displayName(ctx) },
                    value = playerDefaultStartPosition,
                    onValueChange = {
                        playerDefaultStartPosition = it
                        Prefs.playerDefaultStartPosition = it
                    }
                )
            }
            item {
                SettingSwitchListItem(
                    title = stringResource(R.string.settings_player_exit_when_all_is_played_title),
                    supportText = stringResource(R.string.settings_player_exit_when_all_is_played_text),
                    checked = playerExitWhenAllIsPlayed,
                    onCheckedChange = {
                        playerExitWhenAllIsPlayed = it
                        Prefs.playerExitWhenAllIsPlayed = it
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_default_playback_speed_title),
                    supportText = stringResource(R.string.settings_player_default_playback_speed_text),
                    value = defaultPlaybackSpeed,
                    minValue = 0.25,
                    maxValue = 2.5,
                    isInteger = false,
                    step = 0.25,
                    onValueChange = {
                        defaultPlaybackSpeed = it
                        Prefs.defaultPlaySpeed = it.toFloat()
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_seek_forward_step_title),
                    supportText = stringResource(R.string.settings_player_seek_forward_step_text),
                    value = playerSeekForwardStep,
                    minValue = 5.0,
                    maxValue = 30.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        playerSeekForwardStep = it
                        Prefs.playerSeekForwardStep = it.toInt()
                    }
                )
            }
            item {
                SettingNumberListItem(
                    title = stringResource(R.string.settings_player_seek_backward_step_title),
                    supportText = stringResource(R.string.settings_player_seek_backward_step_text),
                    value = playerSeekBackwardStep,
                    minValue = 5.0,
                    maxValue = 30.0,
                    isInteger = true,
                    step = 1.0,
                    onValueChange = {
                        playerSeekBackwardStep = it
                        Prefs.playerSeekBackwardStep = it.toInt()
                    }
                )
            }
            item {
                SettingListItem(
                    title = "视频在线观看人数",
                    supportText = "设置播放器在线人数显示方式",
                    valueText = when (showOnlineViewerCount) {
                        0 -> "不显示"
                        1 -> "30 秒后隐藏"
                        2 -> "始终显示"
                        else -> "30 秒后隐藏"
                    },
                    onClick = { showOnlineViewerCountDialog = true }
                )
            }
            item {
                SettingListItem(
                    title = "直播人气&高能观众",
                    supportText = "设置直播人气和高能观众显示方式",
                    valueText = when (showLiveViewerCountTip) {
                        0 -> "不显示"
                        1 -> "30 秒后隐藏"
                        2 -> "始终显示"
                        else -> "30 秒后隐藏"
                    },
                    onClick = { showLiveViewerCountTipDialog = true }
                )
            }
            item {
                SettingSwitchListItem(
                    title = "跳过 PGC 片头片尾",
                    supportText = "自动跳过 PGC 片头片尾",
                    checked = skipPgcIntroOutro,
                    onCheckedChange = {
                        skipPgcIntroOutro = it
                        Prefs.skipPgcIntroOutro = it
                    }
                )
            }
            item {
                SettingListItemWithDialog(
                    title = "默认字幕",
                    supportText = "首次播放时自动加载的字幕语言，仅加载非AI生成的字幕",
                    options = DefaultSubtitle.entries,
                    getDisplayName = { item, _ -> item.displayName() },
                    value = defaultSubtitle,
                    onValueChange = {
                        defaultSubtitle = it
                        Prefs.defaultSubtitle = it
                    }
                )
            }
            item {
                SettingListItem(
                    title = "控制栏按钮",
                    supportText = "自定义控制栏按钮的显示、排序和默认焦点",
                    onClick = { showControllerButtonDialog = true }
                )
            }
        }

        OnlineViewerCountDialog(
            show = showOnlineViewerCountDialog,
            onHideDialog = { showOnlineViewerCountDialog = false },
            showOnlineViewerCount = showOnlineViewerCount,
            onShowOnlineViewerCountChange = { Prefs.showOnlineViewerCount = it }
        )

        LiveViewerCountTipDialog(
            show = showLiveViewerCountTipDialog,
            onHideDialog = { showLiveViewerCountTipDialog = false },
            showLiveViewerCountTip = showLiveViewerCountTip,
            onShowLiveViewerCountTipChange = { Prefs.showLiveViewerCountTip = it }
        )

        PlayerControllerButtonDialog(
            show = showControllerButtonDialog,
            onHideDialog = { showControllerButtonDialog = false },
            initialOrderString = Prefs.playerControllerButtonsOrder
        )
    }
}

@Composable
private fun OnlineViewerCountDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    showOnlineViewerCount: Int,
    onShowOnlineViewerCountChange: (Int) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "视频在线观看人数") },
            text = {
                Column {
                    val options = listOf(
                        "不显示" to 0,
                        "30 秒后隐藏" to 1,
                        "始终显示" to 2
                    )
                    options.forEach { (text, value) ->
                        ListItem(
                            selected = showOnlineViewerCount == value,
                            onClick = { onShowOnlineViewerCountChange(value) },
                            headlineContent = { Text(text = text) },
                            trailingContent = {
                                RadioButton(
                                    selected = showOnlineViewerCount == value,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun LiveViewerCountTipDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    showLiveViewerCountTip: Int,
    onShowLiveViewerCountTipChange: (Int) -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { Text(text = "直播人气&高能观众") },
            text = {
                Column {
                    val options = listOf(
                        "不显示" to 0,
                        "30 秒后隐藏" to 1,
                        "始终显示" to 2
                    )
                    options.forEach { (text, value) ->
                        ListItem(
                            selected = showLiveViewerCountTip == value,
                            onClick = { onShowLiveViewerCountTipChange(value) },
                            headlineContent = { Text(text = text) },
                            trailingContent = {
                                RadioButton(
                                    selected = showLiveViewerCountTip == value,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun PlayerControllerButtonDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    initialOrderString: String
) {
    if (!show) return

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val initialConfigs = remember(initialOrderString) {
        getControllerButtonConfigsForEditing(initialOrderString)
    }

    var buttonConfigs by remember { mutableStateOf(initialConfigs) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var enterDownTime by remember { mutableStateOf(0L) }
    var longPressHandled by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) focusRequester.requestFocus(scope)
    }

    // 自动滚动到选中项
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(
            index = (selectedIndex - 2).coerceAtLeast(0)
        )
    }

    TvAlertDialog(
        modifier = modifier,
        onDismissRequest = {
            Prefs.playerControllerButtonsOrder = serializeControllerButtonsOrder(buttonConfigs)
            onHideDialog()
        },
        title = { Text(text = "控制栏按钮") },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown) {
                            when (it.key) {
                                Key.DirectionLeft -> {
                                    if (selectedIndex > 0) {
                                        buttonConfigs = buttonConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex - 1]
                                            this[selectedIndex - 1] = temp
                                        }
                                        selectedIndex--
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (selectedIndex < buttonConfigs.size - 1) {
                                        buttonConfigs = buttonConfigs.toMutableList().apply {
                                            val temp = this[selectedIndex]
                                            this[selectedIndex] = this[selectedIndex + 1]
                                            this[selectedIndex + 1] = temp
                                        }
                                        selectedIndex++
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (selectedIndex > 0) selectedIndex--
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (selectedIndex < buttonConfigs.size - 1) selectedIndex++
                                    true
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    if (enterDownTime == 0L) {
                                        // 首次按下，记录时间
                                        enterDownTime = System.currentTimeMillis()
                                        longPressHandled = false
                                    } else if (!longPressHandled && System.currentTimeMillis() - enterDownTime >= 600) {
                                        // 在重复 KeyDown 期间检测到长按
                                        longPressHandled = true
                                        buttonConfigs = buttonConfigs.toMutableList().apply {
                                            for (i in indices) {
                                                this[i] = this[i].copy(
                                                    isDefaultFocus = i == selectedIndex
                                                )
                                            }
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else if (it.type == KeyEventType.KeyUp) {
                            when (it.key) {
                                Key.Enter, Key.DirectionCenter -> {
                                    if (!longPressHandled && enterDownTime > 0L) {
                                        // 短按：切换显示/隐藏
                                        val config = buttonConfigs[selectedIndex]
                                        buttonConfigs = buttonConfigs.toMutableList().apply {
                                            this[selectedIndex] = config.copy(hidden = !config.hidden)
                                        }
                                    }
                                    enterDownTime = 0L
                                    longPressHandled = false
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                Text(
                    text = "左右键排序 · 短按确认显示/隐藏 · 长按确认设为默认焦点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(buttonConfigs) { index, config ->
                        ControllerButtonEditRow(
                            title = getControllerButtonDisplayName(config.id),
                            hidden = config.hidden,
                            isDefaultFocus = config.isDefaultFocus,
                            selected = index == selectedIndex
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ControllerButtonEditRow(
    title: String,
    hidden: Boolean,
    isDefaultFocus: Boolean,
    selected: Boolean
) {
    val shape = remember { RoundedCornerShape(12.dp) }
    val bgColor = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            textDecoration = if (hidden) TextDecoration.LineThrough else null,
            color = if (selected) {
                MaterialTheme.colorScheme.background
            } else if (hidden) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isDefaultFocus) {
                Text(
                    text = "默认焦点",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                )
            }
            if (hidden) {
                Text(
                    text = "隐藏",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "显示",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}