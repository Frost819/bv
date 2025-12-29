package dev.aaa1115910.biliapi.repositories

import bilibili.app.view.v1.ViewGrpcKt
import bilibili.app.view.v1.viewReq
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.video.VideoDetail
import dev.aaa1115910.biliapi.entity.video.VideoPage
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.grpc.utils.handleGrpcException
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository
) {
    private val viewStub
        get() = runCatching {
            ViewGrpcKt.ViewCoroutineStub(channelRepository.defaultChannel!!)
        }.getOrNull()

    suspend fun getVideoDetail(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ): VideoDetail {
        return when (preferApiType) {
            ApiType.Web -> {
                withContext(Dispatchers.IO) {
                    val videoDetailWithoutUserActions = async {
                        val httpVideoDetail = BiliHttpApi.getVideoDetail(
                            av = aid,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        VideoDetail.fromVideoDetail(httpVideoDetail)
                    }

                    //check liked, favoured, coined status...
                    val isFavoured = async {
                        runCatching {
                            favoriteRepository.checkVideoFavoured(
                                aid = aid,
                                preferApiType = ApiType.Web
                            )
                        }.onFailure {
                            println("Check video favoured failed: $it")
                        }.getOrDefault(false)
                    }

                    val isLiked = async {
                        runCatching {
                            likeRepository.checkVideoLiked(
                                aid = aid,
                            )
                        }.onFailure {
                            println("Check video liked failed: $it")
                        }.getOrDefault(false)
                    }

                    val isCoined = async {
                        runCatching {
                            coinRepository.checkVideoCoined(
                                aid = aid,
                            )
                        }.onFailure {
                            println("Check video coined failed: $it")
                        }.getOrDefault(false)
                    }


                    val historyAndPlayerIcon = async {
                        runCatching {
                            val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                                avid = aid,
                                cid = videoDetailWithoutUserActions.await().cid,
                                sessData = authRepository.sessionData ?: "",
                                buvid3 = authRepository.buvid3 ?: ""
                            ).getResponseData()
                            val history = VideoDetail.History(
                                progress = videoModeInfo.lastPlayTime / 1000,
                                lastPlayedCid = videoModeInfo.lastPlayCid
                            )
                            history
                        }.onFailure {
                            println("Get video history failed: $it")
                        }.getOrDefault(VideoDetail.History(0, 0))
                    }

                    videoDetailWithoutUserActions.await().let { detail ->
                        val newUserActions = detail.userActions.copy(
                            favorite = isFavoured.await(),
                            like = isLiked.await(),
                            coin = isCoined.await()
                        )
                        val newHistory = historyAndPlayerIcon.await()
                        detail.copy(
                            userActions = newUserActions,
                            history = newHistory
                        )
                    }
                }
            }

            ApiType.App -> {
                val viewReply = runCatching {
                    viewStub?.view(viewReq {
                        this.aid = aid.toLong()
                    }) ?: throw IllegalStateException("Player stub is not initialized")
                }.onFailure { handleGrpcException(it) }.getOrThrow()
                VideoDetail.fromViewReply(viewReply)
            }
        }
    }

    suspend fun getUgcPages(
        aid: Long,
        preferApiType: ApiType = ApiType.Web
    ): List<VideoPage> {
        return try {
            when (preferApiType) {
                ApiType.Web -> {
                    val detail = BiliHttpApi.getVideoInfo(
                        av = aid,
                        sessData = authRepository.sessionData ?: ""
                    ).getResponseData()
                    detail.pages.map { VideoPage.fromVideoPage(it) }
                }

                ApiType.App -> {
                    val viewReply = runCatching {
                        viewStub?.view(viewReq {
                            this.aid = aid
                        }) ?: throw IllegalStateException("Player stub is not initialized")
                    }.onFailure { handleGrpcException(it) }
                        .getOrThrow()

                    viewReply.pagesList.map { VideoPage.fromViewPage(it) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            println("Get ugc pages failed: aid=$aid, preferApiType=$preferApiType, error=${e.stackTraceToString()}")
            emptyList()
        }
    }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null,
        preferApiType: ApiType = ApiType.Web
    ): SeasonDetail {
        when (preferApiType) {
            ApiType.Web -> {
                val webSeasonData = BiliHttpApi.getWebSeasonInfo(
                    epId = epid,
                    seasonId = seasonId,
                    sessData = authRepository.sessionData ?: ""
                ).getResponseData()
                val seasonDetail = SeasonDetail.fromSeasonData(webSeasonData)
                val firstEp = webSeasonData.episodes.firstOrNull() ?: return seasonDetail

                val playerIcon = runCatching {
                    val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                        avid = firstEp.aid,
                        cid = firstEp.cid,
                        sessData = authRepository.sessionData ?: "",
                        buvid3 = authRepository.buvid3 ?: ""
                    ).getResponseData()
                    val playerIcon = VideoDetail.PlayerIcon.fromPlayerIcon(videoModeInfo.playerIcon)
                    playerIcon
                }.onFailure {
                    println("Get video player icon failed: $it")
                }.getOrDefault(null)
                seasonDetail.playerIcon = playerIcon
                return seasonDetail
            }

            ApiType.App -> {
                val appSeasonData = BiliHttpApi.getAppSeasonInfo(
                    epId = epid,
                    seasonId = seasonId,
                    mobiApp = "android_hd",
                    accessKey = authRepository.accessToken ?: ""
                ).getResponseData()
                return SeasonDetail.fromSeasonData(appSeasonData)
            }
        }
    }
}