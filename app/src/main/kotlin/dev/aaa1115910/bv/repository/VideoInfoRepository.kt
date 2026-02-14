package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.video.RelatedVideo
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toWanString
import dev.aaa1115910.bv.viewmodel.video.VideoDetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class VideoInfoRepository(private val videoDetailRepository: VideoDetailRepository) {
    private val _videoList = MutableStateFlow<List<VideoListItem>>(emptyList())
    private val _videoDetailState = MutableStateFlow<VideoDetailState?>(null)

    val videoList = _videoList.asStateFlow()
    val videoDetailState = _videoDetailState.asStateFlow()

    // --- 按需加载去重缓存 ---
    private val ugcPagesMutex = Mutex()
    private val loadedUgcPagesAid = mutableSetOf<Long>()
    private val inFlightUgcPagesAid = mutableSetOf<Long>()

    /**
     * 仅为指定 aid 加载 UGC 分P列表
     * - 自动去重：已加载/加载中则直接返回
     * - 自动跳过单P（pages.size <= 1 时也会标记 loaded，避免重复请求）
     */
    suspend fun ensureUgcPagesLoaded(aid: Long, preferApiType: ApiType = ApiType.Web) {
        val shouldLoad = ugcPagesMutex.withLock {
            if (loadedUgcPagesAid.contains(aid)) return
            if (inFlightUgcPagesAid.contains(aid)) return
            inFlightUgcPagesAid.add(aid)
            true
        }
        if (!shouldLoad) return

        try {
            val pages = videoDetailRepository.getUgcPages(aid = aid, preferApiType = preferApiType)

            // 失败/取不到数据：不要把 aid 标记为 loaded，否则会“锁死”永远不再重试
            if (pages.isEmpty()) return

            _videoList.update { oldList ->
                oldList.map { item ->
                    if (item.aid != aid) return@map item

                    // 这里用 nullable 的 ugcPages 做三态：
                    // null      -> 未加载 / 上次失败
                    // emptyList -> 已加载但单P（无可展示子项）
                    // notEmpty  -> 多P
                    if (pages.size > 1) item.copy(ugcPages = pages)
                    else item.copy(ugcPages = emptyList())
                }
            }

            ugcPagesMutex.withLock {
                loadedUgcPagesAid.add(aid)
            }
        } finally {
            ugcPagesMutex.withLock {
                inFlightUgcPagesAid.remove(aid)
            }
        }
    }

    suspend fun updateUgcPages(preferApiType: ApiType = ApiType.Web) {
        _videoList.update { oldList ->
            oldList.map { item ->
                val pages =
                    videoDetailRepository.getUgcPages(aid = item.aid, preferApiType = preferApiType)
                /*
                if (pages.size > 1) {
                    item.copy(
                        ugcPages = pages,
                    )
                } else {
                    item
                }
                */
                item.copy(ugcPages = pages)
            }
        }
    }

    fun clearVideoList() {
        _videoList.value = emptyList()
        // 清缓存
        loadedUgcPagesAid.clear()
        inFlightUgcPagesAid.clear()
    }

        suspend fun loadVideoDetail(aid: Long, preferApiType: ApiType) {
            val videoDetail = videoDetailRepository.getVideoDetail(
                aid = aid,
                preferApiType = preferApiType
            )

            val videoDetailState = VideoDetailState(
                aid = videoDetail.aid,
                bvid = videoDetail.bvid,
                title = videoDetail.title,
                lastPlayedCid = videoDetail.history.lastPlayedCid,
                lastPlayedTime = videoDetail.history.progress,
                isLiked = videoDetail.userActions.like,
                isCoined = videoDetail.userActions.coin,
                isFavorite = videoDetail.userActions.favorite,
                cid = videoDetail.cid,
                cover = videoDetail.cover,
                publishDate = videoDetail.publishDate,
                stat = videoDetail.stat,
                author = videoDetail.author,
                tags = videoDetail.tags,
                isUpowerExclusive = videoDetail.isUpowerExclusive,
                redirectToEp = videoDetail.redirectToEp,
                argueTip = videoDetail.argueTip,
                description = videoDetail.description,
                pages = videoDetail.pages,
                relatedVideos = mapToVideoCardData(videoDetail.relatedVideos),
                ugcSeason = videoDetail.ugcSeason,
            )

            _videoDetailState.update { videoDetailState }

            // 仅当前 aid：复用详情页 pages 写回到 videoList 对应项 ugcPages
            // 约定三态与 ensureUgcPagesLoaded 一致：
            // - ugcPages == null      -> 未加载/上次失败
            // - ugcPages == emptyList -> 已加载但单P（无需展示子项）
            // - ugcPages.isNotEmpty() -> 多P
            val pages = videoDetailState.pages
            if (pages.isNotEmpty()) {
                var wroteUgcPages = false
                _videoList.update { oldList ->
                    oldList.map { item ->
                        if (item.aid != aid) return@map item
                        wroteUgcPages = true
                        if (pages.size > 1) item.copy(ugcPages = pages) else item.copy(ugcPages = emptyList())
                    }
                }

                // 只有当当前 videoList 里确实存在该 aid 且写回成功时，才标记“已加载”，避免跨列表污染
                if (wroteUgcPages) {
                    loadedUgcPagesAid.add(aid)
                }
            }
        }

         fun updateVideoList(videoListItem: List<VideoListItem>) {
             _videoList.update { videoListItem }
             // - ugcPages == null      -> 未加载/上次失败，必须允许后续 ensureUgcPagesLoaded 重新触发
             // - ugcPages != null      -> 已加载（emptyList 表示单P）
             //
             // 同时丢弃不在当前列表里的 aid，避免缓存无限增长/跨列表污染。
             val currentAids = videoListItem.asSequence().map { it.aid }.toSet()
             loadedUgcPagesAid.retainAll(currentAids)
             inFlightUgcPagesAid.retainAll(currentAids)

             videoListItem.forEach { item ->
                 val aid = item.aid
                 if (item.ugcPages == null) {
                     // 如果列表里显示为“未加载”，loaded 集合里就不能认为它“已加载”
                     loadedUgcPagesAid.remove(aid)
                 } else {
                     // 列表里已经有 ugcPages（三态中的 emptyList / notEmpty），视为“已加载”
                     loadedUgcPagesAid.add(aid)
                 }
             }
         }

        fun updateHistory(progress: Int, lastPlayedCid: Long) {
            _videoDetailState.update {
                it?.copy(
                    lastPlayedCid = lastPlayedCid,
                    lastPlayedTime = progress
                )
            }
        }

        fun reset() {
            _videoList.update { emptyList() }
            _videoDetailState.update { null }

            // reset 会清空 _videoList，但如果不清去重缓存，可能出现：
            // loadedUgcPagesAid 仍认为“已加载”，导致 ensureUgcPagesLoaded 直接 return，
            // 而新列表项 ugcPages 仍为 null -> UI 永远“加载中……”
            loadedUgcPagesAid.clear()
            inFlightUgcPagesAid.clear()
        }

        private fun mapToVideoCardData(relatedVideos: List<RelatedVideo>): List<VideoCardData> {
            val relateVideoCardDataList = relatedVideos.map {
                VideoCardData(
                    avid = it.aid,
                    cid = it.cid,
                    title = it.title,
                    cover = it.cover,
                    upName = it.author?.name ?: "",
                    upMid = it.author?.mid,
                    timeString = (it.duration * 1000L).formatHourMinSec(),
                    playString = it.view.toWanString(),
                    danmakuString = it.danmaku.toWanString(),
                    jumpToSeason = it.jumpToSeason,
                    epId = it.epid,
                )
            }

            return relateVideoCardDataList
        }
    }