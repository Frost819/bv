package dev.aaa1115910.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 跳过片头/片尾配置
 *
 * @param materialNo 素材编号
 * @param start DASH分段开始
 * @param end DASH分段结束
 * @param clipType Clip类型 (CLIP_TYPE_OP/CLIP_TYPE_ED/CLIP_TYPE_HE/CLIP_TYPE_MULTI_VIEW/CLIP_TYPE_AD)
 * @param toastText 跳过时的提示语
 * @param multiView 多视图信息
 */
@Serializable
data class ClipInfo(
    val materialNo: Long = 0L,
    val start: Int = 0, // 开始时间点，单位秒
    val end: Int = 0, // 结束时间点，单位秒
    val clipType: ClipType = ClipType.Unknown,
    val toastText: String = "",
    val multiView: MultiView? = null
)

/**
 * 跳过片头/片尾配置: Clip类型
 * JSON 中为字符串值: "CLIP_TYPE_OP", "CLIP_TYPE_ED" 等
 */
@Serializable
enum class ClipType {
    Unknown,
    CLIP_TYPE_OP,         // 跳过OP
    CLIP_TYPE_ED,         // 跳过ED
    CLIP_TYPE_HE,
    CLIP_TYPE_MULTI_VIEW,
    CLIP_TYPE_AD
}

/**
 * 多视图信息
 */
@Serializable
data class MultiView(
    val mainViewEndTime: Int? = null,
    val subViewStartTime: Int? = null
)

/**
 * @param code 作用尚不明确 pcg独有
 * @param isPreview 作用尚不明确 pcg独有
 * @param fnver 请求时提供的 fnver pcg独有
 * @param fnval 请求时提供的 fnval pcg独有
 * @param videoProject 作用尚不明确 pcg独有
 * @param type 视频流类型（DASH、FLV、MP4） pcg独有
 * @param bp 是否可以承包 pcg独有
 * @param vipType 当前用户大会员类型 pcg独有
 * @param vipStatus 当前用户大会员状态 pcg独有
 * @param isDrm 作用尚不明确 pcg独有
 * @param noRexcode 作用尚不明确 pcg独有
 * @param hasPaid 是否已购买 免费影片始终为 true pcg独有
 * @param status 作用尚不明确 pcg独有
 * @param from local 作用尚不明确
 * @param result suee 作用尚不明确
 * @param message 空 作用尚不明确
 * @param quality 当前的视频分辨率代码 值含义见上表
 * @param format 视频格式
 * @param timeLength 视频长度（毫秒值） 单位为毫秒 不同分辨率/格式可能有略微差异
 * @param acceptFormat 视频支持的全部格式 每项用,分隔
 * @param acceptDescription 视频支持的分辨率列表
 * @param acceptQuality 视频支持的分辨率代码列表 值含义见上表
 * @param videoCodecId 默认选择视频流的编码id 见视频编码代码
 * @param seekParam 固定值：start 作用尚不明确
 * @param seekType offset（dash、flv） second（mp4） 作用尚不明确
 * @param durl 视频分段 注：仅flv/mp4存在此项
 * @param dash dash音视频流信息 注：仅dash存在此项
 * @param supportFormats 支持格式的详细信息
 * @param high_format null
 * @param lastPlayTime 上次播放进度 毫秒值 非pgc接口独有
 * @param lastPlayCid 上次播放分p的cid 非pgc接口独有
 * @param clipInfoList 跳过片头/片尾配置 pcg独有
 * @param recordInfo 备案登记信息 pcg独有
 */
