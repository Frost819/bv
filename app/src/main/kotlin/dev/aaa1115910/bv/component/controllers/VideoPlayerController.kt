package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.player.DanmakuSettingAction
import dev.aaa1115910.bv.viewmodel.player.MediaProfileSettingAction
import dev.aaa1115910.bv.viewmodel.player.SubtitleSettingAction
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    aid: Long,
    fromSeason: Boolean,
    proxyArea: ProxyArea,
    isLooping: Boolean,
    uiState: PlayerUiState,
    seekerState: State<SeekerState>,

    //player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToStart: () -> Unit,
    onCancelSkipToNextEp: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit,

    //menu events
    onMediaProfileSettingChange: (MediaProfileSettingAction) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSettingChange: (SubtitleSettingAction) -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,

    onEnsureUgcPagesLoaded: (aid: Long) -> Unit,

    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger {}

    //var showListController by remember { mutableStateOf(false) }
    var showUpPanelController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showInfoSeekController by remember { mutableStateOf(false) }
    var showRelatedVideosController by remember { mutableStateOf(false) }
    val showClickableControllers by remember { derivedStateOf { showUpPanelController || showMenuController || showInfoSeekController || showRelatedVideosController } }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var goTime by remember { mutableLongStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }

    var seekCountdown: Job? by remember { mutableStateOf(null) }
    var hideInfoSeekControllerCountdown: Job? by remember { mutableStateOf(null) }

    fun calCoefficient(): Int {
        return if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    fun onTimeForward() {
        isSeeking = true
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime =
            if (targetTime > seekerState.value.totalDuration) seekerState.value.totalDuration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeForward: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    fun onTimeBack() {
        isSeeking = true
        val targetTime = goTime - (10000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        logger.info { "onTimeBack: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    fun startSeekCountdown() {
        seekCountdown?.cancel()
        seekCountdown = scope.launch {
            delay(1000)

            onGoTime(goTime)
            if (!videoPlayer.isPlaying) onPlay()

            isSeeking = false
            showInfoSeekController = false
            hideInfoSeekControllerCountdown?.cancel()
        }
    }

    fun onDirectionLeft() {
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeBack()
        startSeekCountdown()
    }

    fun onDirectionRight() {
        if (!isSeeking) goTime = seekerState.value.currentTime
        onTimeForward()
        startSeekCountdown()
    }

    fun onSeekGoTime() {
        onGoTime(goTime)
        isSeeking = false
        if (!videoPlayer.isPlaying) onPlay()
        showInfoSeekController = false
        seekCountdown?.cancel()
    }

    fun onPlayPause() {
        if (videoPlayer.isPlaying) onPause() else onPlay()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        // 中键需要区分短按和长按
        val isConfirmKey =
            event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.Spacebar

        if (event.type == KeyEventType.KeyUp && !isConfirmKey) {
            return true
        }

        logger.info { "[${event.key} press]" }

        when (event.key) {
            Key.Back -> {
                if (showClickableControllers) {
                    showMenuController = false
                    showUpPanelController = false
                    showInfoSeekController = false
                    showRelatedVideosController = false
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPressBack < 3000) {
                        onExit()
                    } else {
                        lastPressBack = currentTime
                        R.string.video_player_press_back_again_to_exit.toast(context)
                    }
                }
                return true
            }

            Key.Menu -> {
                showInfoSeekController = false
                showMenuController = !showMenuController
                return true
            }

            Key(763) -> {
                showMenuController = true
                return true
            }

            Key.MediaPlayPause -> {
                onPlayPause()
                return true
            }

            Key.MediaPlay -> {
                if (!videoPlayer.isPlaying) onPlay()
                return true
            }

            Key.MediaPause -> {
                if (videoPlayer.isPlaying) onPause()
                return true
            }
        }

        if (showClickableControllers) {
            return false
        } else {
            when (event.key) {
                Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.isLongPress) {
                            showMenuController = true
                        }
                        return true
                    } else {
                        if (uiState.showBackToStart) {
                            onBackToStart()
                        } else {
                            onPlayPause()
                        }
                        return true
                    }
                }

                Key.DirectionUp -> {
                    showUpPanelController = true
                    return true
                }

                Key.DirectionDown -> {
                    showInfoSeekController = true
                    return true
                }

                Key.MediaRewind, Key.DirectionLeft -> {
                    if (uiState.showSkipToNextEp) onCancelSkipToNextEp()
                    showInfoSeekController = true
                    onDirectionLeft()
                    return true
                }

                Key.MediaFastForward, Key.DirectionRight -> {
                    showInfoSeekController = true
                    onDirectionRight()
                    return true
                }
            }
        }

        return false
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                // 重置 info 控制器的隐藏倒计时 (只要有按键活动就重置)
                if (showInfoSeekController) {
                    hideInfoSeekControllerCountdown?.cancel()
                    hideInfoSeekControllerCountdown = scope.launch {
                        delay(5000)
                        showInfoSeekController = false
                    }
                }
                // 调用分离出去的处理函数
                handleKeyEvent(event)
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
                    text = seekerState.value.debugInfo
                )
            }
        }
        if (uiState.subtitleId != -1L) {
            val currentTime = seekerState.value.currentTime

            BottomSubtitle(
                subtitleData = uiState.subtitleData,
                currentTime = currentTime,
                fontSize = uiState.subtitleState.fontSize,
                opacity = uiState.subtitleState.opacity,
                padding = uiState.subtitleState.bottomPadding,
            )
        }

        SkipTips(
            historyTime = uiState.lastPlayed.toLong(),
            showBackToStart = uiState.showBackToStart,
            showSkipToNextEp = uiState.showSkipToNextEp,
        )

        PlayStateTips(
            isPlaying = uiState.playerState == PlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState is PlayerState.Error,
            errorMessage = (uiState.playerState as? PlayerState.Error)?.message,
            needPay = uiState.needPay,
            epid = uiState.epid ?: 0,
        )

        RelatedVideosController(
            show = showRelatedVideosController,
            relatedVideos = uiState.relatedVideos,
            onVideoClicked = onRelatedVideoClicked
        )

        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showInfoSeekController,
            isSeeking = isSeeking,
            goTime = goTime,
            seekerState = seekerState.value,
            title = uiState.title,
            clock = uiState.clock,
            videoShot = uiState.videoShot,
            videoShotCache = uiState.videoShotCache,
            fromSeason = fromSeason,
            danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty(),
            isLooping = isLooping,
            onDirectionLeft = { onDirectionLeft() },
            onDirectionRight = { onDirectionRight() },
            onSeekGoTime = { onSeekGoTime() },
            onPlayPause = { onPlayPause() },
            onDanmakuSwitchChange = {
                if (uiState.danmakuState.enabledTypes.isEmpty()) {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(DanmakuType.entries))
                } else {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(emptyList()))
                }
            },
            onShowSettings = {
                showInfoSeekController = false
                showMenuController = true
            },
            onShowRelatedVideos = {
                showInfoSeekController = false
                showRelatedVideosController = true
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

        /*VideoListController(
            show = showListController,
            currentAid = uiState.aid,currentCid = uiState.cid,
            videoList = uiState.availableVideoList,
            onEnsureUgcPagesLoaded = onEnsureUgcPagesLoaded,onPlayNewVideo = onPlayNewVideo
        )*/
                UpPanelController(
                    show = showUpPanelController,
                    uiState = uiState,
                    currentTimeMs = seekerState.value.currentTime,
                    isPlaying = videoPlayer.isPlaying,
                    onDismiss = { showUpPanelController = false },
                    onGoTime = { targetMs ->
                        onGoTime(targetMs)
                        if (!videoPlayer.isPlaying) onPlay()
                    },
                    onPlay = onPlay,
                    onPlayNewVideo = onPlayNewVideo,
                    onEnsureUgcPagesLoaded = onEnsureUgcPagesLoaded
                )

        MenuController(
            show = showMenuController,
            uiState = uiState,
            onResolutionChange = { qualityId ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetQuality(qualityId)
                )
            },
            onCodecChange = { codec ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetVideoCodec(codec)
                )
            },
            onAudioChange = { audio ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetAudio(audio)
                )
            },
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onDanmakuSwitchChange = { danmakuTypes ->
                onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(danmakuTypes))
            },
            onDanmakuSizeChange = { scale ->
                onDanmakuSettingChange(DanmakuSettingAction.SetScale(scale))
            },
            onDanmakuOpacityChange = { opacity ->
                onDanmakuSettingChange(DanmakuSettingAction.SetOpacity(opacity))
            },
            onDanmakuSpeedFactorChange = { factor ->
                onDanmakuSettingChange(DanmakuSettingAction.SetSpeedFactor(factor))
            },
            onDanmakuAreaChange = { area ->
                onDanmakuSettingChange(DanmakuSettingAction.SetArea(area))
            },
            onDanmakuMaskChange = { enabled ->
                onDanmakuSettingChange(DanmakuSettingAction.SetMaskEnabled(enabled))
            },
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = { size ->
                onSubtitleSettingChange(SubtitleSettingAction.SetFontSize(size))
            },
            onSubtitleBackgroundOpacityChange = { opacity ->
                onSubtitleSettingChange(SubtitleSettingAction.SetOpacity(opacity))
            },
            onSubtitleBottomPadding = { padding ->
                onSubtitleSettingChange(SubtitleSettingAction.SetBottomPadding(padding))
            }
        )
    }
}



