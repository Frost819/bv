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
import org.koin.core.annotation.Single

@Single
class VideoInfoRepository(private val videoDetailRepository: VideoDetailRepository) {
    private val _videoList = MutableStateFlow<List<VideoListItem>>(emptyList())
    private val _videoDetailState = MutableStateFlow<VideoDetailState?>(null)

    val videoList = _videoList.asStateFlow()
    val videoDetailState = _videoDetailState.asStateFlow()

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
        coAuthors = videoDetail.coAuthors,
            )

        _videoDetailState.update { videoDetailState }
    }

    fun updateVideoList(videoListItem: List<VideoListItem>) {
        _videoList.update { videoListItem }
    }

    fun updateHistory(progress: Int, lastPlayedCid: Long) {
        _videoDetailState.update { it?.copy(lastPlayedCid = lastPlayedCid, lastPlayedTime = progress) }
    }

    fun reset(){
        _videoList.update { emptyList() }
        _videoDetailState.update { null }
    }

    private fun mapToVideoCardData(relatedVideos:List<RelatedVideo>): List<VideoCardData> {
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