@Serializable
data class PlayUrlData(
    val code: Int = 0,
    @SerialName("is_preview")
    val isPreview: Int = 0,
    val fnver: Int = 0,
    val fnval: Int = 0,
    @SerialName("video_project")
    val videoProject: Boolean = false,
    val type: String = "",
    val bp: Int = 0,
    @SerialName("vip_type")
    val vipType: Int = 0,
    @SerialName("vip_status")
    val vipStatus: Int = 0,
    @SerialName("is_drm")
    val isDrm: Boolean = false,
    @SerialName("no_rexcode")
    val noRexcode: Int = 0,
    @SerialName("has_paid")
    val hasPaid: Boolean = false,
    val status: Int = 0,
    val from: String,
    val result: String,
    val message: String,
    val quality: Int,
    val format: String,
    @SerialName("timelength")
    val timeLength: Int,
    @SerialName("accept_format")
    val acceptFormat: String,
    @SerialName("accept_description")
    val acceptDescription: List<String> = emptyList(),
    @SerialName("accept_quality")
    val acceptQuality: List<Int> = emptyList(),
    @SerialName("video_codecid")
    val videoCodecId: Int,
    @SerialName("seek_param")
    val seekParam: String,
    @SerialName("seek_type")
    val seekType: String,
    val durl: List<Durl> = emptyList(),
    val dash: Dash? = null,
    @SerialName("support_formats")
    val supportFormats: List<SupportFormat> = emptyList(),
    @SerialName("last_play_time")
    val lastPlayTime: Int = 0,
    @SerialName("last_play_cid")
    val lastPlaycid: Long = 0,
    @SerialName("clip_info_list")
    val clipInfoList: List<ClipInfo> = emptyList(),
    @SerialName("record_info")
    val recordInfo: RecordInfo? = null
)

@Serializable
data class PlayUrlV2Data(
    @SerialName("exp_info")
    val expInfo: ExpInfo,
    @SerialName("play_check")
    val playCheck: PlayCheck,
    @SerialName("play_view_business_info")
    val playViewBusinessInfo: PlayViewBusinessInfo,
    @SerialName("video_info")
    val videoInfo: PlayUrlData,
    @SerialName("view_info")
    val viewInfo: ViewInfo
) {
    @Serializable
    data class ExpInfo(
        @SerialName("buy_vip_donated_season")
        val buyVipDonatedSeason: Int
    )

    @Serializable
    data class PlayCheck(
        @SerialName("play_detail")
        val playDetail: String
    )

    @Serializable
    data class PlayViewBusinessInfo(
        @SerialName("episode_info")
        val episodeInfo: EpisodeInfo,
        @SerialName("season_info")
        val seasonInfo: SeasonInfo,
        @SerialName("user_status")
        val userStatus: UserStatus
    ) {
        @Serializable
        data class EpisodeInfo(
            val aid: Long,
            val bvid: String,
            val cid: Long,
            @SerialName("delivery_business_fragment_video")
            val deliveryBusinessFragmentVideo: Boolean,
            @SerialName("delivery_fragment_video")
            val deliveryFragmentVideo: Boolean,
            @SerialName("ep_id")
            val epId: Int,
            @SerialName("ep_status")
            val epStatus: Int,
            val interaction: Interaction,
            @SerialName("long_title")
            val longTitle: String,
            val title: String
        ) {
            @Serializable
            data class Interaction(
                val interaction: Boolean
            )
        }

        @Serializable
        data class SeasonInfo(
            @SerialName("season_id")
            val seasonId: Int,
            @SerialName("season_type")
            val seasonType: Int,
        )

        @Serializable
        data class UserStatus(
            @SerialName("follow_info")
            val followInfo: FollowInfo,
            @SerialName("is_login")
            val isLogin: Int,
            @SerialName("pay_info")
            val payInfo: PayInfo,
            @SerialName("vip_info")
            val vipInfo: VipInfo,
            @SerialName("watch_progress")
            val watchProgress: WatchProgress
        ) {
            @Serializable
            data class FollowInfo(
                val follow: Int,
                @SerialName("follow_status")
                val followStatus: Int
            )

            @Serializable
            data class PayInfo(
                @SerialName("pay_check")
                val payCheck: Int,
                @SerialName("pay_pack_paid")
                val payPackPaid: Int,
                val sponsor: Int
            )

            @Serializable
            data class VipInfo(
                @SerialName("real_vip")
                val realVip: Boolean
            )

            @Serializable
            data class WatchProgress(
                @SerialName("current_watch_progress")
                val currentWatchProgress: Int,
                @SerialName("last_ep_id")
                val lastEpId: Int,
                @SerialName("last_time")
                val lastTime: Int
            )
        }
    }

    @Serializable
    data class ViewInfo(
        @SerialName("ai_repair_qn_trial_info")
        val aiRepairQnTrialInfo: AiRepairQnTrialInfo,
        @SerialName("end_page")
        val endPage: EndPage,
        @SerialName("ext_toast")
        val extToast: JsonElement,
        @SerialName("qn_trial_info")
        val qnTrialInfo: QnTrialInfo,
        val report: Report
    ) {
        @Serializable
        data class AiRepairQnTrialInfo(
            @SerialName("trial_able")
            val trialAble: Boolean
        )

        @Serializable
        data class EndPage(
            val hide: Boolean
        )

        @Serializable
        data class QnTrialInfo(
            @SerialName("trial_able")
            val trialAble: Boolean
        )

        @Serializable
        data class Report(
            @SerialName("ep_id")
            val epId: String,
            @SerialName("ep_status")
            val epStatus: String,
            @SerialName("season_id")
            val seasonId: String,
            @SerialName("season_status")
            val seasonStatus: String,
            @SerialName("season_type")
            val seasonType: String,
            @SerialName("vip_status")
            val vipStatus: String,
            @SerialName("vip_type")
            val vipType: String
        )
    }
}

