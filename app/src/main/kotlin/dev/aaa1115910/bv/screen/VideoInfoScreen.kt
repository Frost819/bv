package dev.aaa1115910.bv.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SuggestionChip
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.video.Dimension
import dev.aaa1115910.biliapi.entity.video.Tag
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.http.BiliPlusHttpApi
import dev.aaa1115910.biliapi.repositories.CoinRepository
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.LikeRepository
import dev.aaa1115910.biliapi.repositories.OneClickTripleActionRepository
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.SeasonInfoActivity
import dev.aaa1115910.bv.activities.video.TagActivity
import dev.aaa1115910.bv.activities.video.UpInfoActivity
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.TvLazyVerticalGrid
import dev.aaa1115910.bv.component.UpIcon
import dev.aaa1115910.bv.component.buttons.CoinButton
import dev.aaa1115910.bv.component.buttons.FavoriteButton
import dev.aaa1115910.bv.component.buttons.LikeButton
import dev.aaa1115910.bv.component.ifElse
import dev.aaa1115910.bv.component.videocard.VideosRow
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.ui.common.UiEvent
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fDebug
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.focusedBorder
import dev.aaa1115910.bv.util.formatPubTimeString
import dev.aaa1115910.bv.util.launchPlayerActivity
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.swapList
import dev.aaa1115910.bv.util.swapListWithMainContext
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoInfoState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import kotlin.math.ceil

