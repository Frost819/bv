package dev.aaa1115910.bv.repository

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.video.VideoDetail.History
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.bv.entity.VideoListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class VideoInfoRepository(private val videoDetailRepository: VideoDetailRepository) {
    private val _videoList = MutableStateFlow<List<VideoListItem>>(emptyList())
    val videoList = _videoList.asStateFlow()
    private val _history = MutableStateFlow(History(progress = 0, lastPlayedCid = 0))
    val history = _history.asStateFlow()

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
            _videoList.update { oldList ->
                oldList.map { item ->
                    if (item.aid != aid) return@map item
                    if (pages.size > 1) item.copy(ugcPages = pages) else item
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
                if (pages.size > 1) {
                    item.copy(
                        ugcPages = pages,
                    )
                } else {
                    item
                }
            }
        }
    }

    fun updateVideoList(videoListItem: List<VideoListItem>) {
        _videoList.update { videoListItem }

    fun clearVideoList() {
        _videoList.value = emptyList()
        // 清缓存
        loadedUgcPagesAid.clear()
        inFlightUgcPagesAid.clear()
    }

    fun updateHistory(progress: Int, lastPlayedCid: Long) {
        _history.update { History(progress = progress, lastPlayedCid = lastPlayedCid) }
    }
}