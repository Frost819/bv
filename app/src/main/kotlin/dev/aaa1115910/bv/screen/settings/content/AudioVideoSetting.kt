package dev.aaa1115910.bv.screen.settings.content

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.component.settings.SettingSwitchListItem
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.screen.settings.SettingsMenuNavItem
import dev.aaa1115910.bv.util.Prefs

@Composable
fun AudioVideoSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showResolutionDialog by remember { mutableStateOf(false) }
    var showAudioCodecDialog by remember { mutableStateOf(false) }
    var showVideoCodecDialog by remember { mutableStateOf(false) }
    var showPlaySpeedDialog by remember { mutableStateOf(false) }
    var showActionAfterPlayDialog by remember { mutableStateOf(false) }

    var selectedResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var selectedVideoCodec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var selectedAudioCodec by remember { mutableStateOf(Prefs.defaultAudio) }
    var selectedPlaySpeed by remember { mutableStateOf(Prefs.defaultPlaySpeed) }
    var selectedActionAfterPlay by remember { mutableStateOf(Prefs.actionAfterPlay) }

    var enableFfmpegAudioRenderer by remember { mutableStateOf(Prefs.enableFfmpegAudioRenderer) }
    var enableSoftwareVideoRenderer by remember { mutableStateOf(Prefs.enableSoftwareVideoDecoder) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = SettingsMenuNavItem.AudioVideo.getDisplayName(context),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingListItem(
            title = "默认分辨率",
            supportText = "当前：${selectedResolution.getDisplayName(context)}",
            onClick = { showResolutionDialog = true }
        )
        SettingListItem(
            title = "默认视频编码",
            supportText = "当前：${selectedVideoCodec.getDisplayName(context)}",
            onClick = { showVideoCodecDialog = true }
        )
        SettingListItem(
            title = "默认音频编码",
            supportText = "当前：${selectedAudioCodec.getDisplayName(context)}",
            onClick = { showAudioCodecDialog = true }
        )
        SettingListItem(
            title = "默认播放速度",
            supportText = "当前：${selectedPlaySpeed.getDisplayName(context)}",
            onClick = { showPlaySpeedDialog = true }
        )
        SettingListItem(
            title = "播放结束动作",
            supportText = "当前：${selectedActionAfterPlay.getDisplayName(context)}",
            onClick = { showActionAfterPlayDialog = true }
        )
        SettingSwitchListItem(
            title = stringResource(R.string.settings_media_software_video_renderer_title),
            supportText = stringResource(R.string.settings_media_software_video_renderer_text),
            checked = enableSoftwareVideoRenderer,
            onCheckedChange = {
                enableSoftwareVideoRenderer = it
                Prefs.enableSoftwareVideoDecoder = it
            }
        )
        SettingSwitchListItem(
            title = stringResource(R.string.settings_media_ffmpeg_audio_renderer_title),
            supportText = stringResource(R.string.settings_media_ffmpeg_audio_renderer_text),
            checked = enableFfmpegAudioRenderer,
            onCheckedChange = {
                enableFfmpegAudioRenderer = it
                Prefs.enableFfmpegAudioRenderer = it
            }
        )
    }
    // 弹窗复用组件
    if (showResolutionDialog) {
        OptionDialog(
            options = Resolution.entries.toTypedArray(),
            selectedOption = selectedResolution,
            onDismiss = { showResolutionDialog = false },
            onSelect = {
                Prefs.defaultQuality = it
                selectedResolution = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showVideoCodecDialog) {
        OptionDialog(
            options = VideoCodec.entries.toTypedArray(),
            selectedOption = selectedVideoCodec,
            onDismiss = { showVideoCodecDialog = false },
            onSelect = {
                Prefs.defaultVideoCodec = it
                selectedVideoCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showAudioCodecDialog) {
        OptionDialog(
            options = Audio.entries.toTypedArray(),
            selectedOption = selectedAudioCodec,
            onDismiss = { showAudioCodecDialog = false },
            onSelect = {
                Prefs.defaultAudio = it
                selectedAudioCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showPlaySpeedDialog) {
        OptionDialog(
            options = PlaySpeedItem.entries.toTypedArray(),
            selectedOption = selectedPlaySpeed,
            onDismiss = { showPlaySpeedDialog = false },
            onSelect = {
                Prefs.defaultPlaySpeed = it
                selectedPlaySpeed = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }

    if (showActionAfterPlayDialog) {
        OptionDialog(
            options = ActionAfterPlayItems.entries.toTypedArray(),
            selectedOption = selectedActionAfterPlay,
            onDismiss = { showActionAfterPlayDialog = false },
            onSelect = {
                Prefs.actionAfterPlay = it
                selectedActionAfterPlay = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
    }
}

enum class ActionAfterPlayItems (val code: Int, private val displayName: String){
    Pause(0, "暂停"),
    PlayNext(1, "播放下一集"),
    Exit(2, "退出播放器");


    companion object{
        fun fromCode(code: Int): ActionAfterPlayItems {
            return ActionAfterPlayItems.entries.find { it.code == code } ?: Exit
        }
    }

    fun getDisplayName(context: Context): String {
        return displayName
    }
}


