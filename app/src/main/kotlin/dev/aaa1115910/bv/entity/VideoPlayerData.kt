package dev.aaa1115910.bv.entity

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.biliapi.http.entity.video.VideoMoreInfo
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem

data class VideoPlayerControllerData(
    val debugInfo: String = "",
    val infoData: VideoPlayerInfoData = VideoPlayerInfoData(0, 0, 0, 0, 0, ""),
    val resolutionMap: Map<Int, String> = emptyMap(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    val availableSubtitle: List<VideoMoreInfo.SubtitleItem> = emptyList(),
    val availableSubtitleTracks: List<Subtitle> = emptyList(),
    val availableVideoList: List<VideoListItem> = emptyList(),
    val currentVideoCid: Long = 0,
    val currentResolution: Int? = null,
    val currentVideoCodec: VideoCodec = VideoCodec.AVC,
    val currentVideoAspectRatio: VideoAspectRatio = VideoAspectRatio.Default,
    val currentVideoSpeed: Float = 1f,
    val currentSelectedPlaySpeedItem: PlaySpeedItem = PlaySpeedItem.x1,
    val currentAudio: Audio = Audio.A192K,
    val currentDanmakuEnabled: Boolean = true,
    val currentDanmakuEnabledList: List<DanmakuType> = listOf(),
    val currentDanmakuSize: DanmakuSize = DanmakuSize.S2,
    val currentDanmakuScale: Float = 1f,
    val currentDanmakuTransparency: DanmakuTransparency = DanmakuTransparency.T1,
    val currentDanmakuOpacity: Float = 1f,
    val currentDanmakuArea: Float = 1f,
    val currentDanmakuMask: Boolean = false,
    val currentSubtitleId: Long = 0,
    val currentSubtitleData: List<SubtitleItem> = emptyList(),
    val currentPosition: Long = 0,
    val currentSubtitleFontSize: TextUnit = 24.sp,
    val currentSubtitleBackgroundOpacity: Float = 0.4f,
    val currentSubtitleBottomPadding: Dp = 12.dp,
    val lastPlayed: Int = 0,
    val title: String = "Title",
    val secondTitle: String = "Second title",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isError: Boolean = false,
    val exception: Exception? = null,
    val clock: Triple<Int, Int, Int> = Triple(0, 0, 0),
    val showBackToStart: Boolean = false,
    val showSkipToNextEp: Boolean = false,
    val needPay: Boolean = false,
    val epid: Int = 0,
    val videoShot: VideoShot? = null
)

data class VideoPlayerInfoData(
    val totalDuration: Long,
    var currentTime: Long,
    val bufferedPercentage: Int,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val codec: String
)

val LocalVideoPlayerControllerData = compositionLocalOf { VideoPlayerControllerData() }