package dev.aaa1115910.bv.entity

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import kotlin.math.roundToInt

data class PlayerCustomShortcutActionEntry(
    val action: PlayerCustomShortcutAction,
    val displayName: String,
    val valueDisplayName: String = displayName
)

data class PlayerCustomShortcutActionGroup(
    val id: String,
    val displayName: String,
    val action: PlayerCustomShortcutAction? = null,
    val values: List<PlayerCustomShortcutActionEntry> = emptyList()
)

object PlayerCustomShortcutCatalog {
    fun entries(context: Context): List<PlayerCustomShortcutActionEntry> {
        return groups(context).flatMap { group ->
            group.action?.let { listOf(it entry group.displayName) } ?: group.values
        }
    }

    fun groups(context: Context): List<PlayerCustomShortcutActionGroup> {
        return buildList {
            add(simple("show_info", "呼出播放信息层", PlayerCustomShortcutAction.ShowInfo))
            add(simple("open_settings", "打开播放器设置菜单", PlayerCustomShortcutAction.OpenSettings))
            add(simple("open_video_list", "打开视频列表", PlayerCustomShortcutAction.OpenVideoList))
            add(simple("open_related_videos", "打开相关视频", PlayerCustomShortcutAction.OpenRelatedVideos))
            add(simple("toggle_play_pause", "播放/暂停", PlayerCustomShortcutAction.TogglePlayPause))
            add(simple("play_previous", "上一个", PlayerCustomShortcutAction.PlayPrevious))
            add(simple("play_next", "下一个", PlayerCustomShortcutAction.PlayNext))
            add(simple("open_video_detail", "打开视频详情", PlayerCustomShortcutAction.OpenVideoDetail))
            add(simple("open_up_page", "打开 UP 主页", PlayerCustomShortcutAction.OpenUpPage))
            add(simple("toggle_loop", "单视频循环开关", PlayerCustomShortcutAction.ToggleLoop))
            add(simple("toggle_danmaku", "弹幕开关", PlayerCustomShortcutAction.ToggleDanmaku))
            add(simple("toggle_subtitle", "字幕开关", PlayerCustomShortcutAction.ToggleSubtitle))

            add(
                valueGroup(
                    id = "set_playback_speed",
                    displayName = "设置播放速度",
                    values = PlaySpeedItem.entries.map { speed ->
                        PlayerCustomShortcutAction.SetPlaybackSpeed(speed.speed).entry(
                            displayName = "设置播放速度：${speed.getDisplayName(context)}",
                            valueDisplayName = speed.getDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_resolution",
                    displayName = "设置分辨率",
                    values = Resolution.entries.map { resolution ->
                        PlayerCustomShortcutAction.SetResolution(resolution.code).entry(
                            displayName = "设置分辨率：${resolution.getShortDisplayName(context)}",
                            valueDisplayName = resolution.getShortDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_audio",
                    displayName = "设置音频编码",
                    values = Audio.entries.map { audio ->
                        PlayerCustomShortcutAction.SetAudio(audio).entry(
                            displayName = "设置音频编码：${audio.getDisplayName(context)}",
                            valueDisplayName = audio.getDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_video_codec",
                    displayName = "设置视频编码",
                    values = VideoCodec.entries.map { codec ->
                        PlayerCustomShortcutAction.SetVideoCodec(codec).entry(
                            displayName = "设置视频编码：${codec.getDisplayName(context)}",
                            valueDisplayName = codec.getDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_aspect_ratio",
                    displayName = "设置画面比例",
                    values = VideoAspectRatio.entries.map { aspectRatio ->
                        PlayerCustomShortcutAction.SetAspectRatio(aspectRatio).entry(
                            displayName = "设置画面比例：${aspectRatio.getDisplayName(context)}",
                            valueDisplayName = aspectRatio.getDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_danmaku_scale",
                    displayName = "设置弹幕大小",
                    values = listOf(0.5f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f).map { scale ->
                        PlayerCustomShortcutAction.SetDanmakuScale(scale).entry(
                            displayName = "设置弹幕大小：${scale.percentText()}",
                            valueDisplayName = scale.percentText()
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_danmaku_opacity",
                    displayName = "设置弹幕透明度",
                    values = listOf(0f, 0.25f, 0.5f, 0.7f, 0.85f, 1f).map { opacity ->
                        PlayerCustomShortcutAction.SetDanmakuOpacity(opacity).entry(
                            displayName = "设置弹幕透明度：${opacity.percentText()}",
                            valueDisplayName = opacity.percentText()
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_danmaku_speed_factor",
                    displayName = "设置弹幕速度",
                    values = DanmakuSpeedFactor.entries.map { factor ->
                        PlayerCustomShortcutAction.SetDanmakuSpeedFactor(factor.factor).entry(
                            displayName = "设置弹幕速度：${factor.getDisplayName(context)}",
                            valueDisplayName = factor.getDisplayName(context)
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_danmaku_area",
                    displayName = "设置弹幕区域",
                    values = listOf(0.25f, 0.5f, 0.75f, 1f).map { area ->
                        PlayerCustomShortcutAction.SetDanmakuArea(area).entry(
                            displayName = "设置弹幕区域：${area.percentText()}",
                            valueDisplayName = area.percentText()
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_danmaku_mask_enabled",
                    displayName = "设置弹幕防遮挡",
                    values = listOf(false, true).map { enabled ->
                        PlayerCustomShortcutAction.SetDanmakuMaskEnabled(enabled).entry(
                            displayName = "设置弹幕防遮挡：${if (enabled) "开启" else "关闭"}",
                            valueDisplayName = if (enabled) "开启" else "关闭"
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_subtitle_font_size",
                    displayName = "设置字幕字号",
                    values = listOf(12, 16, 20, 24, 32, 40, 48).map { fontSize ->
                        PlayerCustomShortcutAction.SetSubtitleFontSize(fontSize).entry(
                            displayName = "设置字幕字号：${fontSize.sp.value.toInt()} SP",
                            valueDisplayName = "${fontSize.sp.value.toInt()} SP"
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_subtitle_background_opacity",
                    displayName = "设置字幕背景透明度",
                    values = listOf(0f, 0.25f, 0.4f, 0.5f, 0.75f, 1f).map { opacity ->
                        PlayerCustomShortcutAction.SetSubtitleBackgroundOpacity(opacity).entry(
                            displayName = "设置字幕背景透明度：${opacity.percentText()}",
                            valueDisplayName = opacity.percentText()
                        )
                    }
                )
            )

            add(
                valueGroup(
                    id = "set_subtitle_bottom_padding",
                    displayName = "设置字幕底部间距",
                    values = listOf(0, 8, 12, 16, 24, 32, 48).map { padding ->
                        PlayerCustomShortcutAction.SetSubtitleBottomPadding(padding).entry(
                            displayName = "设置字幕底部间距：${padding.dp.value.toInt()} DP",
                            valueDisplayName = "${padding.dp.value.toInt()} DP"
                        )
                    }
                )
            )

            add(
                simple(
                    "toggle_persistent_bottom_progress",
                    "开关底部常驻迷你进度条",
                    PlayerCustomShortcutAction.TogglePersistentBottomProgress
                )
            )
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

    private fun simple(
        id: String,
        displayName: String,
        action: PlayerCustomShortcutAction
    ): PlayerCustomShortcutActionGroup {
        return PlayerCustomShortcutActionGroup(
            id = id,
            displayName = displayName,
            action = action
        )
    }

    private fun valueGroup(
        id: String,
        displayName: String,
        values: List<PlayerCustomShortcutActionEntry>
    ): PlayerCustomShortcutActionGroup {
        return PlayerCustomShortcutActionGroup(
            id = id,
            displayName = displayName,
            values = values
        )
    }

    private infix fun PlayerCustomShortcutAction.entry(displayName: String): PlayerCustomShortcutActionEntry {
        return entry(displayName, displayName)
    }

    private fun PlayerCustomShortcutAction.entry(
        displayName: String,
        valueDisplayName: String
    ): PlayerCustomShortcutActionEntry {
        return PlayerCustomShortcutActionEntry(
            action = this,
            displayName = displayName,
            valueDisplayName = valueDisplayName
        )
    }

    private fun Float.percentText(): String {
        return "${(this * 100).roundToInt()}%"
    }
}
