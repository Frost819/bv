package dev.aaa1115910.bv.component.controllers

import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.entity.LocalVideoPlayerControllerData
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.util.countDownTimer
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    aid: Long,
    fromSeason: Boolean,
    proxyArea: ProxyArea,
    isLooping: Boolean,

    //player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToStart: () -> Unit,
    onBackToHistory: () -> Unit,
    onCancelSkipToNextEp: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,

    //menu events
    onResolutionChange: (Int) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onSelectedPlaySpeedItemChange: (PlaySpeedItem) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,

    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val data = LocalVideoPlayerControllerData.current
    val logger = KotlinLogging.logger {}

    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showInfoSeekController by remember { mutableStateOf(false) }
    val showClickableControllers by remember { derivedStateOf { showListController || showMenuController || showInfoSeekController } }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var goTime by remember { mutableLongStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }

    var onTimeForwardBackTimer: CountDownTimer? by remember { mutableStateOf(null) }

    val calCoefficient = {
        if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    val onTimeForward = {
        isSeeking = true
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime =
            if (targetTime > data.infoData.totalDuration) data.infoData.totalDuration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeForward: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }
    val onTimeBack = {
        isSeeking = true
        val targetTime = goTime - (10000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeBack: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    val onDirectionLeft = {
        if (!isSeeking) goTime = data.infoData.currentTime
        onTimeBack()
        onTimeForwardBackTimer?.cancel()
        onTimeForwardBackTimer = countDownTimer(1000, 100, "onTimeBackTimer") {
            onGoTime(goTime)
            isSeeking = false
            if (!videoPlayer.isPlaying) onPlay()
            showInfoSeekController = false
            onTimeForwardBackTimer = null
        }
    }

    val onDirectionRight = {
        if (!isSeeking) goTime = data.infoData.currentTime
        onTimeForward()
        onTimeForwardBackTimer?.cancel()
        onTimeForwardBackTimer = countDownTimer(1000, 100, "onTimeBackTimer") {
            onGoTime(goTime)
            isSeeking = false
            if (!videoPlayer.isPlaying) onPlay()
            showInfoSeekController = false
            onTimeForwardBackTimer = null
        }
    }

    val onSeekGoTime = {
        onGoTime(goTime)
        isSeeking = false
        if (!videoPlayer.isPlaying) onPlay()
        showInfoSeekController = false
        onTimeForwardBackTimer?.cancel()
        onTimeForwardBackTimer = null
    }

    val onPlayPause = {
        if (videoPlayer.isPlaying) onPause() else onPlay()
    }

    //有历史播放记录时自动跳转播放进度
    LaunchedEffect(data.showBackToStart) {
        if (data.showBackToStart)
            onBackToHistory()
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent {
                if (showClickableControllers) {
                    if (!listOf(Key.Back, Key.Menu).contains(it.key)) {
                        return@onPreviewKeyEvent false
                    }

                }
                when (it.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        if (!showClickableControllers && data.showBackToStart) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            onBackToStart()
                            return@onPreviewKeyEvent true
                        }

                        if (showInfoSeekController) {
                            return@onPreviewKeyEvent false
                        }

                        if (it.nativeKeyEvent.isLongPress) {
                            logger.fInfo { "[${it.key}] long press" }
                            showMenuController = true
                            return@onPreviewKeyEvent true
                        }

                        logger.fInfo { "[${it.key}] short press" }
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        onPlayPause()
                        return@onPreviewKeyEvent false
                    }

                    // KEYCODE_CENTER_LONG
                    // 一切设备上长按 DirectionCenter 键会是这个按键事件
                    Key(763) -> {
                        showMenuController = true
                        return@onPreviewKeyEvent true
                    }


                    Key.Menu -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        showMenuController = !showMenuController
                        return@onPreviewKeyEvent true
                    }

                    Key.Back -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }

                        // 显示controller时，点击返回键关闭所有controller
                        if (showClickableControllers) {
                            showMenuController = false
                            showListController = false
                            showInfoSeekController = false
                        } else {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastPressBack < 1000 * 3) {
                                logger.fInfo { "Exiting video player" }
                                onExit()
                            } else {
                                lastPressBack = currentTime
                                R.string.video_player_press_back_again_to_exit.toast(context)
                            }
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlayPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        onPlayPause()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlay -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (!videoPlayer.isPlaying) onPlay()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (videoPlayer.isPlaying) onPause()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaRewind -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (data.showSkipToNextEp) {
                            onCancelSkipToNextEp()
                        }
                        if (!showInfoSeekController) {
                            showInfoSeekController = true
                            onDirectionLeft()
                            return@onPreviewKeyEvent true
                        }
                    }

                    Key.MediaFastForward -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        if (!showInfoSeekController) {
                            showInfoSeekController = true
                            onDirectionRight()
                            return@onPreviewKeyEvent true
                        }
                    }

                    Key.DirectionLeft -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (data.showSkipToNextEp) {
                            onCancelSkipToNextEp()
                        }
                        if (!showInfoSeekController) {
                            showInfoSeekController = true
                            onDirectionLeft()
                            return@onPreviewKeyEvent true
                        }
                    }

                    Key.DirectionRight -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        if (!showInfoSeekController) {
                            showInfoSeekController = true
                            onDirectionRight()
                            return@onPreviewKeyEvent true
                        }
                        logger.info { "[${it.key} press]" }

                    }

                    Key.DirectionUp -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (showClickableControllers) return@onPreviewKeyEvent false
                        showListController = true
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (showClickableControllers) {
                            return@onPreviewKeyEvent false
                        } else {
                            showInfoSeekController = true
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                false
            }
    ) {
        content()
        if (BuildConfig.DEBUG) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = data.debugInfo
                )
            }
        }
        BottomSubtitle()
        SkipTips(
            historyTime = data.lastPlayed.toLong(),
            showBackToStart = data.showBackToStart,
            showSkipToNextEp = data.showSkipToNextEp,
        )
        PlayStateTips(
            isPlaying = data.isPlaying,
            isBuffering = data.isBuffering,
            isError = data.isError,
            exception = data.exception,
            needPay = data.needPay,
            epid = data.epid
        )
        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showInfoSeekController,
            isSeeking = isSeeking,
            goTime = goTime,
            infoData = data.infoData,
            title = data.title,
            clock = data.clock,
            videoShot = data.videoShot,
            fromSeason = fromSeason,
            danmakuEnabled = data.currentDanmakuEnabledList.isNotEmpty(),
            isLooping = isLooping,
            onHideInfo = { showInfoSeekController = false },
            onDirectionLeft = onDirectionLeft,
            onDirectionRight = onDirectionRight,
            onSeekGoTime = onSeekGoTime,
            onPlayPause = onPlayPause,
            onDanmakuSwitchChange = {
                if (data.currentDanmakuEnabledList.isEmpty()) {
                    onDanmakuSwitchChange(DanmakuType.entries)
                } else {
                    onDanmakuSwitchChange(listOf())
                }
            },
            onShowSettings = {
                showInfoSeekController = false
                showMenuController = true
            },
            onGoToVideoInfo = {
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = aid,
                    fromSeason = fromSeason,
                    fromController = true,
                    proxyArea = proxyArea
                )
            },
            onToggleLoop = onToggleLoop,
            onGoToUpPage = onGoToUpPage
        )
        VideoListController(
            show = showListController,
            currentCid = data.currentVideoCid,
            videoList = data.availableVideoList,
            onPlayNewVideo = onPlayNewVideo
        )
        MenuController(
            show = showMenuController,
            onResolutionChange = onResolutionChange,
            onCodecChange = onCodecChange,
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onSelectedPlaySpeedItemChange = onSelectedPlaySpeedItemChange,
            onAudioChange = onAudioChange,
            onDanmakuSwitchChange = onDanmakuSwitchChange,
            onDanmakuSizeChange = onDanmakuSizeChange,
            onDanmakuOpacityChange = onDanmakuOpacityChange,
            onDanmakuAreaChange = onDanmakuAreaChange,
            onDanmakuMaskChange = onDanmakuMaskChange,
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = onSubtitleSizeChange,
            onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
            onSubtitleBottomPadding = onSubtitleBottomPadding
        )
    }
}



