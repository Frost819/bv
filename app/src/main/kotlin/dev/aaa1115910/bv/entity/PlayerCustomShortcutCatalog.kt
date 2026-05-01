package dev.aaa1115910.bv.entity

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import kotlin.math.roundToInt

data class PlayerCustomShortcutActionEntry(
    val action: PlayerCustomShortcutAction,
    val displayName: String
)

object PlayerCustomShortcutCatalog {
    fun entries(context: Context): List<PlayerCustomShortcutActionEntry> {
        return buildList {
            add(PlayerCustomShortcutAction.ShowInfo entry "呼出播放信息层")
            add(PlayerCustomShortcutAction.OpenSettings entry "打开播放器设置菜单")
            add(PlayerCustomShortcutAction.OpenVideoList entry "打开视频列表")
            add(PlayerCustomShortcutAction.OpenRelatedVideos entry "打开相关视频")
            add(PlayerCustomShortcutAction.TogglePlayPause entry "播放/暂停")
            add(PlayerCustomShortcutAction.PlayPrevious entry "上一个")
            add(PlayerCustomShortcutAction.PlayNext entry "下一个")
            add(PlayerCustomShortcutAction.OpenVideoDetail entry "打开视频详情")
            add(PlayerCustomShortcutAction.OpenUpPage entry "打开 UP 主页")
            add(PlayerCustomShortcutAction.ToggleLoop entry "单视频循环开关")
            add(PlayerCustomShortcutAction.ToggleDanmaku entry "弹幕开关")
            add(PlayerCustomShortcutAction.ToggleSubtitle entry "字幕开关")

            PlaySpeedItem.entries.forEach { speed ->
                add(
                    PlayerCustomShortcutAction.SetPlaybackSpeed(speed.speed) entry
                        "设置播放速度：${speed.getDisplayName(context)}"
                )
            }

            Resolution.entries.forEach { resolution ->
                add(
                    PlayerCustomShortcutAction.SetResolution(resolution.code) entry
                        "设置分辨率：${resolution.getShortDisplayName(context)}"
                )
            }

            Audio.entries.forEach { audio ->
                add(
                    PlayerCustomShortcutAction.SetAudio(audio) entry
                        "设置音频编码：${audio.getDisplayName(context)}"
                )
            }

            VideoCodec.entries.forEach { codec ->
                add(
                    PlayerCustomShortcutAction.SetVideoCodec(codec) entry
                        "设置视频编码：${codec.getDisplayName(context)}"
                )
            }

            VideoAspectRatio.entries.forEach { aspectRatio ->
                add(
                    PlayerCustomShortcutAction.SetAspectRatio(aspectRatio) entry
                        "设置画面比例：${aspectRatio.getDisplayName(context)}"
                )
            }

            listOf(0.5f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f).forEach { scale ->
                add(
                    PlayerCustomShortcutAction.SetDanmakuScale(scale) entry
                        "设置弹幕大小：${scale.percentText()}"
                )
            }

            listOf(0f, 0.25f, 0.5f, 0.7f, 0.85f, 1f).forEach { opacity ->
                add(
                    PlayerCustomShortcutAction.SetDanmakuOpacity(opacity) entry
                        "设置弹幕透明度：${opacity.percentText()}"
                )
            }

            DanmakuSpeedFactor.entries.forEach { factor ->
                add(
                    PlayerCustomShortcutAction.SetDanmakuSpeedFactor(factor.factor) entry
                        "设置弹幕速度：${factor.getDisplayName(context)}"
                )
            }

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { area ->
                add(
                    PlayerCustomShortcutAction.SetDanmakuArea(area) entry
                        "设置弹幕区域：${area.percentText()}"
                )
            }

            add(PlayerCustomShortcutAction.SetDanmakuMaskEnabled(false) entry "设置弹幕防遮挡：关闭")
            add(PlayerCustomShortcutAction.SetDanmakuMaskEnabled(true) entry "设置弹幕防遮挡：开启")

            listOf(12, 16, 20, 24, 32, 40, 48).forEach { fontSize ->
                add(
                    PlayerCustomShortcutAction.SetSubtitleFontSize(fontSize) entry
                        "设置字幕字号：${fontSize.sp.value.toInt()} SP"
                )
            }

            listOf(0f, 0.25f, 0.4f, 0.5f, 0.75f, 1f).forEach { opacity ->
                add(
                    PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(opacity) entry
                        "设置字幕背景透明度：${opacity.percentText()}"
                )
            }

            listOf(0, 8, 12, 16, 24, 32, 48).forEach { padding ->
                add(
                    PlayerCustomShortcutAction.SetSubtitleBottomPadding(padding) entry
                        "设置字幕底部间距：${padding.dp.value.toInt()} DP"
                )
            }

            add(PlayerCustomShortcutAction.TogglePersistentBottomProgress entry "开关底部常驻迷你进度条")
        }
    }

