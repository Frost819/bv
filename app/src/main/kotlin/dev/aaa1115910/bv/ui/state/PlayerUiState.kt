package dev.aaa1115910.bv.ui.state

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.akdanmaku.data.DanmakuItemData
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bilisubtitle.entity.SubtitleItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.VideoAspectRatio
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.entity.VideoListItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.VideoShotImageCache
import dev.aaa1115910.biliapi.entity.video.season.UgcSeason

// 1. 核心 UI 状态 (低频更新)
data class PlayerUiState(
    // 视频信息
    val aid: Long = 0,
    val cid: Long = 0,
    val epid: Int? = null,
    val seasonId: Int = 0,
    val authorMid: Long = 0,
    val authorName: String = "",
    val title: String = "",
    val videoHeight: Int = 0,
    val videoWidth: Int = 0,
    val needPay: Boolean = false,
    val lastPlayed: Int = 0,
    val fromSeason: Boolean = false,
    val proxyArea: ProxyArea = ProxyArea.MainLand,
    val subType: Int = 0,

    // 播放状态
    val playerState: PlayerState = PlayerState.Ready,
    val isBuffering: Boolean = false, // 缓冲和暂停会同时出现，单独列出
    // 进度条缩略图及其缓存
    val videoShot: VideoShot? = null,
    val videoShotCache: VideoShotImageCache = VideoShotImageCache(),
    // 播放器时钟
    val clock: Pair<Int, Int> = Pair(0, 0),

    // 显示tip
    val showSkipToNextEp: Boolean = false,
    val showBackToStart: Boolean = false,

    // 播放器配置与资源
    val availableQuality: Map<Int, String> = emptyMap(),
    val availableVideoCodec: List<VideoCodec> = emptyList(),
    val availableAudio: List<Audio> = emptyList(),
    val availableSubtitles: List<Subtitle> = emptyList(),
    val availableVideoList: List<VideoListItem> = emptyList(),
    // 章节（随 cid 变化加载，用于“选择章节”）
    val availableChapters: List<PlayerChapter> = emptyList(),
    // UGC 合集（用于“选择合集”）
    val ugcSeason: UgcSeason? = null,

    // 相关视频
    val relatedVideos: List<VideoCardData> = emptyList(),

    // ==== 当前选中状态 ====

    // 媒体格式
    val mediaProfileState: MediaProfileState = MediaProfileState(),

    // 播放速度和宽高比
    val playSpeed: Float = 1f,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.Default,

    // 弹幕状态
    val danmakuState: DanmakuState = DanmakuState(),
    val danmakuMasks: List<DanmakuMaskSegment> = emptyList(),

    // 字幕状态
    val subtitleState: SubtitleState = SubtitleState(),
    val subtitleId: Long = -1L,
    val subtitleData: List<SubtitleItem> = emptyList(),
)

// 2. 播放器进度条状态 (高频更新)
data class SeekerState(
    val totalDuration: Long = 0,
    val currentTime: Long = 0,
    val bufferedPercentage: Int = 0,
    val debugInfo: String = ""
)

data class DanmakuState(
    val scale: Float = 0f,
    val opacity: Float = 0f,
    val area: Float = 0f,
    val speedFactor: Float = 1f,
    val maskEnabled: Boolean = false,
    val enabledTypes: List<DanmakuType> = emptyList(),
)

data class SubtitleState(
    val fontSize: TextUnit = 0.sp,
    val opacity: Float = 0f,
    val bottomPadding: Dp = 0.dp
)

data class MediaProfileState(
    val qualityId: Int = 0,
    val videoCodec: VideoCodec = VideoCodec.AVC,
    val audio: Audio = Audio.A192K
)

data class PlayerChapter(
    val content: String,
    val fromSec: Int,
    val toSec: Int
)

sealed class PlayerState {
    data object Ready: PlayerState()
    data object Playing: PlayerState()
    data object Paused: PlayerState()
    data object Ended: PlayerState()
    data class Error(val message: String): PlayerState()
}