@Composable
fun VideoInfoScreen(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    videoDetailViewModel: VideoDetailViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    userRepository: UserRepository = getKoin().get(),
    favoriteRepository: FavoriteRepository = getKoin().get(),
    likeRepository: LikeRepository = getKoin().get(),
    coinRepository: CoinRepository = getKoin().get(),
    oneClickTripleActionRepository: OneClickTripleActionRepository = getKoin().get()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val intent = (context as Activity).intent
    val logger = KotlinLogging.logger { }
    val defaultFocusRequester = remember { FocusRequester() }

    var showFollowButton by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }

    var lastPlayedCid by remember { mutableLongStateOf(0) }
    var lastPlayedTime by remember { mutableIntStateOf(0) }

    var tip by remember { mutableStateOf("Loading") }
    var fromSeason by remember { mutableStateOf(false) }
    var fromController by remember { mutableStateOf(false) }
    val showVideoInfo by remember { mutableStateOf(Prefs.showVideoInfo) }
    var paused by remember { mutableStateOf(false) }
    var proxyArea by remember { mutableStateOf(ProxyArea.MainLand) }

    val containsVerticalScreenVideo by remember {
        derivedStateOf {
            videoDetailViewModel.videoDetail?.pages?.any { it.dimension.isVertical } ?: false
                    || videoDetailViewModel.videoDetail?.ugcSeason?.sections?.any { section -> section.episodes.any { it.dimension!!.isVertical } } ?: false
        }
    }

    var favorited by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(false) }
    var coined by remember { mutableStateOf(false) }

    val favoriteFolderMetadataList = remember { mutableStateListOf<FavoriteFolderMetadata>() }
    val videoInFavoriteFolderIds = remember { mutableStateListOf<Long>() }

    val setHistory = {
        logger.info { "play history: ${videoDetailViewModel.videoDetail?.history}" }
        lastPlayedCid = videoDetailViewModel.videoDetail?.history?.lastPlayedCid ?: 0
        lastPlayedTime = videoDetailViewModel.videoDetail?.history?.progress ?: 0
    }

    val updateHistory = {
        scope.launch(Dispatchers.IO) {
            runCatching {
                videoDetailViewModel.loadDetailOnlyUpdateHistory(videoDetailViewModel.videoDetail!!.aid)
            }
            withContext(Dispatchers.Main) {
                setHistory()
            }
        }
    }


    val updateFollowingState: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Checking is following user $userMid" }
            val success = userRepository.checkIsFollowing(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Following user result: $success" }
            withContext(Dispatchers.Main) {
                showFollowButton = success != null
                isFollowing = success == true
            }
        }
    }

    val addFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Add follow to user $userMid" }
            val success = userRepository.followUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Add follow result: $success" }
            afterModify(success)
        }
    }

    val delFollow: (afterModify: (success: Boolean) -> Unit) -> Unit = { afterModify ->
        scope.launch(Dispatchers.IO) {
            val userMid = videoDetailViewModel.videoDetail?.author?.mid ?: -1
            logger.fInfo { "Del follow to user $userMid" }
            val success = userRepository.unfollowUser(
                mid = userMid,
                preferApiType = Prefs.apiType
            )
            logger.fInfo { "Del follow result: $success" }
            afterModify(success)
        }
    }

    val fetchFavoriteData: (Long) -> Unit = { avid ->
        scope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderMetadataListResult =
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = Prefs.uid,
                        rid = avid,
                        preferApiType = Prefs.apiType
                    )
                favoriteFolderMetadataList.swapListWithMainContext(favoriteFolderMetadataListResult)

                val videoInFavoriteFolderIdsResult = favoriteFolderMetadataListResult
                    .filter { it.videoInThisFav }
                videoInFavoriteFolderIds.swapListWithMainContext(videoInFavoriteFolderIdsResult.map { it.id })

                logger.fDebug { "Update favoriteFolders: ${favoriteFolderMetadataList.map { it.title }}" }
                logger.fDebug { "Update videoInFavoriteFolderIds: ${videoInFavoriteFolderIdsResult.map { it.title }}" }
            }
        }
    }

    val updateVideoFavoriteData: (List<Long>) -> Unit = { folderIds ->
        scope.launch(Dispatchers.IO) {
            runCatching {
                require(favoriteFolderMetadataList.isNotEmpty()) { "Not found favorite folder" }
                require(videoDetailViewModel.videoDetail?.aid != null) { "Video info is null" }
                logger.info { "Update video av${videoDetailViewModel.videoDetail?.aid} to favorite folder $folderIds" }

                favoriteRepository.updateVideoToFavoriteFolder(
                    aid = videoDetailViewModel.videoDetail!!.aid,
                    addMediaIds = folderIds,
                    delMediaIds = favoriteFolderMetadataList.map { it.id } - folderIds.toSet()
                )
            }.onFailure {
                logger.fInfo { "Update video to favorite folder failed: ${it.stackTraceToString()}" }
                withContext(Dispatchers.Main) {
                    it.message ?: "unknown error".toast(context)
                }
            }.onSuccess {
                logger.fInfo { "Update video to favorite folder success" }
                videoInFavoriteFolderIds.swapListWithMainContext(folderIds)
            }
        }
    }

    val addVideoToDefaultFavoriteFolder: () -> Boolean = {
        runCatching {
            val defaultFavoriteFolder =
                favoriteFolderMetadataList.firstOrNull { it.title == "默认收藏夹" }
            require(defaultFavoriteFolder != null) { "Not found default favorite folder" }
            updateVideoFavoriteData(listOf(defaultFavoriteFolder.id))
        }.onFailure {
            logger.fInfo { "Add video to default favorite folder failed: ${it.stackTraceToString()}" }
            it.message ?: "unknown error".toast(context)
        }.isSuccess
    }

    val updateVideoIsFavoured = {
        favorited = videoDetailViewModel.videoDetail?.userActions?.favorite ?: false
    }

    val updateVideoIsLiked = {
        liked = videoDetailViewModel.videoDetail?.userActions?.like ?: false
    }

    val updateVideoIsCoined = {
        coined = videoDetailViewModel.videoDetail?.userActions?.coin ?: false
    }

    fun playCurrentVideo(cid: Long? = null) {
        val videoDetail = videoDetailViewModel.videoDetail!!
        videoDetailViewModel.clearVideoList()
        videoDetailViewModel.addToVideoList(
            listOf(
                VideoListItem(
                    aid = videoDetail.aid,
                    cid = cid ?: videoDetail.cid,
                    title = videoDetail.title,
                )
            )
        )

        launchPlayerActivity(
            context = context,
            avid = videoDetail.aid,
            cid = cid ?: videoDetail.cid,
            title = videoDetail.title,
            partTitle = videoDetail.pages.find { it.cid == cid }?.title
                ?: videoDetail.pages.first().title,
            played = if (cid?.let { it == lastPlayedCid } ?: (videoDetail.cid == lastPlayedCid)) {
                lastPlayedTime * 1000
            } else 0,
            fromSeason = fromSeason,
            isVerticalVideo = containsVerticalScreenVideo,
            author = videoDetail.author
        )
    }

    suspend fun updateVideoLikedData(like: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                logger.info { "Check video ${videoDetailViewModel.videoDetail?.aid} is liked" }
                likeRepository.updateVideoLiked(
                    like = like,
                    aid = videoDetailViewModel.videoDetail!!.aid,
                    bvid = videoDetailViewModel.videoDetail!!.bvid
                )
            }.onFailure { throwable ->
                logger.fInfo { "Update video liked status failed: ${throwable.stackTraceToString()}" }
            }.onSuccess {
                logger.fInfo { "Update video liked status success" }
            }.isSuccess // 返回成功与否
        }
    }

    suspend fun sendVideoCoin(): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                logger.info { "Check video ${videoDetailViewModel.videoDetail?.aid} is liked" }
                coinRepository.sendVideoCoin(
                    aid = videoDetailViewModel.videoDetail!!.aid,
                    bvid = videoDetailViewModel.videoDetail!!.bvid
                )
            }.onFailure { throwable ->
                logger.fInfo { "Send video coin failed: ${throwable.stackTraceToString()}" }
            }.onSuccess {
                logger.fInfo { "Send video coin success" }
            }.isSuccess // 返回成功与否
        }
    }

    suspend fun sendVideoOneClickTripleAction(): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val data =
                    oneClickTripleActionRepository.sendVideoOneClickTripleAction(
                        aid = videoDetailViewModel.videoDetail!!.aid,
                        bvid = videoDetailViewModel.videoDetail!!.bvid
                    )
                if (data != null) {
                    withContext(Dispatchers.Main) {
                        liked = data.like
                        coined = data.coin
                        favorited = data.fav
                    }
                    if (favorited) addVideoToDefaultFavoriteFolder()
                }
            }.onFailure { throwable ->
                logger.fInfo { "Send video one click triple action failed: ${throwable.stackTraceToString()}" }
            }.onSuccess {
                logger.fInfo { "Send video one click triple action success" }
            }.isSuccess
        }
    }


    LaunchedEffect(Unit) {
        if (intent.hasExtra("aid")) {
            val aid = intent.getLongExtra("aid", 170001)
            fromSeason = intent.getBooleanExtra("fromSeason", false)
            fromController = intent.getBooleanExtra("fromController", false)
            proxyArea = ProxyArea.entries[intent.getIntExtra("proxyArea", 0)]
            //获取视频信息
            scope.launch(Dispatchers.IO) {
                if (proxyArea != ProxyArea.MainLand) {
                    runCatching {
                        val seasonId = BiliPlusHttpApi.getSeasonIdByAvid(aid)
                        logger.info { "Get season id from biliplus: $seasonId" }
                        seasonId?.let {
                            logger.fInfo { "Redirect to season $seasonId" }
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = seasonId,
                                proxyArea = proxyArea
                            )
                            context.finish()
                        }
                    }.onFailure {
                        logger.fWarn { "Redirect failed: ${it.stackTraceToString()}" }
                    }
                }

                runCatching {
                    videoDetailViewModel.loadDetail(aid)
                    withContext(Dispatchers.Main) {
                        updateVideoIsFavoured()
                        updateVideoIsLiked()
                        updateVideoIsCoined()
                        setHistory()
                    }
                    if (Prefs.isLogin) fetchFavoriteData(aid)

                    if (!fromController) {
                        //如果是从剧集跳转过来的或设置不显示视频详情，就直接播放当前视频
                        if (fromSeason || !showVideoInfo) {
                            playCurrentVideo(lastPlayedCid.takeIf { it != 0L })
                            context.finish()
                        }
                    }
                }.onFailure {
                    val errorMessage = it.localizedMessage
                    val isVideoNotFound = when (Prefs.apiType) {
                        ApiType.Web -> errorMessage == "啥都木有"
                        ApiType.App -> errorMessage == "访问权限不足"
                    }

                    logger.fInfo { "Get video info failed: ${it.stackTraceToString()}" }
                    if (!isVideoNotFound || !Prefs.enableProxy) {
                        withContext(Dispatchers.Main) {
                            tip = it.localizedMessage ?: "未知错误"
                        }
                        return@onFailure
                    }
                    withContext(Dispatchers.Main) {
                        videoDetailViewModel.state = VideoInfoState.Loading
                    }

                    logger.fInfo { "Trying get video info through proxy server" }
                    runCatching {
                        val seasonId = BiliPlusHttpApi.getSeasonIdByAvid(aid)
                        logger.info { "Get season id from biliplus: $seasonId" }
                        seasonId?.let {
                            logger.fInfo { "Redirect to season $seasonId" }
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = seasonId,
                                proxyArea = ProxyArea.HongKong
                            )
                            context.finish()
                        } ?: let {
                            withContext(Dispatchers.Main) {
                                tip = "视频不存在"
                                videoDetailViewModel.state = VideoInfoState.Error
                            }
                        }
                    }.onFailure { e ->
                        logger.fWarn { "Redirect failed: ${e.stackTraceToString()}" }
                        withContext(Dispatchers.Main) {
                            tip = e.localizedMessage ?: "未知错误"
                            videoDetailViewModel.state = VideoInfoState.Error
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    LaunchedEffect(videoDetailViewModel.videoDetail) {
        //如果是从剧集页跳转回来的，那就不需要再跳转到剧集页了
        if (fromSeason) return@LaunchedEffect

        videoDetailViewModel.videoDetail?.let {
            if (it.redirectToEp) {
                runCatching {
                    logger.fInfo { "Redirect to ep ${it.epid}" }
                    SeasonInfoActivity.actionStart(
                        context = context,
                        epId = it.epid,
                        proxyArea = proxyArea
                    )
                    context.finish()
                }.onFailure {
                    logger.fWarn { "Redirect failed: ${it.stackTraceToString()}" }
                }
            } else {
                logger.fInfo { "No redirection required" }
                defaultFocusRequester.requestFocus(scope)
            }

            if (!fromSeason) updateFollowingState()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 如果 pause==true 那可能是从播放页返回回来的，此时更新历史记录
                if (paused) updateHistory()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (videoDetailViewModel.videoDetail == null ||
        videoDetailViewModel.videoDetail?.redirectToEp == true ||
        fromSeason ||
        (if (fromController) false else !showVideoInfo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = tip
            )
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier.padding(innerPadding)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (videoDetailViewModel.videoDetail?.isUpowerExclusive == true) {
                                ArgueTip(text = stringResource(R.string.video_info_argue_tip_upower_exclusive))
                            }
                            if (containsVerticalScreenVideo) {
                                ArgueTip(text = stringResource(R.string.video_info_argue_tip_vertical_screen))
                            }
                            if (videoDetailViewModel.videoDetail?.argueTip != null) {
                                ArgueTip(text = videoDetailViewModel.videoDetail!!.argueTip!!)
                            }
                        }
                    }
                    item {
                        VideoInfoData(
                            defaultFocusRequester = defaultFocusRequester,
                            videoDetail = videoDetailViewModel.videoDetail!!,
                            showFollowButton = showFollowButton,
                            isFollowing = isFollowing,
                            tags = videoDetailViewModel.videoDetail!!.tags,
                            isFavorite = favorited,
                            isLiked = liked,
                            isCoined = coined,
                            userFavoriteFolders = favoriteFolderMetadataList,
                            favoriteFolderIds = videoInFavoriteFolderIds,
                            onClickCover = {
                                logger.fInfo { "Click video cover" }
                                // 点击封面播放当前视频
                                playCurrentVideo(lastPlayedCid.takeIf { it != 0L })
                            },
                            onClickUp = {
                                UpInfoActivity.actionStart(
                                    context,
                                    mid = videoDetailViewModel.videoDetail!!.author.mid,
                                    name = videoDetailViewModel.videoDetail!!.author.name
                                )
                            },
                            onAddFollow = {
                                addFollow {
                                    updateFollowingState()
                                }
                            },
                            onDelFollow = {
                                delFollow {
                                    updateFollowingState()
                                }
                            },
                            onClickTip = { tag ->
                                TagActivity.actionStart(
                                    context = context,
                                    tagId = tag.id,
                                    tagName = tag.name
                                )
                            },
                            onAddToDefaultFavoriteFolder = {
                                if (addVideoToDefaultFavoriteFolder())
                                    favorited = true
                                else
                                    "收藏失败".toast(context)
                            },
                            onUpdateFavoriteFolders = {
                                updateVideoFavoriteData(it)
                                favorited = it.isNotEmpty()
                                videoInFavoriteFolderIds.swapList(it)
                            },
                            onUpdateLiked = {
                                scope.launch(Dispatchers.Main) {
                                    if (updateVideoLikedData(it))
                                        liked = it
                                    else
                                        "点赞失败".toast(context)
                                }
                            },
                            onSendVideoCoin = {
                                scope.launch(Dispatchers.Main) {
                                    if (!coined) {
                                        if (sendVideoCoin()) coined = true
                                        else "投币失败".toast(context)
                                    }
                                }
                            },
                            onSendVideoOneClickTripleAction = {
                                scope.launch(Dispatchers.Main) {
                                    if (sendVideoOneClickTripleAction()) {
                                        "一键三连".toast(context)
                                    } else {
                                        "一键三连失败".toast(context)
                                    }
                                }
                            }
                        )
                    }
                    if ((videoDetailViewModel.videoDetail?.description ?: "").isNotBlank()) {
                        item {
                            VideoDescription(
                                description = videoDetailViewModel.videoDetail?.description
                                    ?: "no desc"
                            )
                        }
                    }

                    item {
                        //视频分P
                        VideoPartRow(
                            pages = videoDetailViewModel.videoDetail?.pages ?: emptyList(),
                            lastPlayedCid = lastPlayedCid,
                            lastPlayedTime = lastPlayedTime,
                            enablePartListDialog =
                            (videoDetailViewModel.videoDetail?.pages?.size ?: 0) > 5,
                            onClick = { cid ->
                                logger.fInfo { "Click video part: [av:${videoDetailViewModel.videoDetail?.aid}, bv:${videoDetailViewModel.videoDetail?.bvid}, cid:$cid]" }
                                // 播放当前视频的对应分P
                                playCurrentVideo(cid)
                            }
                        )
                    }

                    val videoDetail = videoDetailViewModel.videoDetail
                    videoDetail?.ugcSeason?.let { season ->
                        itemsIndexed(items = season.sections) { index, section ->
                            VideoUgcSeasonRow(
                                title = if (season.sections.size == 1) season.title else section.title,
                                episodes = section.episodes,
                                lastPlayedCid = lastPlayedCid,
                                lastPlayedTime = lastPlayedTime,
                                enableUgcListDialog = section.episodes.size > 5,
                                onClick = { aid, cid ->
                                    logger.fInfo { "Click ugc season part: [av:${videoDetail.aid}, bv:${videoDetail.bvid}, cid:$lastPlayedCid]" }

                                    // 读取合集内视频
                                    videoDetailViewModel.updateUgcSeasonSectionVideoList(index)

                                    val currentEpisode = section.episodes.find { it.cid == cid }
                                    val episodeTitle = currentEpisode?.title ?: ""

                                    launchPlayerActivity(
                                        context = context,
                                        avid = aid,
                                        cid = cid,
                                        title = episodeTitle,
                                        partTitle = episodeTitle,
                                        played = if (cid == lastPlayedCid) lastPlayedTime * 1000 else 0,
                                        fromSeason = false,
                                        isVerticalVideo = containsVerticalScreenVideo,
                                        author = videoDetail.author
                                    )
                                }
                            )
                        }
                    }

                    val relatedVideos = videoDetailViewModel.relatedVideos
                    if (relatedVideos.isNotEmpty()) {
                        item {
                            VideosRow(
                                header = stringResource(R.string.video_info_related_video_title),
                                videos = relatedVideos,
                                showMore = {},
                                onAddWatchLater = { aid ->
                                    toViewViewModel.addToView(aid)
                                },
                                onGoToDetailPage = { aid ->
                                    VideoInfoActivity.actionStart(
                                        context = context,
                                        fromController = true,
                                        aid = aid
                                    )
                                },
                                onGoToUpPage = { mid, upName ->
                                    UpInfoActivity.actionStart(context, mid, upName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArgueTip(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        colors = SurfaceDefaults.colors(
            containerColor = Color.Yellow.copy(alpha = 0.2f),
            contentColor = Color.Yellow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color.Yellow
            )
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoInfoData(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    videoDetail: VideoDetail,
    showFollowButton: Boolean,
    isFollowing: Boolean,
    tags: List<Tag>,
    isFavorite: Boolean,
    isLiked: Boolean,
    isCoined: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onClickCover: () -> Unit,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit,
    onClickTip: (Tag) -> Unit,
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    onUpdateLiked: (Boolean) -> Unit,
    onSendVideoCoin: () -> Unit,
    onSendVideoOneClickTripleAction: () -> Unit
) {
    val localDensity = LocalDensity.current
    var heightIs by remember { mutableStateOf(0.dp) }

    Row(
        modifier = modifier
            .padding(horizontal = 50.dp, vertical = 16.dp),
    ) {
        Surface(
            modifier = Modifier
                .focusRequester(defaultFocusRequester)
                .weight(3f)
                .aspectRatio(1.6f)
                .onGloballyPositioned { coordinates ->
                    heightIs = with(localDensity) { coordinates.size.height.toDp() }
                },
            onClick = onClickCover,
            shape = ClickableSurfaceDefaults.shape(
                shape = MaterialTheme.shapes.large,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 3.dp, color = MaterialTheme.colorScheme.border),
                    shape = MaterialTheme.shapes.large
                )
            ),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = videoDetail.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier
                .weight(7f)
                .height(heightIs),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = videoDetail.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.labelMedium
                    ) {
                        Text(text = "发布于 ${videoDetail.publishDate.formatPubTimeString()}")
                        Text(text = "·")
                        Text(text = "播放量 ${(videoDetail.stat.view).toWanString()}")
                        Text(text = "·")
                        Text(text = "弹幕 ${(videoDetail.stat.danmaku).toWanString()}")
                        Text(text = "·")
                        Text(text = "点赞 ${videoDetail.stat.like.toWanString()}")
                        Text(text = "·")
                        Text(text = "投币 ${videoDetail.stat.coin.toWanString()}")
                        Text(text = "·")
                        Text(text = "收藏 ${videoDetail.stat.favorite.toWanString()}")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpButton(
                        name = videoDetail.author.name,
                        followed = isFollowing,
                        showFollowButton = showFollowButton,
                        onClickUp = onClickUp,
                        onAddFollow = onAddFollow,
                        onDelFollow = onDelFollow
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LikeButton(
                    isLiked = isLiked,
                    onClick = { onUpdateLiked(!isLiked) },
                    onLongClick = { onSendVideoOneClickTripleAction() })
                Spacer(modifier = Modifier.width(5.dp))
                CoinButton(
                    isCoined = isCoined,
                    onClick = onSendVideoCoin,
                )
                Spacer(modifier = Modifier.width(5.dp))
                FavoriteButton(
                    isFavorite = isFavorite,
                    userFavoriteFolders = userFavoriteFolders,
                    favoriteFolderIds = favoriteFolderIds,
                    onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                    onUpdateFavoriteFolders = onUpdateFavoriteFolders
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = tags) { tag ->
                        SuggestionChip(onClick = {
                            onClickTip(tag)
                        }) {
                            Text(text = tag.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpButton(
    modifier: Modifier = Modifier,
    name: String,
    followed: Boolean,
    showFollowButton: Boolean = false,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit
) {
    val view = LocalView.current
    val isLogin by remember { mutableStateOf(if (!view.isInEditMode) Prefs.isLogin else true) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.2f))
                .focusedBorder(MaterialTheme.shapes.small)
                .padding(4.dp)
                .clickable { onClickUp() },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpIcon(color = Color.White)
            Text(text = name, color = Color.White)
        }
        AnimatedVisibility(visible = isLogin && showFollowButton) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.2f))
                    .focusedBorder(MaterialTheme.shapes.small)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .clickable { if (followed) onDelFollow() else onAddFollow() }
                    .animateContentSize()
            ) {
                if (followed) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(R.string.video_info_followed),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(text = stringResource(R.string.video_info_follow), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun VideoDescription(
    modifier: Modifier = Modifier,
    description: String
) {
    var hasFocus by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )
    var showDescriptionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = 50.dp),
    ) {
        Text(
            text = stringResource(R.string.video_info_description_title),
            fontSize = titleFontSize.sp,
            color = titleColor
        )
        Box(
            modifier = Modifier
                .padding(top = 15.dp)
                .onFocusChanged { hasFocus = it.hasFocus }
                .clip(MaterialTheme.shapes.medium)
                .focusedBorder(MaterialTheme.shapes.medium)
                .padding(8.dp)
                .clickable {
                    showDescriptionDialog = true
                }
        ) {
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
    }

    VideoDescriptionDialog(
        show = showDescriptionDialog,
        onHideDialog = { showDescriptionDialog = false },
        description = description
    )
}

@Composable
fun VideoDescriptionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    description: String
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = {
                Text(
                    text = stringResource(R.string.video_info_description_title),
                    color = Color.White
                )
            },
            text = {
                LazyColumn {
                    item {
                        Text(text = description)
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun VideoPartButton(
    modifier: Modifier = Modifier,
    title: String,
    duration: Int,
    played: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(200.dp, 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.2f))
                    .fillMaxHeight()
                    .fillMaxWidth(if (played < 0) 1f else (played / duration.toFloat()))
            ) {}
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoPartRowButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.size(64.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun VideoPartRow(
    modifier: Modifier = Modifier,
    pages: List<VideoPage>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    enablePartListDialog: Boolean = false,
    onClick: (cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showPartListDialog by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.video_info_part_row_title),
            fontSize = titleFontSize.sp,
            color = titleColor
        )

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enablePartListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showPartListDialog = true }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.ViewModule,
                            contentDescription = null
                        )
                    }
                }
            }

            val matchedPage = pages.find { it.cid == lastPlayedCid }
            if (matchedPage != null && pages.size > 1) {
                item {
                    // 分P历史播放按钮
                    VideoPartRowButton(
                        onClick = { onClick(matchedPage.cid) }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.History,
                            contentDescription = null
                        )
                    }
                }
            }

            itemsIndexed(items = pages, key = { _, page -> page.cid }) { index, page ->
                VideoPartButton(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    title = page.title,
                    played = if (page.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = page.duration,
                    onClick = { onClick(page.cid) }
                )
            }
        }
    }

    VideoPartListDialog(
        show = showPartListDialog,
        onHideDialog = { showPartListDialog = false },
        pages = pages,
        title = "分 P 列表",
        onClick = onClick
    )
}

@Composable
fun VideoUgcSeasonRow(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    enableUgcListDialog: Boolean = false,
    onClick: (avid: Long, cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showUgcListDialog by remember { mutableStateOf(false) }
    val titleColor = if (hasFocus) Color.White else Color.White.copy(alpha = 0.6f)
    val titleFontSize by animateFloatAsState(
        targetValue = if (hasFocus) 30f else 14f,
        label = "title font size"
    )

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = titleFontSize.sp,
            color = titleColor
        )

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enableUgcListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showUgcListDialog = true }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.ViewModule,
                            contentDescription = null
                        )
                    }
                }
            }

            val matchedEp = episodes.find { it.cid == lastPlayedCid }
            if (matchedEp != null && episodes.size > 1) {
                item {
                    // ugc分季历史播放按钮
                    VideoPartRowButton(
                        onClick = { onClick(matchedEp.aid, matchedEp.cid) }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            imageVector = Icons.Rounded.History,
                            contentDescription = null
                        )
                    }
                }
            }

            itemsIndexed(items = episodes) { index, episode ->
                VideoPartButton(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    title = episode.title,
                    played = if (episode.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = episode.duration,
                    onClick = { onClick(episode.aid, episode.cid) }
                )
            }
        }
    }

    VideoUgcListDialog(
        show = showUgcListDialog,
        onHideDialog = { showUgcListDialog = false },
        episodes = episodes,
        title = "合集列表",
        onClick = onClick
    )
}