    fun getActionDisplayName(
        context: Context,
        action: PlayerCustomShortcutAction
    ): String {
        return entries(context).firstOrNull { it.action == action }?.displayName
            ?: when (action) {
                PlayerCustomShortcutAction.ShowInfo -> "呼出播放信息层"
                PlayerCustomShortcutAction.OpenSettings -> "打开播放器设置菜单"
                PlayerCustomShortcutAction.OpenVideoList -> "打开视频列表"
                PlayerCustomShortcutAction.OpenRelatedVideos -> "打开相关视频"
                PlayerCustomShortcutAction.TogglePlayPause -> "播放/暂停"
                PlayerCustomShortcutAction.PlayPrevious -> "上一个"
                PlayerCustomShortcutAction.PlayNext -> "下一个"
                PlayerCustomShortcutAction.OpenVideoDetail -> "打开视频详情"
                PlayerCustomShortcutAction.OpenUpPage -> "打开 UP 主页"
                PlayerCustomShortcutAction.ToggleLoop -> "单视频循环开关"
                PlayerCustomShortcutAction.ToggleDanmaku -> "弹幕开关"
                PlayerCustomShortcutAction.ToggleSubtitle -> "字幕开关"
                PlayerCustomShortcutAction.TogglePersistentBottomProgress -> "开关底部常驻迷你进度条"
                is PlayerCustomShortcutAction.SetPlaybackSpeed -> "设置播放速度：${action.speed}x"
                is PlayerCustomShortcutAction.SetResolution -> "设置分辨率：${action.qualityId}"
                is PlayerCustomShortcutAction.SetAudio -> "设置音频编码：${action.audio.getDisplayName(context)}"
                is PlayerCustomShortcutAction.SetVideoCodec -> "设置视频编码：${action.codec.getDisplayName(context)}"
                is PlayerCustomShortcutAction.SetAspectRatio -> "设置画面比例：${action.aspectRatio.getDisplayName(context)}"
                is PlayerCustomShortcutAction.SetDanmakuScale -> "设置弹幕大小：${action.scale.percentText()}"
                is PlayerCustomShortcutAction.SetDanmakuOpacity -> "设置弹幕透明度：${action.opacity.percentText()}"
                is PlayerCustomShortcutAction.SetDanmakuSpeedFactor -> "设置弹幕速度：${action.factor}x"
                is PlayerCustomShortcutAction.SetDanmakuArea -> "设置弹幕区域：${action.area.percentText()}"
                is PlayerCustomShortcutAction.SetDanmakuMaskEnabled -> "设置弹幕防遮挡：${if (action.enabled) "开启" else "关闭"}"
                is PlayerCustomShortcutAction.SetSubtitleFontSize -> "设置字幕字号：${action.sp} SP"
                is PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity -> "设置字幕背景透明度：${action.opacity.percentText()}"
                is PlayerCustomShortcutAction.SetSubtitleBottomPadding -> "设置字幕底部间距：${action.dp} DP"
            }
    }

    private infix fun PlayerCustomShortcutAction.entry(
        displayName: String
    ): PlayerCustomShortcutActionEntry {
        return PlayerCustomShortcutActionEntry(
            action = this,
            displayName = displayName
        )
    }

    private fun Float.percentText(): String {
        return "${(this * 100).roundToInt()}%"
    }
}