/**
 * 视频播放地址
 *
 * @param order 视频分段序号 某些视频会分为多个片段（从1顺序增长）
 * @param length 视频长度 单位为毫秒
 * @param size 视频大小 单位为Byte
 * @param ahead 空 作用尚不明确
 * @param vhead 空 作用尚不明确
 * @param url 视频流url 注：url内容存在转义符 有效时间为120min
 * @param backupUrl 备用视频流
 */
@Serializable
data class Durl(
    val order: Int,
    val length: Int,
    val size: Int,
    val ahead: String,
    val vhead: String,
    val url: String,
    @SerialName("backup_url")
    val backupUrl: List<String> = emptyList()
)

//TODO
@Serializable
data class Dash(
    val duration: Int,
    val minBufferTime: Float,
    val video: List<DashData> = emptyList(),
    val audio: List<DashData>? = null,
    val dolby: DashDolby = DashDolby(),
    val flac: DashFlac? = null
)

@Serializable
data class DashDolby(
    val audio: List<DashData>? = null,
    val type: Int = 2
)

@Serializable
data class DashFlac(
    val display: Boolean,
    val audio: DashData? = null
)

@Serializable
data class DashData(
    val id: Int,
    @SerialName("base_url")
    val baseUrl: String,
    val backupUrl: List<String> = emptyList(),
    val bandwidth: Int,
    @SerialName("mime_type")
    val mimeType: String,
    val codecs: String,
    val width: Int,
    val height: Int,
    @SerialName("frame_rate")
    val frameRate: String,
    val sar: String,
    @SerialName("start_with_sap")
    val startWithSap: Int,
    @SerialName("segment_base")
    val segmentBase: SegmentBase,
    @SerialName("codecid")
    val codecId: Int
)

@Serializable
data class SegmentBase(
    val initialization: String,
    @SerialName("index_range")
    val indexRange: String
)

/**
 * 支持的视频格式
 *
 * @param quality 视频清晰度代码
 * @param format 视频格式
 * @param newDescription 格式描述
 * @param description 格式描述
 * @param displayDesc 格式描述
 * @param superScript (?)
 * @param codecs 可用编码格式列表
 * @param needLogin 需要登录
 * @param needVip 需要大会员
 */
@Serializable
data class SupportFormat(
    val quality: Int,
    val format: String,
    @SerialName("new_description")
    val newDescription: String,
    val description: String? = null,
    @SerialName("display_desc")
    val displayDesc: String,
    @SerialName("superscript")
    val superScript: String,
    val codecs: List<String>? = emptyList(),
    @SerialName("need_login")
    val needLogin: Boolean = false,
    @SerialName("need_vip")
    val needVip: Boolean = false
)

/**
 * 备案登记信息
 *
 * @param recordIcon
 * @param record 显示文案 登记号：10417060172092207
 */
@Serializable
data class RecordInfo(
    @SerialName("record_icon")
    val recordIcon: String,
    val record: String
)

@Serializable
enum class VideoQuality(val qn: Int, val displayName: String) {
    Q260P(6, "240P 极速"),
    Q360P(16, "360P 流畅"),
    Q480P(32, "480P 清晰"),
    Q720P(64, ""),
    Q720P60(74, ""),
    Q1080P(80, ""),
    Q1080PPlus(112, ""),
    Q1080P60(116, ""),
    Q4K(120, ""),
    HDR(125, ""),
    Dolby(126, ""),
    Q8K(127, "")
}