@Composable
private fun VideoPartListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    pages: List<VideoPage>,
    onHideDialog: () -> Unit,
    onClick: (cid: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(pages.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<VideoPage>() }

    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= pages.size) {
            toIndex = pages.size
        }
        selectedVideoPart.swapListWithMainContext(pages.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabRowFocusRequester.requestFocus(scope)
        if (show && tabCount == 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(600.dp, 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItem(0)
                                    }
                                }
                            },
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabRowFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    TvLazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { _, video -> video.cid }
                        ) { index, page ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier

                            VideoPartButton(
                                modifier = buttonModifier,
                                title = page.title,
                                played = 0,
                                duration = page.duration,
                                onClick = { onClick(page.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun VideoUgcListDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    episodes: List<Episode>,
    onHideDialog: () -> Unit,
    onClick: (avid: Long, cid: Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabCount by remember { mutableIntStateOf(ceil(episodes.size / 20.0).toInt()) }
    val selectedVideoPart = remember { mutableStateListOf<Episode>() }

    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(selectedTabIndex) {
        val fromIndex = selectedTabIndex * 20
        var toIndex = (selectedTabIndex + 1) * 20
        if (toIndex >= episodes.size) {
            toIndex = episodes.size
        }
        selectedVideoPart.swapListWithMainContext(episodes.subList(fromIndex, toIndex))
    }

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabRowFocusRequester.requestFocus(scope)
        if (show && tabCount == 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        AlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier.size(600.dp, 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabRow(
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    scope.launch(Dispatchers.Main) {
                                        listState.scrollToItem(0)
                                    }
                                }
                            },
                        selectedTabIndex = selectedTabIndex,
                        separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    ) {
                        for (i in 0 until tabCount) {
                            Tab(
                                modifier = if (i == 0) Modifier.focusRequester(
                                    tabRowFocusRequester
                                ) else Modifier,
                                selected = i == selectedTabIndex,
                                onFocus = { selectedTabIndex = i },
                            ) {
                                Text(
                                    text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }

                    TvLazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = selectedVideoPart,
                            key = { _, video -> video.cid }
                        ) { index, episode ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier

                            VideoPartButton(
                                modifier = buttonModifier,
                                title = episode.title,
                                played = 0,
                                duration = episode.duration,
                                onClick = { onClick(episode.aid, episode.cid) }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun VideoPartButtonShortTextPreview() {
    BVTheme {
        VideoPartButton(
            title = "这是一段短文字",
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartButtonLongTextPreview() {
    BVTheme {
        VideoPartButton(
            title = "这可能是我这辈子距离梅西最近的一次",
            played = 23333,
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartRowPreview() {
    val pages = remember { mutableStateListOf<VideoPage>() }
    for (i in 0..10) {
        pages.add(
            VideoPage(
                cid = 1000L + i,
                index = i,
                title = "这可能是我这辈子距离梅西最近的一次",
                duration = 10,
                dimension = Dimension(0, 0)
            )
        )
    }
    BVTheme {
        VideoPartRow(pages = pages, onClick = {})
    }
}

@Preview
@Composable
fun VideoDescriptionPreview() {
    BVTheme {
        VideoDescription(description = "12435678")
    }
}

@Preview
@Composable
private fun UpButtonPreview() {
    var followed by remember { mutableStateOf(false) }
    BVTheme {
        UpButton(
            name = "12435678",
            followed = followed,
            onClickUp = { followed = !followed },
            onAddFollow = {},
            onDelFollow = {}
        )
    }
}