package dev.aaa1115910.bv.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.http.entity.video.ClipInfo
import dev.aaa1115910.biliapi.entity.video.HeartbeatVideoType
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.SubtitleAiStatus
import dev.aaa1115910.biliapi.entity.video.SubtitleAiType
import dev.aaa1115910.biliapi.entity.video.SubtitleType
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import dev.aaa1115910.biliapi.http.entity.live.LiveEvent
import dev.aaa1115910.biliapi.http.entity.live.OnlineRankCountEvent
import dev.aaa1115910.biliapi.http.entity.live.PopularityChangeEvent
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.biliapi.websocket.LiveDataWebSocket
import dev.aaa1115910.bilisubtitle.SubtitleParser
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.renderer.OptimizedTextRenderer
import dev.aaa1115910.bv.player.renderer.SimpleRenderer
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.DefaultSubtitle
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.RequestState
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoRotation
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.LiveStreamUrlFetcher
import dev.aaa1115910.bv.util.fDebug
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.swapListWithMainContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.net.URI

@KoinViewModel
class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
) : ViewModel() {
    private val logger = KotlinLogging.logger { }

    var videoPlayer: AbstractVideoPlayer? by mutableStateOf(null)
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
    var show by mutableStateOf(false)
    
    override fun onCleared() {
        super.onCleared()
        logger.fInfo { "VideoPlayerV3ViewModel onCleared" }
        
        // 清理直播重连任务
        liveRetryJob?.cancel()
        liveRetryJob = null
        
        // 清理直播弹幕资源
        stopLiveDanmaku()
        
        try {
            videoPlayer?.release()
            videoPlayer = null
        } catch (e: Exception) {
            logger.fError { "Error releasing video player: ${e.message}" }
        }

        try {
            danmakuPlayer?.release()
            danmakuPlayer = null
            danmakuData.clear()
            danmakuMasks.clear()
        } catch (e: Exception) {
            logger.fError { "Error releasing danmaku player: ${e.message}" }
        }

        // 清除可能未被GC回收的资源
        currentSubtitleData.clear()
    }

    var loadState by mutableStateOf(RequestState.Ready)
    var errorMessage by mutableStateOf("")

    private var playData: PlayData? by mutableStateOf(null)
    val danmakuData: MutableList<DanmakuItemData> = ArrayList()
    val danmakuMasks = mutableStateListOf<DanmakuMaskSegment>()
    var videoShot: VideoShot? by mutableStateOf(null)
    var clipInfoList: List<ClipInfo> by mutableStateOf(emptyList())

    var availableQuality = mutableStateListOf<Resolution>()
    var availableVideoCodec = mutableStateListOf<VideoCodec>()
    var availableSubtitle = mutableStateListOf<Subtitle>()
    var availableAudio = mutableStateListOf<Audio>()
    val availableVideoList get() = videoInfoRepository.videoList
    val relatedVideos get() =  videoInfoRepository.relatedVideos

    var currentVideoHeight by mutableIntStateOf(0)
    var currentVideoWidth by mutableIntStateOf(0)

    var currentQuality by mutableStateOf(Prefs.defaultQuality)
    var currentVideoCodec by mutableStateOf(Prefs.defaultVideoCodec)
    var currentPlaySpeed by mutableFloatStateOf(Prefs.currentPlaySpeed)
    var currentVideoAspectRatio by mutableStateOf(VideoAspectRatio.Default)
    var currentVideoRotation by mutableStateOf(VideoRotation.Original)
    var currentAudio by mutableStateOf(Prefs.defaultAudio)
    var currentDanmakuScale by mutableFloatStateOf(Prefs.defaultDanmakuScale)
    var currentDanmakuOpacity by mutableFloatStateOf(Prefs.defaultDanmakuOpacity)
    var currentDanmakuEnabled by mutableStateOf(Prefs.defaultDanmakuEnabled)
    val currentDanmakuTypes = mutableStateListOf<DanmakuType>().apply {
        addAll(Prefs.defaultDanmakuTypes)
    }
    var currentDanmakuArea by mutableFloatStateOf(Prefs.defaultDanmakuArea)
    var currentDanmakuMask by mutableStateOf(Prefs.defaultDanmakuMask)
    var currentDanmakuRollingDurationFactor by mutableFloatStateOf(Prefs.defaultDanmakuRollingDurationFactor)
    var currentSubtitleId by mutableLongStateOf(-1L)
    var currentSubtitleData = mutableStateListOf<SubtitleItem>()
    var currentSubtitleType by mutableStateOf(SubtitleType.CC)
    var currentSubtitleFontSize by mutableStateOf(Prefs.defaultSubtitleFontSize)
    var currentSubtitleBackgroundOpacity by mutableFloatStateOf(Prefs.defaultSubtitleBackgroundOpacity)
    var currentSubtitleBottomPadding by mutableStateOf(Prefs.defaultSubtitleBottomPadding)

    var currentPlayMode by mutableStateOf(Prefs.defaultPlayMode)

    var title by mutableStateOf("")
    var partTitle by mutableStateOf("")
    var lastPlayed by mutableIntStateOf(0)
    var fromSeason by mutableStateOf(false)
    var subType by mutableIntStateOf(0)
    var epid by mutableIntStateOf(0)
    var seasonId by mutableIntStateOf(0)
    var isVerticalVideo by mutableStateOf(false)
    var proxyArea by mutableStateOf(ProxyArea.MainLand)
    var play by mutableLongStateOf(0)
    var danmaku by mutableStateOf(0)
    var like by mutableStateOf(0)
    
    // 直播相关属性
    var isLive by mutableStateOf(false)
    var liveRoomId by mutableIntStateOf(0)
    var liveStreamUrl by mutableStateOf("")

    // 直播画质管理
    var availableLiveQualities = mutableStateListOf<Pair<Int, String>>() // qn -> description
    var currentLiveQn by mutableIntStateOf(0)
    var currentLiveQualityDescription by mutableStateOf("")
    private var liveQnDescMap: Map<Int, String> = emptyMap()
    
    // 直播自动重连
    private var liveRetryJob: Job? = null

    // 直播人气值与高能观众
    var livePopularityText by mutableStateOf("")   // "2.5万人气" (POPULARITY_CHANGE)
    var liveOnlineCount by mutableStateOf("")      // "4333 高能观众" (ONLINE_RANK_COUNT)

    // 人气和高能观众更新频率限制（至少间隔 10 秒）
    private var lastPopularityUpdateTime = 0L
    private var lastOnlineCountUpdateTime = 0L

    // 直播弹幕管理
    private var liveWebSocket: Job? = null
    private var liveWebSocketInner: Job? = null
    private var liveDanmakuConsumer: Job? = null
    private var liveDanmakuChannel: Channel<DanmakuEvent>? = null
    private val liveDanmakuBuffer = mutableListOf<DanmakuItemData>()
    private var liveDanmakuFlushJob: Job? = null
    
    var coin by mutableStateOf(0)
    var favorite by mutableStateOf(0)
    var upName by mutableStateOf("")
    var upFace by mutableStateOf("")
    var pubTime by mutableStateOf("")
    var upId by mutableLongStateOf(0L)
    var isLoop by mutableStateOf(Prefs.isLoop)
    var showDanmaku by mutableStateOf(Prefs.showDanmaku)
    var showRelatedVideos by mutableStateOf(false)
    var isFollowingUp by mutableStateOf(false)

    var needPay by mutableStateOf(false)

    var logs by mutableStateOf("")
    var lastChangedLog by mutableLongStateOf(System.currentTimeMillis())
    var showBuffering by mutableStateOf(false)

    var playerIconIdle by mutableStateOf("")
    var playerIconMoving by mutableStateOf("")

    var currentAid = 0L
    var currentCid by mutableLongStateOf(0L)
    private var currentEpid = 0

    private suspend fun ensureDanmakuPlayer() = withContext(Dispatchers.Main) {
        danmakuPlayer?.release()
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        logger.fInfo { "(Re)create DanmakuPlayer: $danmakuPlayer" }
    }

    fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int? = null,
        seasonId: Int? = null,
        continuePlayNext: Boolean = false
    ) {
        currentAid = avid
        currentCid = cid
        currentEpid = epid ?: 0
        epid?.let { this.epid = it }
        seasonId?.let { this.seasonId = it }
        viewModelScope.launch(Dispatchers.Default) {
            addLogs("加载视频中")
            ensureDanmakuPlayer()
            addLogs("弹幕引擎已就绪")
            if (epid != null || seasonId != null) {
                addLogs("av$avid，cid:$cid, epid:$epid, seasonId:$seasonId")
            } else {
                addLogs("av$avid，cid:$cid")
            }

            val lastPlayEnabledSubtitle = currentSubtitleId != -1L
            val lastSubtitleLang = availableSubtitle.find { it.id == currentSubtitleId }?.langDoc
            if (lastPlayEnabledSubtitle) {
                logger.info { "Subtitle is enabled, next video will enable subtitle automatic" }
            }

            updateSubtitle()
            loadPlayUrl(avid, cid, epid ?: 0, preferApi = Prefs.apiType, proxyArea = proxyArea)
            // addLogs("加载弹幕中")
            loadDanmaku(cid)
            updateDanmakuMask()

            updateVideoShot()

            //如果是继续播放下一集，且之前开启了字幕，就自动加载之前选择的语言的字幕
            //否则根据默认字幕设置自动加载
            if (continuePlayNext && lastPlayEnabledSubtitle) {
                autoLoadSubtitle(preferLang = lastSubtitleLang, ccOnly = false)
            } else if (!continuePlayNext) {
                autoLoadSubtitle(preferLang = null, ccOnly = true)
            }
        }
    }

    private suspend fun loadPlayUrl(
        avid: Long,
        cid: Long,
        epid: Int = 0,
        preferApi: ApiType = Prefs.apiType,
        proxyArea: ProxyArea = ProxyArea.MainLand
    ) {
        logger.fInfo { "Load play url: [av=$avid, cid=$cid, preferApi=$preferApi, proxyArea=$proxyArea]" }
        withContext(Dispatchers.Main) { loadState = RequestState.Ready }
        logger.fInfo { "Set request state: ready" }
        logger.fInfo { "fromSeason: $fromSeason" }
        runCatching {
            val playData = if (fromSeason) {
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
            withContext(Dispatchers.Main) { needPay = playData.needPay }
            if (needPay) return@runCatching

            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.playData = playData }
            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.clipInfoList = playData.clipInfoList }
            logger.fInfo { "Load play data response success" }
            //logger.info { "Play data: $playData" }

            //读取清晰度
            val resolutionList = mutableListOf<Resolution>()
            playData.dashVideos.forEach {
                Resolution.fromCode(it.quality)?.let { resolution ->
                    if (!resolutionList.contains(resolution)) resolutionList.add(resolution)
                }
            }

            logger.fInfo { "Video available resolution: $resolutionList" }
            availableQuality.swapListWithMainContext(resolutionList)

            //读取音频
            val audioList = mutableListOf<Audio>()
            playData.dashAudios.forEach {
                Audio.fromCode(it.codecId)?.let { audio ->
                    if (!audioList.contains(audio)) audioList.add(audio)
                }
            }
            playData.dolby?.let {
                Audio.fromCode(it.codecId)?.let { audio ->
                    audioList.add(audio)
                }
            }
            playData.flac?.let {
                Audio.fromCode(it.codecId)?.let { audio ->
                    audioList.add(audio)
                }
            }

            logger.fInfo { "Video available audio: $audioList" }
            availableAudio.swapListWithMainContext(audioList)

            // 确定使用哪个默认分辨率
            val defaultQualityToUse = if (
                isVerticalVideo &&
                Prefs.portraitVideoFixMode == PortraitVideoFixMode.LimitResolution1080P &&
                Prefs.defaultQuality >= Resolution.R4K
            ) {
                // 如果是竖屏视频且用户设置了竖屏视频限制最高使用1080P
                Resolution.R1080P60
            } else {
                // 否则使用普通设置
                Prefs.defaultQuality
            }

            //先确认最终所选清晰度
            val existDefaultResolution =
                availableQuality.find { it == defaultQualityToUse } != null

            if (!existDefaultResolution) {
                val tempList = resolutionList.sortedByDescending { it.code }
                val currentQuality = tempList.firstOrNull { it.code < defaultQualityToUse.code }
                    ?: tempList.last()
                withContext(Dispatchers.Main) {
                    this@VideoPlayerV3ViewModel.currentQuality = currentQuality
                }
            } else {
                // 如果默认清晰度可用，直接使用
                withContext(Dispatchers.Main) { currentQuality = defaultQualityToUse }
            }

            //确认最终所选音质
            val existDefaultAudio = availableAudio.contains(Prefs.defaultAudio)
            if (!existDefaultAudio && availableAudio.isNotEmpty()) {
                val currentAudio = when {
                    Prefs.defaultAudio == Audio.ADolbyAtoms && availableAudio.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
                    (Prefs.defaultAudio == Audio.ADolbyAtoms || Prefs.defaultAudio == Audio.AHiRes) && availableAudio.contains(Audio.AHiRes) -> Audio.AHiRes
                    availableAudio.contains(Audio.A192K) -> Audio.A192K
                    availableAudio.contains(Audio.A132K) -> Audio.A132K
                    availableAudio.contains(Audio.A64K) -> Audio.A64K
                    else -> availableAudio.first()
                }
                withContext(Dispatchers.Main) {
                    this@VideoPlayerV3ViewModel.currentAudio = currentAudio
                }
            }

            //再确认最终所选视频编码
            updateAvailableCodec()

            playQuality(qn = currentQuality.code, codec = currentVideoCodec)

        }.onFailure {
            addLogs("加载视频地址失败：${it.localizedMessage}")
            errorMessage = it.localizedMessage ?: "Unknown error"
            loadState = RequestState.Failed
            logger.fException(it) { "Load video failed" }
        }.onSuccess {
            addLogs("加载视频地址成功")
            loadState = RequestState.Success
            logger.fInfo { "Load play url success" }
        }
    }

    private suspend fun updateAvailableCodec() {
        if (Prefs.apiType == ApiType.App && playData!!.codec.isEmpty()) {
            // 纠正当前实际播放的编码
            val videoItem = playData!!.dashVideos
                .find { it.quality == currentQuality.code }
                ?: playData!!.dashVideos.first()
            withContext(Dispatchers.Main) {
                currentVideoCodec = VideoCodec.fromCodecId(videoItem.codecId)
            }
            logger.fInfo { "App API fixed, Select codec: $currentVideoCodec" }
            return
        }

        val supportedCodec = playData!!.codec
        val codecList =
            supportedCodec[currentQuality.code]?.mapNotNull { VideoCodec.fromCodecString(it) } ?: emptyList()

        availableVideoCodec.swapListWithMainContext(codecList)
        logger.fInfo { "Video available codec: ${availableVideoCodec.toList()}" }

        logger.fInfo { "Default codec: $currentVideoCodec" }
        val currentVideoCodec = if (codecList.contains(Prefs.defaultVideoCodec)) {
            Prefs.defaultVideoCodec
        } else {
            codecList.minByOrNull { it.ordinal }!!
        }
        withContext(Dispatchers.Main) {
            this@VideoPlayerV3ViewModel.currentVideoCodec = currentVideoCodec
        }
        logger.fInfo { "Select codec: $currentVideoCodec" }
    }

    suspend fun playQuality(
        qn: Resolution = currentQuality,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio
    ) {
        if (qn != currentQuality) {
            // 更新清晰度后需要先设置清晰度再更新编码列表
            withContext(Dispatchers.Main) { currentQuality = qn }
            updateAvailableCodec()
            playQuality(qn.code, currentVideoCodec, audio)
        } else {
            playQuality(qn.code, codec, audio)
        }
    }

    private suspend fun playQuality(
        qn: Int = currentQuality.code,
        codec: VideoCodec = currentVideoCodec,
        audio: Audio = currentAudio
    ) {
        logger.fInfo { "Select resolution: $qn, codec: $codec, audio: $audio" }
        if(playData == null) {
            return
        }

        val videoItem = playData!!.dashVideos.find {
            when (Prefs.apiType) {
                ApiType.Web -> it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                ApiType.App -> {
                    if (playData!!.codec.isEmpty()) it.quality == qn
                    else it.quality == qn && it.codecs!!.startsWith(codec.prefix)
                }
            }
        }
        var videoUrl = videoItem?.baseUrl ?: playData!!.dashVideos.firstOrNull()?.baseUrl
        if (videoUrl == null) {
            logger.fError { "Failed to get video URL" }
            errorMessage = "获取视频地址失败"
            loadState = RequestState.Failed
            return
        }
        val videoUrls = mutableListOf<String?>()
        videoUrls.add(videoItem?.baseUrl)
        videoUrls.addAll(videoItem?.backUrl ?: emptyList())

        val audioItem = listOfNotNull(
            playData!!.dashAudios.find { it.codecId == audio.code },
            playData!!.dolby.takeIf { it?.codecId == audio.code },
            playData!!.flac.takeIf { it?.codecId == audio.code },
            playData!!.dashAudios.minByOrNull { it.codecId },
            playData!!.dolby,
            playData!!.flac
        ).firstOrNull()
        var audioUrl = audioItem?.baseUrl
        val audioUrls = mutableListOf<String>()
        audioItem?.baseUrl?.let(audioUrls::add)
        audioUrls.addAll(audioItem?.backUrl ?: emptyList())

        logger.fInfo { "all video hosts: ${videoUrls.filterNotNull().map { with(URI(it)) { "$scheme://$authority" } }}" }
        logger.fInfo { "all audio hosts: ${audioUrls.map { with(URI(it)) { "$scheme://$authority" } }}" }

        //replace cdn
        if (Prefs.enableProxy && proxyArea != ProxyArea.MainLand) {
            videoUrl = videoUrl.replaceUrlDomainWithAliCdn()
            audioUrl = audioUrl?.replaceUrlDomainWithAliCdn()
        } else {
            // 如果未通过网络代理获得播放地址，才判断是否应该替换为官方 cdn
            videoUrl = selectOfficialCdnUrl(videoUrls.filterNotNull())
            audioUrl = audioUrls.takeIf { it.isNotEmpty() }?.let(::selectOfficialCdnUrl)
        }

        if (audioUrl == null) {
            logger.fWarn { "Failed to get audio URL, fallback to video-only playback" }
        }

        addLogs(
            "播放清晰度：${availableQuality.firstOrNull { it.code == qn }}, " +
                    "视频编码：${codec.getDisplayName(BVApp.context)}, " +
                    "音频编码：${(Audio.fromCode(audioItem?.codecId ?: 0))?.getDisplayName(BVApp.context) ?: "未知"}"
        )
        addLogs("video host: ${with(URI(videoUrl)) { "$scheme://$authority" }}")
        addLogs(
            audioUrl?.let { "audio host: ${with(URI(it)) { "$scheme://$authority" }}" }
                ?: "audio host: 无音频流，使用纯视频播放"
        )

        logger.fInfo { "Select audio: $audioItem" }

        withContext(Dispatchers.Main) {
            currentVideoHeight = videoItem?.height ?: 0
            currentVideoWidth = videoItem?.width ?: 0
            logger.info { "Video url: $videoUrl" }
            logger.info { "Audio url: $audioUrl" }
            videoPlayer!!.playUrl(videoUrl, audioUrl)
            // 根据 DefaultStartPosition 设置初始跳转位置，避免在 onReady 中 seekTo 导致的状态抖动
            if (lastPlayed > 0 && Prefs.playerDefaultStartPosition == PlayerDefaultStartPosition.History) {
                logger.info { "Set initial seek position to history: ${lastPlayed}ms" }
                videoPlayer!!.setInitialSeekPosition(lastPlayed.toLong())
            }
            videoPlayer!!.prepare()
            showBuffering = true
        }
    }

    suspend fun loadDanmaku(cid: Long) {
        runCatching {
            val danmakuXmlData = BiliHttpApi.getDanmakuXml(cid = cid, sessData = Prefs.sessData)
            val total = danmakuXmlData.data.size
            val batchSize = 600 // 分批大小，可根据设备性能调节
            withContext(Dispatchers.Main) {
                danmakuData.clear()
            }
            danmakuXmlData.data.asSequence()
                .chunked(batchSize) // 按批次切分原始数据
                .forEachIndexed { index, rawBatch ->
                    val convertedBatch = rawBatch.map {
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
                    danmakuData.addAll(convertedBatch)
                    // 让出调度，避免长时间占用 IO/CPU
                    withContext(Dispatchers.IO) { kotlinx.coroutines.delay(16) }
                }
            danmakuPlayer?.updateData(danmakuData.sortedBy { it.position })
        }.onFailure {
            addLogs("加载弹幕失败：${it.localizedMessage}")
            logger.fWarn { "Load danmaku filed: ${it.stackTraceToString()}" }
        }.onSuccess {
            addLogs("已加载 ${danmakuData.size} 条弹幕")
            logger.fInfo { "Load danmaku success, size=${danmakuData.size}" }
        }
    }

    private suspend fun updateSubtitle() {
        currentSubtitleId = -1
        currentSubtitleData.clear()

        runCatching {
            val subtitleData = videoPlayRepository.getSubtitle(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) {
                availableSubtitle.clear()
                availableSubtitle.add(
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
                availableSubtitle.addAll(subtitleData)
                availableSubtitle.sortBy { it.id }
            }
            addLogs("获取到 ${subtitleData.size} 条字幕: ${subtitleData.map { it.langDoc }}")
            logger.fInfo { "Update subtitle size: ${subtitleData.size}" }
        }.onFailure {
            addLogs("获取字幕失败：${it.localizedMessage}")
            logger.fWarn { "Update subtitle failed: ${it.stackTraceToString()}" }
        }
    }

    /**
     * 自动加载字幕
     *
     * @param preferLang 优先匹配的语言名称（来自播放器中手动选择的字幕），优先级高于默认字幕设置
     * @param ccOnly 是否仅匹配非AI字幕（type == CC）
     */
    private fun autoLoadSubtitle(preferLang: String?, ccOnly: Boolean) {
        val defaultSubtitle = Prefs.defaultSubtitle
        if (preferLang == null && defaultSubtitle == DefaultSubtitle.Off) return

        val langKeyword = preferLang ?: when (defaultSubtitle) {
            DefaultSubtitle.Chinese -> "中文"
            DefaultSubtitle.English -> "English"
            DefaultSubtitle.Off -> return
        }

        val candidates = availableSubtitle.filter { it.id != -1L && (!ccOnly || it.type == SubtitleType.CC) }
        val match = candidates.firstOrNull { it.langDoc.contains(langKeyword, ignoreCase = true) }

        if (match != null) {
            logger.fInfo { "Auto load subtitle: ${match.langDoc}" }
            loadSubtitle(match.id)
        } else {
            logger.fInfo { "No matching subtitle for lang=\"$langKeyword\", ccOnly=$ccOnly" }
        }
    }

    private suspend fun addLogs(text: String) {
        logger.fInfo { text }
        if (!Prefs.playerShowDebugInfo) {
            return
        }
        val lines = logs.lines().toMutableList()
        lines.add(text)
        while (lines.size > 8) {
            lines.removeAt(0)
        }
        var newTip = ""
        lines.forEach {
            newTip += if (newTip == "") it else "\n$it"
        }
        withContext(Dispatchers.Main) {
            logs = newTip
            lastChangedLog = System.currentTimeMillis()
        }
    }

    suspend fun uploadHistory(time: Int) {
        runCatching {
            if (!fromSeason) {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    preferApiType = Prefs.apiType
                )
            } else {
                logger.info { "Send heartbeat: [avid=$currentAid, cid=$currentCid, epid=$epid, sid=$seasonId, time=$time]" }
                videoPlayRepository.sendHeartbeat(
                    aid = currentAid,
                    cid = currentCid,
                    time = time,
                    type = HeartbeatVideoType.Season,
                    subType = subType,
                    epid = epid,
                    seasonId = seasonId,
                    preferApiType = Prefs.apiType
                )
            }
        }.onSuccess {
            logger.info { "Send heartbeat success" }
        }.onFailure {
            logger.warn { "Send heartbeat failed: ${it.stackTraceToString()}" }
        }
    }

    fun loadSubtitle(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == -1L) {
                withContext(Dispatchers.Main) {
                    currentSubtitleData.clear()
                    currentSubtitleId = -1
                    currentSubtitleType = SubtitleType.CC
                }
                return@launch
            }
            var subtitleName = ""
            runCatching {
                val subtitle = availableSubtitle.find { it.id == id } ?: return@runCatching
                subtitleName = subtitle.langDoc
                val isAI = subtitle.type == SubtitleType.AI
                logger.info { "Subtitle url: ${subtitle.url}, isAI: $isAI" }
                val client = HttpClient(OkHttp)
                val responseText = client.get(subtitle.url).bodyAsText()
                val subtitleData = SubtitleParser.fromBccString(responseText, isAI)
                withContext(Dispatchers.Main) {
                    currentSubtitleId = id
                    currentSubtitleType = subtitle.type
                    currentSubtitleData.swapList(subtitleData)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    currentSubtitleData.clear()
                    currentSubtitleId = -1
                    currentSubtitleType = SubtitleType.CC
                }
                logger.fInfo { "Load subtitle failed: ${it.stackTraceToString()}" }
                addLogs("加载字幕 $subtitleName 失败: ${it.localizedMessage}")
            }.onSuccess {
                logger.fInfo { "Load subtitle $subtitleName success" }
                addLogs("加载字幕 $subtitleName 成功，数量: ${currentSubtitleData.size}")
            }
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

    private fun selectOfficialCdnUrl(urls: List<String>): String {
        if (urls.isEmpty()) {
            logger.fInfo { "doesn't find any url, select a random url" }
            return urls.randomOrNull() ?: ""
        }

        // 判定是否为“官方” CDN 的简单规则，和之前逻辑保持一致
        val isOfficialCdn: (String) -> Boolean = {
            !it.contains(".mcdn.bilivideo.") &&
            !it.contains(".szbdyd.com") &&
            !Regex("^(https?://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})?)(/[a-zA-Z0-9_./-]*)?(\\?.*)?$")
                .matches(it)
        }

        if (!Prefs.preferOfficialCdn) {
            // 当用户不偏好官方 CDN 时，使用加权随机：官方权重 0.8，非官方权重 1.2（基准为 1）
            logger.fInfo { "doesn't need to filter official cdn url, select a weighted random url (favor non-official)" }

            val weights = urls.map { url -> if (isOfficialCdn(url)) 1 else 1 }
            val total = weights.sum()
            // 如果权重计算异常，退回随机
            if (total <= 0.0) return urls.randomOrNull() ?: ""

            val r = kotlin.random.Random.Default.nextDouble() * total
            var acc = 0.0
            for (i in urls.indices) {
                acc += weights[i]
                if (r <= acc) return urls[i]
            }
            return urls.randomOrNull() ?: ""
        }

        val filteredUrls = urls.filter{isOfficialCdn(it)}
        if (filteredUrls.isEmpty()) {
            logger.fInfo { "doesn't find any official cdn url, select a random url" }
            return urls.random()
        } else {
            logger.fInfo { "filtered official cdn urls: $filteredUrls" }
            return filteredUrls.random()
        }
    }

    private suspend fun updateDanmakuMask() {
        runCatching {
            val masks = videoPlayRepository.getDanmakuMask(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            danmakuMasks.swapListWithMainContext(masks)
            logger.fInfo { "Load danmaku mask size: ${danmakuMasks.size}" }
        }.onFailure {
            logger.fWarn { "Load danmaku mask failed: ${it.stackTraceToString()}" }
        }
    }

    private suspend fun updateVideoShot() {
        withContext(Dispatchers.Main) { videoShot = null }
        runCatching {
            val videoShot = videoPlayRepository.getVideoShot(
                aid = currentAid,
                cid = currentCid,
                preferApiType = Prefs.apiType
            )
            withContext(Dispatchers.Main) { this@VideoPlayerV3ViewModel.videoShot = videoShot }
            logger.fInfo { "Load video shot success" }
        }.onFailure {
            logger.fWarn { "Load video shot failed: ${it.stackTraceToString()}" }
        }
    }

    fun playNextVideo() {
        logger.fInfo { "Video finished" }
        when (currentPlayMode) {
            PlayMode.Single -> {
                logger.info { "Play mode: $currentPlayMode, do nothing" }
            }

            PlayMode.Sequential -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list" }
                playNextVideoInList()
            }

            PlayMode.SingleLoop -> {
                logger.info { "Play mode: $currentPlayMode, replay current video" }
                danmakuPlayer?.seekTo(0L)
                danmakuPlayer?.pause()
                videoPlayer?.seekTo(0L)
            }

            PlayMode.ListLoop -> {
                logger.info { "Play mode: $currentPlayMode, play next video in list or loop to first" }
                playNextVideoInList(loop = true)
            }
        }
    }

    private fun playNextVideoInList(loop: Boolean = false) {
        val currentIndex = availableVideoList
            .indexOfFirst {
                when (it) {
                    is VideoListItemData -> it.cid == currentCid
                    else -> false
                }
            }
        if (currentIndex + 1 < availableVideoList.size) {
            val nextVideos = availableVideoList.subList(
                currentIndex + 1,
                availableVideoList.size
            )
            val nextVideo =
                nextVideos.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Play next video: $nextVideo" }
            partTitle = nextVideo.title
            loadPlayUrl(
                avid = nextVideo.aid,
                cid = nextVideo.cid!!,
                epid = nextVideo.epid,
                seasonId = nextVideo.seasonId,
                continuePlayNext = true
            )
        } else if (loop) {
            //loop to first
            val firstVideo =
                availableVideoList.firstOrNull { it is VideoListItemData }!! as VideoListItemData
            logger.info { "Loop to first video: $firstVideo" }
            partTitle = firstVideo.title
            loadPlayUrl(
                avid = firstVideo.aid,
                cid = firstVideo.cid!!,
                epid = firstVideo.epid,
                seasonId = firstVideo.seasonId,
                continuePlayNext = true
            )
        }
    }
    
    /**
     * 加载直播流（带画质信息）
     * @param roomId 直播间ID
     * @param qn 请求的画质编号，默认30000（最高值，服务端会自动降级）
     */
    fun loadLiveStreamWithQuality(roomId: Int, qn: Int = 30000) {
        // 取消之前的重连任务
        liveRetryJob?.cancel()
        liveRetryJob = null

        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "Load live stream with quality: roomId=$roomId, qn=$qn" }
            withContext(Dispatchers.Main) { loadState = RequestState.Doing }

            // 仅在首次加载时初始化弹幕播放器，画质切换时不重复创建
            if (danmakuPlayer == null) {
                ensureDanmakuPlayer()
            }

            val playInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(roomId, qn)
            if (playInfo == null) {
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "获取直播流失败"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                liveStreamUrl = playInfo.streamUrl
                currentLiveQn = playInfo.currentQn
                liveQnDescMap = playInfo.qnDescMap

                // 更新可用画质列表（按 qn 降序，即最高画质在前）
                val qualities = playInfo.acceptQn
                    .sortedDescending()
                    .map { qualityQn ->
                        qualityQn to (playInfo.qnDescMap[qualityQn] ?: "未知画质 $qualityQn")
                    }
                availableLiveQualities.clear()
                availableLiveQualities.addAll(qualities)

                currentLiveQualityDescription = playInfo.qnDescMap[playInfo.currentQn] ?: "未知画质"
                logger.fInfo { "Live quality: current=${playInfo.currentQn} ($currentLiveQualityDescription), available=$qualities" }
            }

            runCatching {
                withContext(Dispatchers.Main) {
                    videoPlayer?.playUrl(videoUrl = playInfo.streamUrl)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                    loadState = RequestState.Success
                }
                logger.fInfo { "Live stream loaded successfully with quality ${playInfo.currentQn}" }
                // 播放成功后自动启动直播弹幕（仅首次加载，画质切换时不重启弹幕）
                if (liveWebSocket == null) {
                    startLiveDanmaku(roomId)
                }
            }.onFailure { e ->
                logger.fError { "Failed to load live stream: ${e.message}" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "加载直播流失败: ${e.message}"
                }
            }
        }
    }

    /**
     * 切换直播画质
     * @param qn 目标画质编号
     */
    fun changeLiveQuality(qn: Int) {
        logger.fInfo { "Change live quality to: $qn" }
        loadLiveStreamWithQuality(liveRoomId, qn)
    }

    /**
     * 直播流错误时自动重连
     * 延迟 2 秒后重新获取直播流 URL 并播放。
     * 使用 liveRetryJob 做防抖：新的重连请求会取消上一次未执行的延迟重试。
     * 当直播间已关闭（liveStatus != 1）时，fetchLiveStreamUrl 返回 null，自动停止重试。
     */
    fun retryLiveStream() {
        if (!isLive) return
        logger.fInfo { "Scheduling live stream retry in 2s for room $liveRoomId" }

        // 防抖：取消上一次待执行的重试
        liveRetryJob?.cancel()
        liveRetryJob = viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            // 仅在播放器未在播放时重试
            val playing = withContext(Dispatchers.Main) { videoPlayer?.isPlaying == true }
            if (playing) {
                logger.fInfo { "Player is already playing, skip retry" }
                return@launch
            }
            logger.fInfo { "Retrying live stream for room $liveRoomId, qn=$currentLiveQn" }
            withContext(Dispatchers.Main) {
                loadState = RequestState.Doing
                // 重连时先清除错误状态，让 UI 不再显示错误
                errorMessage = ""
            }
            val playInfo = LiveStreamUrlFetcher.fetchLiveStreamUrl(liveRoomId, currentLiveQn)
            if (playInfo == null) {
                // fetchLiveStreamUrl 内部已判断 liveStatus != 1 并 Toast "主播未开播"
                // 此时不再继续重试
                logger.fInfo { "Live stream fetch returned null, live may have ended" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "直播已结束或获取直播流失败"
                }
                return@launch
            }
            // 成功获取新 URL，重新播放
            withContext(Dispatchers.Main) {
                liveStreamUrl = playInfo.streamUrl
                currentLiveQn = playInfo.currentQn
                videoPlayer?.playUrl(videoUrl = playInfo.streamUrl)
                videoPlayer?.prepare()
                videoPlayer?.start()
                loadState = RequestState.Success
            }
            logger.fInfo { "Live stream retry successful, new URL loaded" }
        }
    }

    /**
     * 加载直播流
     */
    fun loadLiveStream(streamUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            logger.fInfo { "Load live stream: $streamUrl" }
            withContext(Dispatchers.Main) { loadState = RequestState.Doing }
            
            // 初始化弹幕播放器
            ensureDanmakuPlayer()
            logger.fInfo { "Danmaku player initialized for live stream" }
            
            runCatching {
                withContext(Dispatchers.Main) {
                    videoPlayer?.playUrl(videoUrl = streamUrl)
                    videoPlayer?.prepare()
                    videoPlayer?.start()
                    loadState = RequestState.Success
                }
                logger.fInfo { "Live stream loaded successfully" }
            }.onFailure { e ->
                logger.fError { "Failed to load live stream: ${e.message}" }
                withContext(Dispatchers.Main) {
                    loadState = RequestState.Failed
                    errorMessage = "加载直播流失败: ${e.message}"
                }
            }
        }
    }
    
    /**
     * 启动直播弹幕
     */
    fun startLiveDanmaku(roomId: Int) {
        if (roomId <= 0) {
            logger.fWarn { "Invalid room id: $roomId" }
            return
        }
        
        logger.fInfo { "Starting live danmaku for room $roomId" }
        stopLiveDanmaku()
        
        // 连接 WebSocket
        liveWebSocket = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                logger.fInfo { "Getting live danmaku info for room $roomId" }
                val danmuInfo = BiliLiveHttpApi.getLiveDanmuInfo(roomId, sessData = Prefs.sessData)
                logger.fInfo { "Danmaku info response: code=${danmuInfo.code}, message=${danmuInfo.message}" }
                
                if (danmuInfo.data == null) {
                    logger.fError { "Failed to get danmaku info: data is null" }
                    return@launch
                }
                
                logger.fInfo { "Getting live room play info for room $roomId" }
                val playInfo = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId)
                logger.fInfo { "Play info response: code=${playInfo.code}, message=${playInfo.message}" }
                
                val realRoomId = playInfo.data?.roomId
                if (realRoomId == null) {
                    logger.fError { "Failed to get real room id: data.roomId is null" }
                    return@launch
                }
                
                logger.fInfo { "Real room id: $realRoomId, starting WebSocket connection" }
                
                // 创建 Channel 和单消费者协程，避免每条弹幕创建一个协程
                val channel = Channel<DanmakuEvent>(capacity = Channel.BUFFERED)
                liveDanmakuChannel = channel
                liveDanmakuConsumer = viewModelScope.launch(Dispatchers.IO) {
                    for (event in channel) {
                        addLiveDanmaku(event)
                    }
                }
                
                logger.fInfo { "Connecting to live danmaku WebSocket for room $realRoomId" }
                // 使用预取的 token 和 hostList，避免 connectLiveEvent 内部重复调用 API
                liveWebSocketInner = LiveDataWebSocket.connectLiveEvent(
                    realRoomId = realRoomId,
                    token = danmuInfo.data!!.token,
                    hostList = danmuInfo.data!!.hostList,
                    uid = Prefs.uid,
                ) { event ->
                    when (event) {
                        is DanmakuEvent -> channel.trySend(event)
                        is PopularityChangeEvent -> {
                            val now = System.currentTimeMillis()
                            if (now - lastPopularityUpdateTime >= 10_000) {
                                livePopularityText = event.popularityText
                                lastPopularityUpdateTime = now
                            }
                        }
                        is OnlineRankCountEvent -> {
                            val now = System.currentTimeMillis()
                            if (now - lastOnlineCountUpdateTime >= 10_000) {
                                liveOnlineCount = "${event.count} 高能观众"
                                lastOnlineCountUpdateTime = now
                            }
                        }
                    }
                }
            }.onFailure { e ->
                logger.fError { "Live danmaku connection failed: ${e.message}\n${e.stackTraceToString()}" }
            }
        }

        // 启动批量发送定时任务
        startLiveDanmakuFlushJob()

        logger.fInfo { "Live danmaku started" }
    }
    
    /**
     * 停止直播弹幕
     */
    fun stopLiveDanmaku() {
        logger.fInfo { "Stopping live danmaku" }

        // 停止批量发送定时任务
        stopLiveDanmakuFlushJob()

        liveWebSocket?.cancel()
        liveWebSocket = null
        liveWebSocketInner?.cancel()
        liveWebSocketInner = null
        liveDanmakuChannel?.close()
        liveDanmakuChannel = null
        liveDanmakuConsumer?.cancel()
        liveDanmakuConsumer = null

        // 清空缓冲区
        synchronized(liveDanmakuBuffer) {
            liveDanmakuBuffer.clear()
        }

        logger.fInfo { "Live danmaku stopped" }
    }
    
    /**
     * 添加直播弹幕到缓冲区
     */
    private fun addLiveDanmaku(event: DanmakuEvent) {
        val danmakuItem = DanmakuItemData(
            danmakuId = System.currentTimeMillis(),
            position = 0L, 
            content = event.content,
            mode = when (event.mode) {
                4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                else -> DanmakuItemData.DANMAKU_MODE_ROLLING
            },
            textSize = event.fontSize,
            textColor = Color(event.color).toArgb()
        )

        // 添加到缓冲区
        synchronized(liveDanmakuBuffer) {
            liveDanmakuBuffer.add(danmakuItem)
        }
    }

    /**
     * 启动直播弹幕批量发送定时任务
     */
    private fun startLiveDanmakuFlushJob() {
        liveDanmakuFlushJob?.cancel()
        liveDanmakuFlushJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000) // 每秒执行一次
                flushLiveDanmakuBuffer()
            }
        }
    }

    /**
     * 批量发送缓冲区中的弹幕
     */
    private suspend fun flushLiveDanmakuBuffer() {
        val itemsToSend = synchronized(liveDanmakuBuffer) {
            if (liveDanmakuBuffer.isEmpty()) return@synchronized emptyList()
            val items = liveDanmakuBuffer.toList()
            liveDanmakuBuffer.clear()
            items
        }

        if (itemsToSend.isEmpty()) return

        val player = danmakuPlayer ?: return
        val sendPosition = player.getCurrentTimeMs() + 1000L

        // 更新每条弹幕的 position
        val updatedItems = itemsToSend.map {
            DanmakuItemData(
                danmakuId = it.danmakuId,
                position = sendPosition,
                content = it.content,
                mode = it.mode,
                textSize = it.textSize,
                textColor = it.textColor
            )
        }

        withContext(Dispatchers.Main) {
            danmakuPlayer?.updateData(updatedItems)
        }
    }

    /**
     * 停止直播弹幕批量发送定时任务
     */
    private fun stopLiveDanmakuFlushJob() {
        liveDanmakuFlushJob?.cancel()
        liveDanmakuFlushJob = null
    }
}
