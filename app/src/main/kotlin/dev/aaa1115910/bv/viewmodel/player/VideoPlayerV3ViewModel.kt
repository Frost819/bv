package dev.aaa1115910.bv.viewmodel.player

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerListener
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.screen.settings.content.ActionAfterPlayItems
import dev.aaa1115910.bv.ui.effect.PlayerUiEffect
import dev.aaa1115910.bv.ui.state.DanmakuState
import dev.aaa1115910.bv.ui.state.MediaProfileState
import dev.aaa1115910.bv.ui.state.PlayerState
import dev.aaa1115910.bv.ui.state.PlayerUiState
import dev.aaa1115910.bv.ui.state.SeekerState
import dev.aaa1115910.bv.ui.state.SubtitleState
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.android.annotation.KoinViewModel
import java.net.URI
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

@KoinViewModel

class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
        private set
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private var playData: PlayData? = null
    private val typeFilter = TypeFilter()
    private var danmakuConfig = DanmakuConfig()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _seekerState = MutableStateFlow(SeekerState())
    val seekerState = _seekerState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var seekerUpdateJob: Job? = null
    private var clockUpdateJob: Job? = null
    private var heartbeatJob: Job? = null

    private var backToStartCountdownJob: Job? = null
    private var playNextCountdownJob: Job? = null

    // --- UGC 分P后台预取（以当前父项为中心，向两侧交替） ---
    private var ugcPagesPrefetchJob: Job? = null

    // 防止 videoList 因 ugcPages 填充而频繁 emit 导致反复重启：
    // 仅当“中心 aid 变化”或“父项列表(aid顺序)变化”时重启。
    private var lastPrefetchCenterAid: Long = -1L
    private var lastPrefetchAidsHash: Int = 0

    // 降速参数：避免风控/限流（可按需调整）
    private val ugcPagesPrefetchDelayMs = 800L
    private val ugcPagesPrefetchFailureDelayMs = 3000L

    private val videoPlayerListener = object : VideoPlayerListener {
        override fun onError(error: Exception) {
            logger.info { "onError: $error" }
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(
                        error.message ?: "Unknown error"
                    )
                )
            }
        }

        override fun onReady() {
            logger.info { "onReady" }
            _uiState.update { it.copy(playerState = PlayerState.Ready) }

            updatePlaySpeed(forceUpdate = true)
            startSeekerUpdater()
        }

        override fun onPlay() {
            logger.info { "onPlay" }
            danmakuPlayer?.start()
            _uiState.update { it.copy(playerState = PlayerState.Playing, isBuffering = false) }

            if (_uiState.value.lastPlayed > 0) {
                seekToLastPlayed()
                _uiState.update { it.copy(lastPlayed = 0) }
            }
        }

        override fun onPause() {
            logger.info { "onPause" }

            danmakuPlayer?.pause()
            _uiState.update { it.copy(playerState = PlayerState.Paused) }
        }

        override fun onBuffering() {
            logger.info { "onBuffering" }

            danmakuPlayer?.pause()
            _uiState.update { it.copy(isBuffering = true) }
        }

        override fun onEnd() {
            logger.info { "onEnd" }

            danmakuPlayer?.pause()
            stopSeekerUpdater()

            _uiState.update { it.copy(playerState = PlayerState.Ended) }
            viewModelScope.launch {
                _uiEffect.emit(PlayerUiEffect.PlayEnded)
            }
        }

        override fun onSeekBack(seekBackIncrementMs: Long) {
        }

        override fun onSeekForward(seekForwardIncrementMs: Long) {
        }
    }

    init {
        videoInfoRepository.videoList
            .onEach { newList ->
                _uiState.update { currentState ->
                    currentState.copy(availableVideoList = newList)
                }
                logger.fInfo { "Sync video list from repo, size: ${newList.size}" }
                // 尝试启动/重启：用于“首次拿到小合集父项列表”时开始后台预取
                restartUgcPagesPrefetchIfNeeded(newList)
            }
            .launchIn(viewModelScope)

        videoInfoRepository.videoDetailState
            .onEach { newDetail ->
                if (newDetail == null) return@onEach

                _uiState.update { currentState ->
                    currentState.copy(
                        relatedVideos = newDetail.relatedVideos,
                        ugcSeason = newDetail.ugcSeason
                    )
                }
                logger.fInfo { "Sync related videos from repo" }
            }
            .launchIn(viewModelScope)

        uiState
            .map { it.aid to it.cid }
            .distinctUntilChanged()
            .mapLatest { (aid, cid) ->
                // cid/aid 不完整时直接清空
                if (aid == 0L || cid == 0L) return@mapLatest emptyList()

                // 先清空，避免旧章节残留
                _uiState.update { it.copy(availableChapters = emptyList()) }

                val viewPoints = withContext(Dispatchers.IO) {
                    videoPlayRepository.getViewPoints(aid = aid, cid = cid)
                }

                viewPoints.mapNotNull { element ->
                    val obj = element as? JsonObject ?: return@mapNotNull null
                    val content = obj["content"]?.jsonPrimitive?.content.orEmpty()
                    val from = obj["from"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                    val to = obj["to"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    if (content.isBlank()) return@mapNotNull null

                    dev.aaa1115910.bv.ui.state.PlayerChapter(
                        content = content,
                        fromSec = from,
                        toSec = to
                    )
                }
            }
            .onEach { chapters ->
                _uiState.update { it.copy(availableChapters = chapters) }
            }
            .launchIn(viewModelScope)
    }

    fun init(
        aid: Long,
        cid: Long,
        epid: Int?,
        title: String,
        lastPlayed: Int,
        fromSeason: Boolean,
        subType: Int,
        seasonId: Int,
        proxyArea: ProxyArea = ProxyArea.MainLand,
        authorMid: Long = 0,
        authorName: String
    ) {
        loadPlayUrl(
            avid = aid,
            cid = cid,
            epid = epid.takeIf { it != 0 },
            title = title
        )

        _uiState.update {
            it.copy(
                lastPlayed = lastPlayed,
                fromSeason = fromSeason,
                subType = subType,
                epid = epid,
                seasonId = seasonId,
                proxyArea = proxyArea,
                authorMid = authorMid,
                authorName = authorName,
                mediaProfileState = MediaProfileState(
                    qualityId = Prefs.defaultQuality.code,
                    videoCodec = Prefs.defaultVideoCodec,
                    audio = Prefs.defaultAudio
                ),
                playSpeed = Prefs.defaultPlaySpeed.speed,
                danmakuState = DanmakuState(
                    scale = Prefs.defaultDanmakuScale,
                    opacity = Prefs.defaultDanmakuOpacity,
                    area = Prefs.defaultDanmakuArea,
                    speedFactor = Prefs.defaultDanmakuSpeedFactor,
                    maskEnabled = Prefs.defaultDanmakuMask,
                    enabledTypes = Prefs.defaultDanmakuTypes,
                ),
                subtitleState = SubtitleState(
                    fontSize = Prefs.defaultSubtitleFontSize,
                    opacity = Prefs.defaultSubtitleBackgroundOpacity,
                    bottomPadding = Prefs.defaultSubtitleBottomPadding
                )
            )
        }

        startClockUpdater()
    }

    fun attachPlayer(player: AbstractVideoPlayer) {
        videoPlayer = player
        videoPlayer?.setPlayerEventListener(videoPlayerListener)
    }

    fun dettachPlayer() {
        val player = videoPlayer
        if (player != null && !Prefs.incognitoMode) {
            val currentTime = (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
            val totalTime = (player.duration.coerceAtLeast(0) / 1000).toInt()
            val reportTime = if (currentTime >= totalTime) -1 else currentTime

            // 用于更新详情页播放进度
            videoInfoRepository.updateHistory(
                progress = reportTime,
                lastPlayedCid = _uiState.value.cid
            )

            // 最后一次发送心跳
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) { // 使用GlobalScope，避免ViewModel销毁导致协程域取消
                try {
                    withTimeout(3000L) { // 3秒超时
                        uploadHistory(reportTime)
                    }
                } catch (e: Exception) {
                    logger.warn { "Failed to upload history on detach: $e" }
                }
            }
        }

        videoPlayer?.release()
        videoPlayer = null
    }

    fun initDanmakuPlayer() {
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        initDanmakuConfig()
    }

    fun releaseDanmakuPlayer() {
        danmakuPlayer?.release()
        danmakuPlayer = null
    }

    fun loadSubtitle(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                _uiState.update {
                    it.copy(
                        subtitleId = -1,
                        subtitleData = emptyList()
                    )
                }
                return@launch
            }
            var subtitleName = ""
            runCatching {
                val subtitle =
                    _uiState.value.availableSubtitles.find { it.id == id } ?: return@runCatching
                subtitleName = subtitle.langDoc
                logger.info { "Subtitle url: ${subtitle.url}" }
                val client = HttpClient(OkHttp)
                val responseText = client.get(subtitle.url).bodyAsText()
                val subtitleData = SubtitleParser.fromBccString(responseText)
                _uiState.update {
                    it.copy(
                        subtitleId = id,
                        subtitleData = subtitleData
                    )
                }
            }.onFailure {
                logger.fInfo { "Load subtitle failed: ${it.stackTraceToString()}" }
            }.onSuccess {
                logger.fInfo { "Load subtitle $subtitleName success" }
            }
        }
    }

    fun updatePlaySpeed(
        speed: Float? = null,
        forceUpdate: Boolean = false
    ) {
        val currentSpeed = _uiState.value.playSpeed
        val targetSpeed = speed ?: currentSpeed

        if (!forceUpdate && currentSpeed == targetSpeed) return

        _uiState.update { it.copy(playSpeed = targetSpeed) }
        videoPlayer?.speed = targetSpeed
        danmakuPlayer?.updatePlaySpeed(targetSpeed)
    }

    fun updateVideoAspectRatio(aspectRatio: VideoAspectRatio) {
        _uiState.update {
            it.copy(aspectRatio = aspectRatio)
        }
    }

    fun updateMediaProfile(action: MediaProfileSettingAction) {
        val old = _uiState.value.mediaProfileState
        val new = when (action) {
            is MediaProfileSettingAction.SetQuality -> old.copy(qualityId = action.value)
            is MediaProfileSettingAction.SetVideoCodec -> old.copy(videoCodec = action.value)
            is MediaProfileSettingAction.SetAudio -> old.copy(audio = action.value)
        }

        if (old == new) return

        _uiState.update { it.copy(mediaProfileState = new) }

        videoPlayer?.let { player ->
            player.pause()
            val currentPosition = player.currentPosition

            playQuality(new.qualityId, new.videoCodec, new.audio)

            if (currentPosition > 0) {
                player.seekTo(currentPosition)
            }
            player.start()
        }
    }

    fun updateDanmakuState(action: DanmakuSettingAction) {
        val old = _uiState.value.danmakuState
        val new = when (action) {
            is DanmakuSettingAction.SetScale -> old.copy(scale = action.value)
            is DanmakuSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is DanmakuSettingAction.SetArea -> old.copy(area = action.value)
            is DanmakuSettingAction.SetSpeedFactor -> old.copy(speedFactor = action.value)
            is DanmakuSettingAction.SetMaskEnabled -> old.copy(maskEnabled = action.enabled)
            is DanmakuSettingAction.SetEnabledTypes -> old.copy(enabledTypes = action.types)
        }

        if (old == new) return

        _uiState.update { it.copy(danmakuState = new) }

        // ===== 副作用处理 =====
        if (new.enabledTypes != old.enabledTypes) {
            updateDanmakuConfigTypeFilter(new.enabledTypes)
            Prefs.defaultDanmakuTypes = new.enabledTypes
        }
        if (new.scale != old.scale) {
            updateDanmakuScale(new.scale)
            Prefs.defaultDanmakuScale = new.scale
        }
        if (new.speedFactor != old.speedFactor) {
            updateDanmakuSpeedFactor(new.speedFactor)
            Prefs.defaultDanmakuSpeedFactor = new.speedFactor
        }
        if (new.area != old.area) {
            Prefs.defaultDanmakuArea = new.area
        }
        if (new.opacity != old.opacity) {
            Prefs.defaultDanmakuOpacity = new.opacity
        }
        if (new.maskEnabled != old.maskEnabled) {
            Prefs.defaultDanmakuMask = new.maskEnabled
        }
    }

    fun updateSubtitleState(action: SubtitleSettingAction) {
        val old = _uiState.value.subtitleState
        val new = when (action) {
            is SubtitleSettingAction.SetFontSize -> old.copy(fontSize = action.value)
            is SubtitleSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is SubtitleSettingAction.SetBottomPadding -> old.copy(bottomPadding = action.value)
        }

        if (old == new) return

        _uiState.update { it.copy(subtitleState = new) }

        // ===== 持久化副作用 =====
        if (new.fontSize != old.fontSize) {
            Prefs.defaultSubtitleFontSize = new.fontSize
        }

        if (new.opacity != old.opacity) {
            Prefs.defaultSubtitleBackgroundOpacity = new.opacity
        }

        if (new.bottomPadding != old.bottomPadding) {
            Prefs.defaultSubtitleBottomPadding = new.bottomPadding
        }
    }

    /**
     * 触发播放结束后的检查逻辑
     */
    fun checkAndPlayNext() {
        when (Prefs.actionAfterPlay) {
            ActionAfterPlayItems.Pause -> return
            ActionAfterPlayItems.Exit -> {
                viewModelScope.launch {
                    _uiEffect.emit(PlayerUiEffect.FinishActivity)
                }
                return
            }

            ActionAfterPlayItems.PlayNext -> {
                /* 继续执行 */
            }
        }

        val currentState = _uiState.value
        val videoList = currentState.availableVideoList
        val currentCid = currentState.cid

        // 查找当前视频在列表中的位置
        val videoListIndex = videoList.indexOfFirst { it.aid == currentState.aid }
        val currentVideoItem = videoList.getOrNull(videoListIndex)

        // 预计算下一个播放项 (NextTarget)
        var nextTarget: NextPlayTarget? = null

        // 检查是否有下一个分 P (UGC Page)
        if (currentVideoItem?.ugcPages?.isNotEmpty() == true) {
            val currentInnerIndex = currentVideoItem.ugcPages.indexOfFirst { it.cid == currentCid }
            if (currentInnerIndex != -1 && currentInnerIndex + 1 < currentVideoItem.ugcPages.size) {
                val nextPage = currentVideoItem.ugcPages[currentInnerIndex + 1]
                nextTarget = NextPlayTarget.UgcPage(currentVideoItem, nextPage)
            }
        }

        // 如果没有分 P，检查是否有下一个视频
        if (nextTarget == null && videoListIndex + 1 < videoList.size) {
            val nextVideo = videoList[videoListIndex + 1]
            nextTarget = NextPlayTarget.VideoItem(nextVideo)
        }

        // 根据查找结果执行操作
        if (nextTarget != null) {
            startNextEpisodeCountdown(nextTarget)
        } else {
            // 没有下一集了，发送事件关闭页面
            viewModelScope.launch {
                _uiEffect.emit(PlayerUiEffect.FinishActivity)
            }
        }
    }

    fun cancelPlayNext() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
    }

    fun backToStart() {
        backToStartCountdownJob?.cancel()
        _uiState.update { it.copy(showBackToStart = false) }

        videoPlayer?.seekTo(0)
        danmakuPlayer?.seekTo(0)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
    }

    /**
     * 开始周期性更新播放进度
     */
    fun startSeekerUpdater() {
        // 防止重复启动
        if (seekerUpdateJob?.isActive == true) return

        seekerUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateSeekerState()
                delay(100)
            }
        }
    }

    fun seekToTime(time: Long) {
        videoPlayer?.seekTo(time)
        _seekerState.update { it.copy(currentTime = time) }
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
    }

    fun playNewVideo(video: VideoListItem) {
        videoPlayer?.pause()

        // 重置弹幕
        releaseDanmakuPlayer()
        initDanmakuPlayer()

        // 切换视频时加载新detail
        if (video.aid != _uiState.value.aid) {
            viewModelScope.launch(Dispatchers.IO) {
                videoInfoRepository.loadVideoDetail(video.aid, Prefs.apiType)
            }
        }

        // 加载新播放url
        loadPlayUrl(
            avid = video.aid,
            cid = video.cid,
            epid = video.epid,
            seasonId = video.seasonId,
            title = video.title
        )
    }

    fun trySendHeartbeat() {
        if (Prefs.incognitoMode) return
        val player = videoPlayer ?: return

        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Main) {
            val currentTimeSeconds = (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
            val totalTimeSeconds = (player.duration.coerceAtLeast(0) / 1000).toInt()

            val reportTime = if (currentTimeSeconds >= totalTimeSeconds) -1 else currentTimeSeconds

            withContext(Dispatchers.IO) {
                uploadHistory(reportTime)
            }
        }
    }

    fun ensureUgcPagesLoaded(aid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            videoInfoRepository.ensureUgcPagesLoaded(aid = aid, preferApiType = Prefs.apiType)
        }
    }

    private fun computeAidsHash(list: List<VideoListItem>): Int {
        var hash = 1
        list.forEach { item ->
            hash = 31 * hash + item.aid.hashCode()
        }
        return hash
    }

    private fun restartUgcPagesPrefetchIfNeeded(videoList: List<VideoListItem>) {
        val state = _uiState.value

        // PGC 不加载子项
        if (state.epid != null) return

        val centerAid = state.aid
        if (centerAid <= 0L) return
        if (videoList.isEmpty()) return

        val aidsHash = computeAidsHash(videoList)
        val shouldRestart = centerAid != lastPrefetchCenterAid || aidsHash != lastPrefetchAidsHash

        // 如果已经在跑且 key 没变，就不重启（避免 ugcPages 填充导致的频繁 emit 重启）
        if (!shouldRestart && ugcPagesPrefetchJob?.isActive == true) return

        lastPrefetchCenterAid = centerAid
        lastPrefetchAidsHash = aidsHash

        ugcPagesPrefetchJob?.cancel()
        ugcPagesPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val centerIndex = videoList.indexOfFirst { it.aid == centerAid }
            if (centerIndex == -1) return@launch

            suspend fun prefetchItem(item: VideoListItem) {
                // PGC 父项跳过
                if (item.epid != null) return

                val delayMs = runCatching {
                    videoInfoRepository.ensureUgcPagesLoaded(aid = item.aid, preferApiType = Prefs.apiType)
                }.fold(
                    onSuccess = { ugcPagesPrefetchDelayMs },
                    onFailure = { throwable ->
                        if (throwable is CancellationException) throw throwable
                        logger.fWarn { "UGC pages prefetch failed: aid=${item.aid}, error=${throwable.stackTraceToString()}" }
                        ugcPagesPrefetchFailureDelayMs
                    }
                )
                delay(delayMs)
            }

            // 立刻确保当前父项已触发加载（repository 会去重）
            prefetchItem(videoList[centerIndex])

            // 以当前父项为中心，向上/向下交替，直到遍历完当前小合集所有父项
            val lastIndex = videoList.lastIndex
            val maxDelta = maxOf(centerIndex, lastIndex - centerIndex)
            for (delta in 1..maxDelta) {
                val upIndex = centerIndex - delta
                if (upIndex >= 0) {
                    prefetchItem(videoList[upIndex])
                }

                val downIndex = centerIndex + delta
                if (downIndex <= lastIndex) {
                    prefetchItem(videoList[downIndex])
                }
            }
        }
    }

    /*
    private fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        title: String,
    ) {
        _uiState.update {
            it.copy(
                aid = avid,
                cid = cid,
                epid = epid ?: 0,
                seasonId = seasonId ?: 0,
                title = title
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            loadPlayUrlImpl(
                avid,
                cid,
                epid ?: 0,
                preferApi = Prefs.apiType,
                proxyArea = _uiState.value.proxyArea
            )

            loadDanmaku(cid)
            updateDanmakuMask()
            updateSubtitle()
            updateVideoShot()
            updateVideoPages()
            clearVideoShotCache()
            */
    private fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
    ) {
        // 在 UI State 中保持 epid 的真实语义：
        // - UGC：null
        // - PGC：非 null
        // 同时兼容外部偶发传入 0 的情况（0 视作无 epid）
        val normalizedEpid = epid?.takeIf { it != 0 }

        _uiState.update {
            it.copy(
                aid = avid,
                cid = cid,
                epid = normalizedEpid,
                seasonId = seasonId ?: 0,
            )
        }

        // 切换父项时重启后台预取（切换中心）
        restartUgcPagesPrefetchIfNeeded(_uiState.value.availableVideoList)

        viewModelScope.launch(Dispatchers.Default) {
            loadPlayUrlImpl(
                avid,
                cid,
                normalizedEpid ?: 0,
                preferApi = Prefs.apiType,
                proxyArea = _uiState.value.proxyArea
            )

            loadDanmaku(cid)
            updateDanmakuMask()
            updateSubtitle()
            updateVideoShot()
            updateVideoPages()
            clearVideoShotCache()

            //如果是继续播放下一集，且之前开启了字幕，就会自动加载第一条字幕，主要用于观看番剧时自动加载字幕
            val lastPlayEnabledSubtitle = _uiState.value.subtitleId != -1L
            if (lastPlayEnabledSubtitle) {
                logger.info { "Subtitle is enabled, next video will enable subtitle automatic" }
                enableFirstSubtitle()
            }
        }
    }

    private suspend fun loadPlayUrlImpl(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        preferApi: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ) {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea]" }

        runCatching {
            val playData = if (_uiState.value.fromSeason) {
                videoPlayRepository.getPgcPlayData(
                    aid = avid,
                    cid = cid,
                    epid = epid,
                    preferCodec = Prefs.defaultVideoCodec.toBiliApiCodeType(),
                    preferApiType = Prefs.apiType,
                    enableProxy = Prefs.enableProxy,
                    proxyArea = when (proxyArea) {
                        ProxyArea.MainLand -> ""
                        ProxyArea.HongKong -> "hk"
                        ProxyArea.TaiWan -> "tw"
                    }
                )
            } else {
                videoPlayRepository.getPlayData(
                    aid = avid,
                    cid = cid,
                    preferApiType = Prefs.apiType
                )
            }

            //检查是否需要购买，如果未购买，则正片返回的dash为null，非正片例如可以免费观看的预告片等则会返回数据，此时不做提示
            _uiState.update { it.copy(needPay = playData.needPay) }
            if (playData.needPay) return@runCatching

            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.playData = playData }
            logger.fInfo { "Load play data response success" }
            logger.info { "Play data: $playData" }

            //读取清晰度
            val resolutionMap = mutableMapOf<Int, String>()
            playData.dashVideos.forEach {
                if (!resolutionMap.containsKey(it.quality)) {
                    val name = Resolution.fromCode(it.quality).getShortDisplayName(BVApp.context)
                    resolutionMap[it.quality] = name
                }
            }
            logger.fInfo { "Video available resolution: $resolutionMap" }
            _uiState.update { it.copy(availableQuality = resolutionMap) }

            //读取音频
            val audioList = mutableListOf<Audio>()
            playData.dashAudios.forEach {
                Audio.fromCode(it.codecId).let { audio ->
                    if (!audioList.contains(audio)) audioList.add(audio)
                }
            }
            playData.dolby?.let {
                Audio.fromCode(it.codecId).let { audio ->
                    audioList.add(audio)
                }
            }
            playData.flac?.let {
                Audio.fromCode(it.codecId).let { audio ->
                    audioList.add(audio)
                }
            }
            logger.fInfo { "Video available audio: $audioList" }
            _uiState.update { it.copy(availableAudio = audioList) }

            // 确认最终所选清晰度
            val defaultQualityCode = Prefs.defaultQuality.code
            val existDefaultResolution = resolutionMap.containsKey(defaultQualityCode)
            val targetQualityId = if (existDefaultResolution) {
                defaultQualityCode
            } else {
                val sortedQualities = resolutionMap.keys.sorted()
                sortedQualities.findLast { it <= defaultQualityCode }
                    ?: sortedQualities.firstOrNull()
                    ?: 0
            }
            _uiState.update {
                it.copy(
                    mediaProfileState = it.mediaProfileState.copy(qualityId = targetQualityId)
                )
            }

            // 确定最终目标音质
            val availableAudio = audioList
            Log.d("Available audio", "Available audio: $availableAudio")
            Log.d("Available audio", "Prefs.defaultAudio: ${Prefs.defaultAudio}")
            val targetAudio = if (availableAudio.contains(Prefs.defaultAudio)) {
                Prefs.defaultAudio
            } else {
                when {
                    Prefs.defaultAudio == Audio.ADolbyAtoms && availableAudio.contains(Audio.AHiRes) -> Audio.AHiRes
                    Prefs.defaultAudio == Audio.AHiRes && availableAudio.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
                    availableAudio.contains(Audio.A192K) -> Audio.A192K
                    availableAudio.contains(Audio.A132K) -> Audio.A132K
                    availableAudio.contains(Audio.A64K) -> Audio.A64K
                    else -> availableAudio.firstOrNull() ?: Audio.A132K
                }
            }
            _uiState.update {
                it.copy(mediaProfileState = it.mediaProfileState.copy(audio = targetAudio))
            }

            //确认最终所选视频编码
            val targetCodec = getTargetVideoCodec()

            // 开始播放对应profile的视频
            withContext(Dispatchers.Main) {
                playQuality(
                    qn = targetQualityId,
                    codec = targetCodec,
                    audio = targetAudio,
                )
                videoPlayer?.start()
            }
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(throwable.message ?: "Unknown error"),
                )
            }
            logger.fException(throwable) { "Load video failed" }
        }.onSuccess {
            logger.fInfo { "Load play url success" }
        }
    }

    // 加载合集内的分P
    /*
    private fun updateVideoPages() {
        viewModelScope.launch(Dispatchers.IO) {
            videoInfoRepository.updateUgcPages(Prefs.apiType)
        }
    }
     */
    // 加载当前视频的分P（仅 UGC，多P才会写入 ugcPages；番剧集不加载片段）
    private fun updateVideoPages() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentItem = state.availableVideoList.firstOrNull { it.aid == state.aid }
            // 番剧集（epid != null）不加载片段子项
            if (currentItem?.epid != null || state.epid != null) return@launch
            videoInfoRepository.ensureUgcPagesLoaded(aid = state.aid, preferApiType = Prefs.apiType)
        }
    }

    private fun getTargetVideoCodec(): VideoCodec {
        val state = _uiState.value

        if (Prefs.apiType == ApiType.App && playData!!.codec.isEmpty()) {
            // 纠正当前实际播放的编码
            val videoItem = playData!!.dashVideos
                .find { it.quality == state.mediaProfileState.qualityId }
                ?: playData!!.dashVideos.first()
            val codec = VideoCodec.fromCodecId(videoItem.codecId)
            _uiState.update {
                it.copy(
                    mediaProfileState = it.mediaProfileState.copy(
                        videoCodec = VideoCodec.fromCodecId(
                            videoItem.codecId
                        )
                    )
                )
            }
            return codec
        }

        val supportedCodec = playData!!.codec

        val codecList =
            supportedCodec[state.mediaProfileState.qualityId]!!.mapNotNull {
                VideoCodec.fromCodecString(
                    it
                )
            }

        val targetVideoCodec = if (codecList.contains(Prefs.defaultVideoCodec)) {
            Prefs.defaultVideoCodec
        } else {
            codecList.minByOrNull { it.ordinal }!!
        }
        _uiState.update {
            it.copy(
                availableVideoCodec = codecList,
                mediaProfileState = it.mediaProfileState.copy(videoCodec = targetVideoCodec)
            )
        }
        logger.fInfo { "Select codec: $targetVideoCodec" }
        return targetVideoCodec
    }

    private fun playQuality(
        qn: Int? = null,
        codec: VideoCodec? = null,
        audio: Audio? = null
    ) {
        val state = _uiState.value

        val targetQn = qn ?: state.mediaProfileState.qualityId
        val targetCodec = codec ?: state.mediaProfileState.videoCodec
        val targetAudio = audio ?: state.mediaProfileState.audio
        logger.fInfo {
            "Video quality：${state.availableQuality[targetQn]}, video encoding：${
                targetCodec.getDisplayName(
                    BVApp.context
                )
            }"
        }

        val foundVideoItem = playData!!.dashVideos.find {
            when (Prefs.apiType) {
                ApiType.Web -> it.quality == targetQn && it.codecs!!.startsWith(targetCodec.prefix)
                ApiType.App -> {
                    if (playData!!.codec.isEmpty()) it.quality == targetQn
                    else it.quality == targetQn && it.codecs!!.startsWith(targetCodec.prefix)
                }
            }
        }

        val actualVideoItem = foundVideoItem ?: playData!!.dashVideos.first()

        var videoUrl = actualVideoItem.baseUrl
        val videoUrls = mutableListOf<String?>()
        videoUrls.add(actualVideoItem.baseUrl)
        videoUrls.addAll(actualVideoItem.backUrl ?: emptyList())

        val audioItem = playData!!.dashAudios.find { it.codecId == targetAudio.code }
            ?: playData!!.dolby.takeIf { it?.codecId == targetAudio.code }
            ?: playData!!.flac.takeIf { it?.codecId == targetAudio.code }
            ?: playData!!.dashAudios.minByOrNull { it.codecId }
        var audioUrl = audioItem?.baseUrl ?: playData!!.dashAudios.first().baseUrl
        val audioUrls = mutableListOf<String?>()
        audioUrls.add(audioItem?.baseUrl)
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        logger.fInfo { "all video hosts: ${videoUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }
        logger.fInfo { "all audio hosts: ${audioUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }

        //replace cdn
        if (Prefs.enableProxy && state.proxyArea != ProxyArea.MainLand) {
            videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            audioUrl = audioUrl.replaceUrlDomainWithAliCdn()
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            videoUrl = selectOfficialCdnUrl(videoUrls.filterNotNull())
            audioUrl = selectOfficialCdnUrl(audioUrls.filterNotNull())
        }

        logger.fInfo {
            "Audio encoding：${
                (Audio.fromCode(audioItem?.codecId ?: 0)).getDisplayName(
                    BVApp.context
                ) ?: "未知"
            }"
        }


        logger.info { "Video url: $videoUrl" }
        logger.info { "Audio url: $audioUrl" }
        videoPlayer!!.playUrl(videoUrl, audioUrl)
        videoPlayer!!.prepare()

        _uiState.update {
            it.copy(
                videoHeight = actualVideoItem.height,
                videoWidth = actualVideoItem?.width ?: 0
            )
        }

    }

    private suspend fun loadDanmaku(cid: Long) {
        runCatching {
            val danmakuXmlData = BiliHttpApi.getDanmakuXml(cid = cid, sessData = Prefs.sessData)

            danmakuXmlData.data.map {
                DanmakuItemData(
                    danmakuId = it.dmid,
                    position = (it.time * 1000).toLong(),
                    content = it.text,
                    mode = when (it.type) {
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = it.size,
                    textColor = Color(it.color).toArgb()
                )
            }
        }.onSuccess { list ->
            danmakuPlayer?.updateData(list)
            logger.fInfo { "Load danmaku success, size: ${list.size}" }
        }.onFailure { error ->
            logger.fWarn { "Load danmaku failed: ${error.stackTraceToString()}" }
        }
    }

    private suspend fun updateSubtitle() {
        val state = _uiState.value

        runCatching {
            val subtitleData = videoPlayRepository.getSubtitle(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.apiType
            )
            _uiState.update { currentState ->
                val newSubtitles: List<Subtitle> = buildList {
                    add(
                        Subtitle(
                            id = -1,
                            lang = "",
                            langDoc = "关闭",
                            url = "",
                            type = SubtitleType.CC,
                            aiType = SubtitleAiType.Normal,
                            aiStatus = SubtitleAiStatus.None
                        )
                    )
                    addAll(subtitleData)
                    sortBy { it.id }
                }
                currentState.copy(
                    subtitleId = -1,
                    subtitleData = emptyList(),
                    availableSubtitles = newSubtitles
                )
            }

            logger.fInfo { "Update subtitle size: ${subtitleData.size}" }
        }.onFailure {
            logger.fWarn { "Update subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private fun enableFirstSubtitle() {
        runCatching {
            logger.info { "Load first subtitle" }
            logger.info { "availableSubtitle: ${_uiState.value.availableSubtitles.toList()}" }
            loadSubtitle(
                _uiState.value.availableSubtitles
                    .firstOrNull { it.id != -1L }?.id
                    ?: throw IllegalStateException("No available subtitle")
            )
        }.onFailure {
            logger.error { "Load first subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun uploadHistory(time: Int) {
        val state = _uiState.value

        try {
            with(state) {
                val currentApiType = Prefs.apiType

                if (!fromSeason) {
                    logger.info { "Send heartbeat: [avid=$aid, cid=$cid, time=$time]" }
                    videoPlayRepository.sendHeartbeat(
                        aid = aid,
                        cid = cid,
                        time = time,
                        preferApiType = currentApiType
                    )
                } else {
                    logger.info { "Send heartbeat: [avid=$aid, cid=$cid, epid=$epid, sid=$seasonId, time=$time]" }
                    videoPlayRepository.sendHeartbeat(
                        aid = aid,
                        cid = cid,
                        time = time,
                        type = HeartbeatVideoType.Season,
                        subType = subType,
                        epid = epid,
                        seasonId = seasonId,
                        preferApiType = currentApiType
                    )
                }
            }
            logger.info { "Send heartbeat success" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn { "Send heartbeat failed: ${e.stackTraceToString()}" }
        }
    }

    private suspend fun updateDanmakuMask() {
        val state = _uiState.value

        runCatching {
            val masks = videoPlayRepository.getDanmakuMask(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.apiType
            )

            _uiState.update { it.copy(danmakuMasks = masks) }

            logger.fInfo { "Load danmaku mask size: ${masks.size}" }

        }.onFailure {
            logger.fWarn { "Load danmaku mask failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoShot() {
        _uiState.update { it.copy(videoShot = null) }

        val state = _uiState.value
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = state.aid,
                cid = state.cid,
                preferApiType = Prefs.apiType
            )
            _uiState.update { it.copy(videoShot = videoShot) }
            logger.fInfo { "Load video shot success" }
        }.onFailure { err ->
            logger.fWarn { "Load video shot failed: ${err.stackTraceToString()}" }
        }
    }

    private fun clearVideoShotCache() {
        _uiState.value.videoShotCache.clear()
    }

    private fun initDanmakuConfig() {
        val danmakuTypes = Prefs.defaultDanmakuTypes
        val scale = Prefs.defaultDanmakuScale
        val factor = Prefs.defaultDanmakuSpeedFactor

        if (!danmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(danmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { typeFilter.addFilterItem(it) }
        }
        danmakuConfig = danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            textSizeScale = scale,
            dataFilter = listOf(typeFilter),
            rollingSpeedFactor = factor
        )
        danmakuConfig.updateFilter()
        logger.info { "Init danmaku config: $danmakuConfig" }
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuConfigTypeFilter(enabledDanmakuTypes: List<DanmakuType>) {
        typeFilter.clear()

        if (!enabledDanmakuTypes.contains(DanmakuType.All)) {
            val types = DanmakuType.entries.toMutableList()
            types.remove(DanmakuType.All)
            types.removeAll(enabledDanmakuTypes)
            val filterTypes = types.mapNotNull {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }
            filterTypes.forEach { typeFilter.addFilterItem(it) }
        }
        logger.info { "Update danmaku type filters: ${typeFilter.filterSet}" }
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuScale(scale: Float) {
        logger.info { "Update danmaku config: $danmakuConfig" }

        danmakuConfig = danmakuConfig.copy(
            textSizeScale = scale,
        )
        danmakuPlayer?.updateConfig(danmakuConfig)

        // 更新弹幕库之后updateConfig会导致滚动速度被重置，所以这里需要重新设置
        danmakuPlayer?.setDanmakuRollingSpeed(_uiState.value.danmakuState.speedFactor)
    }

    private fun updateDanmakuSpeedFactor(factor: Float) {
        logger.info { "Update danmaku rolling speed factor: $factor" }
        _uiState.update { it.copy(danmakuState = it.danmakuState.copy(speedFactor = factor)) }

        danmakuPlayer?.setDanmakuRollingSpeed(factor)
    }

    private fun startNextEpisodeCountdown(target: NextPlayTarget) {
        playNextCountdownJob?.cancel()

        playNextCountdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showSkipToNextEp = true,
                )
            }
            delay(5000)

            playNextTarget(target)
            _uiState.update { it.copy(showSkipToNextEp = false) }
        }
    }

    private fun playNextTarget(target: NextPlayTarget) {
        when (target) {
            is NextPlayTarget.UgcPage -> {
                logger.info { "Play next UGC page: ${target.page.title}" }
                playNewVideo(
                    VideoListItem(
                        aid = target.parentVideo.aid,
                        cid = target.page.cid,
                        title = target.title
                    )
                )
            }

            is NextPlayTarget.VideoItem -> {
                logger.info { "Play next video item: ${target.video.title}" }
                playNewVideo(
                    VideoListItem(
                        aid = target.video.aid,
                        cid = target.video.cid,
                        title = target.title,
                        epid = target.video.epid,
                        seasonId = target.video.seasonId,
                    )
                )
            }
        }
    }

    private fun seekToLastPlayed() {
        val time = _uiState.value.lastPlayed.toLong()
        logger.fInfo { "Back to history: ${time.formatHourMinSec()}" }

        videoPlayer?.seekTo(time)
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()

        _uiState.update { it.copy(showBackToStart = true) }

        backToStartCountdownJob?.cancel()
        backToStartCountdownJob = viewModelScope.launch {
            delay(5000)
            _uiState.update { it.copy(showBackToStart = false) }
        }
    }

    private fun stopSeekerUpdater() {
        seekerUpdateJob?.cancel()
        seekerUpdateJob = null
    }

    private fun updateSeekerState() {
        val player = videoPlayer ?: return

        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        _seekerState.update {
            it.copy(
                totalDuration = duration,
                currentTime = currentPos,
                bufferedPercentage = player.bufferedPercentage,
                debugInfo = player.debugInfo
            )
        }
    }

    private fun startClockUpdater() {
        clockUpdateJob?.cancel()
        clockUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateClock()
                delay(1000)
            }
        }
    }

    private fun updateClock() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        _uiState.update { it.copy(clock = Pair(hour, minute)) }
    }

    private fun selectOfficialCdnUrl(urls: List<String>): String {
        if (!Prefs.preferOfficialCdn) {
            logger.fInfo { "doesn't need to filter official cdn url, select the first url" }
            return urls.first()
        }
        val filteredUrls = urls
            .filter { !it.contains(".mcdn.bilivideo.") }
            .filter { !it.contains(".szbdyd.com") }
            .filter {
                !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/[a-zA-Z0-9_./-]*)?(\\?.*)?$")
                    .matches(it)
            }
        if (filteredUrls.isEmpty()) {
            logger.fInfo { "doesn't find any official cdn url, select the first url" }
            return urls.first()
        } else {
            logger.fInfo { "filtered official cdn urls: $filteredUrls" }
            return filteredUrls.first()
        }
    }

    private fun String.replaceUrlDomainWithAliCdn(): String {
        val replaceDomainKeywords = listOf(
            "mirroraliov",
            "mirrorakam"
        )
        if (replaceDomainKeywords.none { this.contains(it) }) return this

        return Uri.parse(this)
            .buildUpon()
            .authority("upos-sz-mirrorali.bilivideo.com")
            .build()
            .toString()
    }

    private sealed interface NextPlayTarget {
        val title: String

        data class UgcPage(val parentVideo: VideoListItem, val page: VideoPage) : NextPlayTarget {
            override val title: String = page.title
        }

        data class VideoItem(val video: VideoListItem) : NextPlayTarget {
            override val title: String = video.title
        }
    }

    override fun onCleared() {
        super.onCleared()

        // 最后一次心跳，此时viewModelScope已失效
        if (Prefs.incognitoMode) return
        val player = videoPlayer ?: return

        val currentTimeSeconds =
            (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
        val totalTimeSeconds =
            (player.duration.coerceAtLeast(0) / 1000).toInt()

        val reportTime =
            if (currentTimeSeconds >= totalTimeSeconds) -1 else currentTimeSeconds

        runBlocking {
            withContext(Dispatchers.IO) {
                uploadHistory(reportTime)
            }
        }
    }

}

sealed interface DanmakuSettingAction {
    data class SetScale(val value: Float) : DanmakuSettingAction
    data class SetOpacity(val value: Float) : DanmakuSettingAction
    data class SetArea(val value: Float) : DanmakuSettingAction
    data class SetSpeedFactor(val value: Float) : DanmakuSettingAction
    data class SetMaskEnabled(val enabled: Boolean) : DanmakuSettingAction
    data class SetEnabledTypes(val types: List<DanmakuType>) : DanmakuSettingAction
}

sealed interface SubtitleSettingAction {
    data class SetFontSize(val value: TextUnit) : SubtitleSettingAction
    data class SetOpacity(val value: Float) : SubtitleSettingAction
    data class SetBottomPadding(val value: Dp) : SubtitleSettingAction
}

sealed interface MediaProfileSettingAction {
    data class SetQuality(val value: Int) : MediaProfileSettingAction
    data class SetVideoCodec(val value: VideoCodec) : MediaProfileSettingAction
    data class SetAudio(val value: Audio) : MediaProfileSettingAction
}