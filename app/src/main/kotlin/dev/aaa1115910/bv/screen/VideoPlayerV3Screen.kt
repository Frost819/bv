package dev.aaa1115910.bv.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.component.DanmakuPlayerCompose
import dev.aaa1115910.bv.component.controllers.VideoPlayerController
import dev.aaa1115910.bv.component.controllers.VideoProgressSeek
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.BvVideoPlayer
import dev.aaa1115910.bv.ui.effect.PlayerUiEffect
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.util.DanmakuMaskFinder
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.VideoShotImageCache
import dev.aaa1115910.bv.util.calculateMaskDelay
import dev.aaa1115910.bv.util.danmakuMask
import dev.aaa1115910.bv.viewmodel.player.VideoPlayerV3ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import kotlin.math.absoluteValue

@Composable
fun VideoPlayerV3Screen(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel = koinViewModel()
) {
    val logger = KotlinLogging.logger { }
    val context = LocalContext.current
    val videoPlayer = playerViewModel.videoPlayer
    val danmakuPlayer = playerViewModel.danmakuPlayer

    val maskFinder = remember { DanmakuMaskFinder() }
    var currentDanmakuMaskFrame: DanmakuMaskFrame? by remember { mutableStateOf(null) }

    var isLooping by remember { mutableStateOf(false) }
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }
    val uiState by playerViewModel.uiState.collectAsState()
    val seekerState = playerViewModel.seekerState.collectAsState()

    val videoShotCache by remember(uiState.videoShot) { mutableStateOf(VideoShotImageCache()) }

    LaunchedEffect(Unit) {
        playerViewModel.uiEffect.collect { effect ->
            when (effect) {
                PlayerUiEffect.FinishActivity -> {
                    (context as Activity).finish()
                }

                PlayerUiEffect.PlayEnded -> {
                    if (isLooping) {
                        playerViewModel.backToStart()
                        return@collect
                    }

                    playerViewModel.checkAndPlayNext()
                }
            }
        }
    }

    // 循环发送心跳
    LaunchedEffect(Unit) {
        delay(5000)
        while (isActive) {
            if (uiState.playerState == PlayerState.Playing) playerViewModel.trySendHeartbeat()
            // 周期延迟
            delay(15000)
        }
    }

    // 弹幕防遮挡蒙版更新
    LaunchedEffect(uiState.danmakuState.maskEnabled, uiState.danmakuMask) {
        if (!uiState.danmakuState.maskEnabled || uiState.danmakuMask == null) {
            currentDanmakuMaskFrame = null
            return@LaunchedEffect
        }

        // 当 mask 列表变化（如切集）或开关变化时，重置查找器缓存
        maskFinder.reset()

        val mask = uiState.danmakuMask ?: return@LaunchedEffect

        var lastCheckTime = -1L

        while (isActive) {
            val currentTime = seekerState.value.currentTime
            val isPlaying = uiState.playerState == PlayerState.Playing

            // 判断是否发生了 Seek (时间突变 > 200ms)
            val isTimeJumping = (currentTime - lastCheckTime).absoluteValue > 200

            // 2. 只有在播放中或刚刚发生 Seek 时才进行计算，否则低频休眠
            if (isPlaying || isTimeJumping) {
                // 使用工具类查找 Frame
                val foundFrame = maskFinder.findFrame(mask, currentTime)

                // 状态去重更新
                if (currentDanmakuMaskFrame != foundFrame) {
                    currentDanmakuMaskFrame = foundFrame
                }

                lastCheckTime = currentTime

                // 3. 计算休眠时间
                val delayTime = calculateMaskDelay(foundFrame, currentTime, isPlaying)
                delay(delayTime)
            } else {
                // 暂停且未拖动进度条，降低 CPU 占用
                delay(500L)
            }
        }
    }

    VideoPlayerController(
        modifier = modifier,
        aid = uiState.aid,
        fromSeason = uiState.fromSeason,
        proxyArea = ProxyArea.MainLand,
        isLooping = isLooping,
        isPlaying = videoPlayer?.isPlaying ?: false,
        videoShotCache = videoShotCache,
        uiState = uiState,
        seekerState = seekerState,
        onPlay = { videoPlayer?.start() },
        onPause = {
            videoPlayer?.pause()
            playerViewModel.trySendHeartbeat()
        },
        onExit = {
            (context as Activity).finish()
        },
        onGoTime = { time ->
            playerViewModel.seekToTime(time)
        },
        onBackToStart = { playerViewModel.backToStart() },
        onPlayNewVideo = {
            playerViewModel.trySendHeartbeat()
            playerViewModel.playNewVideo(it)
        },
        onPlayPrevious = {
            playerViewModel.playPreviousNow()
        },
        onPlayNext = {
            playerViewModel.playNextNow()
        },
        onCancelSkipToNextEp = {
            playerViewModel.cancelPlayNext()
        },
        onToggleLoop = {
            isLooping = !isLooping
        },
        onToggleSubtitle = {
            playerViewModel.toggleSubtitle()
        },
        onTogglePersistentSeek = {
            showPersistentSeek = !showPersistentSeek
            Prefs.showPersistentSeek = showPersistentSeek
        },
        onGoToUpPage = {
            UpInfoActivity.actionStart(
                context,
                mid = uiState.authorMid,
                name = uiState.authorName
            )
        },

        onMediaProfileSettingChange = { action ->
            playerViewModel.updateMediaProfile(action)
        },
        onAspectRatioChange = { aspectRadio ->
            playerViewModel.updateVideoAspectRatio(aspectRadio)
        },
        onPlaySpeedChange = { speed ->
            logger.info { "Set default play speed: $speed" }
            playerViewModel.updatePlaySpeed(speed)
        },
        onDanmakuSettingChange = { action ->
            playerViewModel.updateDanmakuState(action)
            logger.info { "On danmaku state change" }
        },
        onSubtitleChange = { subtitle ->
            playerViewModel.loadSubtitle(subtitle.id)
        },
        onSubtitleSettingChange = { action ->
            logger.info { "On subtitle config change" }
            playerViewModel.updateSubtitleState(action)
        },
        onRelatedVideoClicked = { video ->
            video.cid?.let {
                playerViewModel.playNewVideo(
                    VideoListItem(
                        aid = video.avid,
                        cid = video.cid,
                        title = video.title,
                    )
                )
            }
        },
    ) {
        Box(
            modifier = Modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(Unit) {
                videoPlayer?.setOptions()
            }

            val aspectRatio = when (uiState.aspectRatio) {
                VideoAspectRatio.Default -> {
                    if (uiState.videoHeight > 0 && uiState.videoWidth > 0) {
                        uiState.videoWidth / uiState.videoHeight.toFloat()
                    } else {
                        16 / 9f
                    }
                }

                VideoAspectRatio.FourToThree -> 4 / 3f
                VideoAspectRatio.SixteenToNine -> 16 / 9f
            }

            BvVideoPlayer(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectRatio)
                    .align(Alignment.Center),
                videoPlayer = videoPlayer,
            )
            DanmakuPlayerCompose(
                modifier = Modifier
                    .fillMaxSize()
                    // 在之前版本中，设置 DanmakuConfig 透明度后，更改其它弹幕设置后，可能会导致弹幕透明度
                    // 突然变成完全不透明一瞬间，因此这次新版选择直接在此处设置透明度
                    .alpha(uiState.danmakuState.opacity)
                    .ifElse(
                        { Prefs.defaultDanmakuMask },
                        Modifier.danmakuMask(currentDanmakuMaskFrame, aspectRatio)
                    ),
                danmakuPlayer = danmakuPlayer
            )
            if (showPersistentSeek) {
                VideoProgressSeek(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    duration = seekerState.value.totalDuration,
                    position = seekerState.value.currentTime,
                    bufferedPercentage = seekerState.value.bufferedPercentage,
                    isPersistentSeek = true
                )
            }
        }
    }
}
