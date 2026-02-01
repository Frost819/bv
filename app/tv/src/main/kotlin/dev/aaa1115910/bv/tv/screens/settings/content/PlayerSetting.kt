package dev.aaa1115910.bv.tv.screens.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.tv.component.settings.SettingListItem
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
    var showUGCVideoInfo by remember { mutableStateOf(Prefs.showUGCVideoInfo) }
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
    var enableAudioPlaybackParams by remember { mutableStateOf(Prefs.enableAudioPlaybackParams) }
    var enableAsyncQueueing by remember { mutableStateOf(Prefs.enableAsyncQueueing) }

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
                    title = "启用音频播放参数调整",
                    supportText = "优化音频播放速度和音效的调整，如果开启后音频播放出问题请关闭此选项",
                    checked = enableAudioPlaybackParams,
                    onCheckedChange = {
                        enableAudioPlaybackParams = it
                        Prefs.enableAudioPlaybackParams = it
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
                    title = stringResource(R.string.settings_show_ugc_video_info_title),
                    supportText = stringResource(R.string.settings_show_ugc_video_info_text),
                    checked = showUGCVideoInfo,
                    onCheckedChange = {
                        showUGCVideoInfo = it
                        Prefs.showUGCVideoInfo = it
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
        }

        OnlineViewerCountDialog(
            show = showOnlineViewerCountDialog,
            onHideDialog = { showOnlineViewerCountDialog = false },
            showOnlineViewerCount = showOnlineViewerCount,
            onShowOnlineViewerCountChange = { Prefs.showOnlineViewerCount = it }
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