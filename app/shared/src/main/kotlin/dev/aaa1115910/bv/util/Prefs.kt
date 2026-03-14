@file:Suppress("SpellCheckingInspection")

package dev.aaa1115910.bv.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.util.generateBuvid
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.entity.InterfaceMode
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.ThemeType
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.PortraitVideoFixMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.DefaultSubtitle
import dev.aaa1115910.bv.player.entity.PlayerLoadNextAction
import dev.aaa1115910.bv.player.entity.PlayerDefaultStartPosition
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt

object Prefs {
    private val dsm = BVApp.dataStoreManager
    val logger = KotlinLogging.logger { }

    var isLogin: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIsLoginRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIsLoginKey, value) }

    var uid: Long
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidKey, value) }

    var sid: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSidKey, value) }

    var sessData: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSessDataRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSessDataKey, value) }

    var biliJct: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefBiliJctRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBiliJctKey, value) }

    var uidCkMd5: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidCkMd5Request).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidCkMd5Key, value) }

    var tokenExpiredData: Date
        get() = Date(runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefTokenExpiredDateRequest).first()
        })
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefTokenExpiredDateKey, value.time)
        }

    var defaultQuality: Resolution
        get() = runBlocking {
            Resolution.fromCode(dsm.getPreferenceFlow(PrefKeys.prefDefaultQualityRequest).first())
                ?: Resolution.R1080P
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultQualityKey, value.code)
        }

    var defaultPlaySpeed: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultPlaySpeedRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultPlaySpeedKey, value) }

    var currentPlaySpeed: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefCurrentPlaySpeedRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefCurrentPlaySpeedKey, value) }

    var defaultAudio: Audio
        get() = runBlocking {
            Audio.fromCode(dsm.getPreferenceFlow(PrefKeys.prefDefaultAudioRequest).first())
                ?: Audio.A192K
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultAudioKey, value.code) }

    var defaultDanmakuSize: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuSizeRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuSizeKey, value) }

    var defaultDanmakuScale: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuScaleRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuScaleKey, value) }

    var defaultDanmakuTransparency: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTransparencyRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuTransparencyKey, value)
        }

    var defaultDanmakuOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuOpacityKey, value)
        }

    var defaultDanmakuEnabled: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuEnabledRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuEnabledKey, value)
        }

    var defaultDanmakuTypes: List<DanmakuType>
        get() = runBlocking {
            val danmakuTypeIdsString =
                dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTypesRequest).first()
            if (danmakuTypeIdsString == "") {
                emptyList()
            } else {
                danmakuTypeIdsString.split(",").map { DanmakuType.entries[it.toInt()] }
            }
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultDanmakuTypesKey,
                value.map { it.ordinal }.joinToString(",")
            )
        }

    var defaultDanmakuArea: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuAreaRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuAreaKey, value) }

    var defaultDanmakuRollingDurationFactor: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuRollingDurationFactorRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuRollingDurationFactorKey, value)
        }

    var defaultVideoCodec: dev.aaa1115910.bv.player.entity.VideoCodec
        get() = dev.aaa1115910.bv.player.entity.VideoCodec.Companion.fromCode(
            runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultVideoCodecRequest).first() }
        )
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultVideoCodecKey, value.ordinal)
        }

    var enableFirebaseCollection: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnabledFirebaseCollectionRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefEnabledFirebaseCollectionKey, value)
        }

    var incognitoMode: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIncognitoModeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIncognitoModeKey, value) }

    var defaultSubtitle: DefaultSubtitle
        get() = runBlocking {
            val intValue = dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleRequest).first()
            DefaultSubtitle.fromValue(intValue)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultSubtitleKey, value.value) }

    var defaultSubtitleFontSize: TextUnit
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleFontSizeRequest).first().sp
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleFontSizeKey, value.value.roundToInt())
        }

    var defaultSubtitleBackgroundOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBackgroundOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleBackgroundOpacityKey, value)
        }

    var defaultSubtitleBottomPadding: Dp
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBottomPaddingRequest).first().dp
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultSubtitleBottomPaddingKey, value.value.roundToInt()
            )
        }

    var showFps: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowFpsRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowFpsKey, value) }

    var buvid: String
        get() = runBlocking {
            val id = dsm.getPreferenceFlow(PrefKeys.prefBuvidRequest).first()
            if (id != "") {
                id
            } else {
                val randomBuvid = generateBuvid()
                buvid = randomBuvid
                randomBuvid
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvidKey, value) }

    var buvid3: String
        get() = runBlocking {
            var id = dsm.getPreferenceFlow(PrefKeys.prefBuvid3Request).first()
            if(!id.contains("infoc")){
                buvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
                id = buvid3
            }
            if (id != "") {
                id
            } else {
                //random buvid3
                val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
                buvid3 = randomBuvid3
                randomBuvid3
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvid3Key, value) }

    var playerType: PlayerType
        get() = runBlocking {
            runCatching {
                PlayerType.entries[dsm.getPreferenceFlow(PrefKeys.prefPlayerTypeRequest).first()]
            }.getOrDefault(PlayerType.Media3)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerTypeKey, value.ordinal) }

    val densityFlow: Flow<Float> get() = dsm.getPreferenceFlow(PrefKeys.prefDensityRequest)
    var density: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDensityRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDensityKey, value) }

    var updateAlpha: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAlphaRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAlphaKey, value) }

    var accessToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAccessTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAccessTokenKey, value) }

    var refreshToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefRefreshTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefRefreshTokenKey, value) }

    var apiType: ApiType
        get() = runBlocking {
            ApiType.entries[dsm.getPreferenceFlow(PrefKeys.prefApiTypeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefApiTypeKey, value.ordinal) }

    var enableProxy: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnabelProxyRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableProxyKey, value) }

    var proxyHttpServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyHttpServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyHttpServerKey, value) }

    var proxyGRPCServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyGRPCServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyGRPCServerKey, value) }

    var lastVersionCode: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefLastVersionCodeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefLastVersionCodeKey, value) }

    var showedRemoteControllerPanelDemo: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefShowedRemoteControllerPanelDemoRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefShowedRemoteControllerPanelDemoKey, value)
        }

    var preferOfficialCdn: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPreferOfficialCdnRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPreferOfficialCdn, value) }

    var defaultDanmakuMask: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuMaskRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuMask, value) }

    var enableFfmpegAudioRenderer: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnableFfmpegEndererRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefEnableFfmpegAudioRenderer,
                value
            )
        }

    var blacklistUser: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefBlacklistUserRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBlacklistUserKey, value) }

    var themeType: ThemeType
        get() = runBlocking {
            ThemeType.entries[dsm.getPreferenceFlow(PrefKeys.prefThemeTypeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefThemeTypeKey, value.ordinal) }

    val themeTypeFlow: Flow<ThemeType>
        get() = dsm.getPreferenceFlow(PrefKeys.prefThemeTypeRequest)
            .transform { ordinal -> emit(ThemeType.entries[ordinal]) }

    var interfaceMode: InterfaceMode
        get() = runBlocking {
            InterfaceMode.entries[dsm.getPreferenceFlow(PrefKeys.prefInterfaceModeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefInterfaceModeKey, value.ordinal) }

    var defaultPlayMode: PlayMode
        get() = runBlocking {
            PlayMode.entries[dsm.getPreferenceFlow(PrefKeys.prefPlayModeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayModeKey, value.ordinal) }

    var defaultHomeTab: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultHomeTabRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultHomeTabKey, value) }


    var portraitVideoFixMode: PortraitVideoFixMode
        get() = runBlocking {
            // 读取整型枚举值
            val intValue = dsm.getPreferenceFlow(PrefKeys.prefPortraitVideoFixModeRequest).first()
            PortraitVideoFixMode.fromValue(intValue)
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefPortraitVideoFixModeKey, value.value)
        }

    var playerShowDebugInfo: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerShowDebugInfoRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerShowDebugInfoKey, value) }

    var playerLoadNextAction: PlayerLoadNextAction
        get() = runBlocking {
            val intValue = dsm.getPreferenceFlow(PrefKeys.prefPlayerLoadNextActionRequest).first()
            PlayerLoadNextAction.fromValue(intValue)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerLoadNextActionKey, value.value) }

    var playerDefaultStartPosition: PlayerDefaultStartPosition
        get() = runBlocking {
            val intValue = dsm.getPreferenceFlow(PrefKeys.prefPlayerDefaultStartPositionRequest).first()
            PlayerDefaultStartPosition.fromValue(intValue)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerDefaultStartPositionKey, value.value) }

    var playerExitWhenAllIsPlayed: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerExitWhenAllIsPlayedRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerExitWhenAllIsPlayedKey, value) }

    var playerSeekForwardStep: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerSeekForwardStepRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerSeekForwardStepKey, value) }

    var playerSeekBackwardStep: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerSeekBackwardStepRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerSeekBackwardStepKey, value) }

    var playerShowBottomProgressBar: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerShowBottomProgressBarRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerShowBottomProgressBarKey, value) }

    var showUGCVideoInfo: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowUGCVideoInfoRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowUGCVideoInfoKey, value) }

    var isLoop: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIsLoopRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIsLoopKey, value) }

    var showDanmaku: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowDanmakuRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowDanmakuKey, value) }

    var showOnlineViewerCount: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowOnlineViewerCountRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowOnlineViewerCountKey, value) }

    val showOnlineViewerCountFlow: Flow<Int>
        get() = dsm.getPreferenceFlow(PrefKeys.prefShowOnlineViewerCountRequest)

    var showLiveViewerCountTip: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowLiveViewerCountTipRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowLiveViewerCountTipKey, value) }

    val showLiveViewerCountTipFlow: Flow<Int>
        get() = dsm.getPreferenceFlow(PrefKeys.prefShowLiveViewerCountTipRequest)

    // 首页导航项排序和隐藏状态
    val homeNavItemsOrderFlow: Flow<String>
        get() = dsm.getPreferenceFlow(PrefKeys.prefHomeNavItemsOrderRequest)

    var homeNavItemsOrder: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefHomeNavItemsOrderRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefHomeNavItemsOrderKey, value) }

    // UGC 顶部导航项排序和隐藏状态
    val ugcNavItemsOrderFlow: Flow<String>
        get() = dsm.getPreferenceFlow(PrefKeys.prefUgcNavItemsOrderRequest)

    var ugcNavItemsOrder: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUgcNavItemsOrderRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUgcNavItemsOrderKey, value) }

    // PGC 顶部导航项排序和隐藏状态
    val pgcNavItemsOrderFlow: Flow<String>
        get() = dsm.getPreferenceFlow(PrefKeys.prefPgcNavItemsOrderRequest)

    var pgcNavItemsOrder: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPgcNavItemsOrderRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPgcNavItemsOrderKey, value) }
    
    var enableTunneling: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnableTunnelingRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableTunneling, value) }

    var enableAsyncQueueing: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnableAsyncQueueingRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableAsyncQueueing, value) }
    
    var skipPgcIntroOutro: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSkipPgcIntroOutroRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSkipPgcIntroOutroKey, value) }

    var playerControllerButtonsOrder: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPlayerControllerButtonsOrderRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerControllerButtonsOrderKey, value) }

    var ugcVideoInfoHistoryCount: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUgcVideoInfoHistoryCountRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUgcVideoInfoHistoryCountKey, value) }

    var videoInfoHistoryIncludeFromPlayer: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefVideoInfoHistoryIncludeFromPlayerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefVideoInfoHistoryIncludeFromPlayerKey, value) }

    var ugcVideoPlayerHistoryCount: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUgcVideoPlayerHistoryCountRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUgcVideoPlayerHistoryCountKey, value) }
}

object PrefKeys {
    val prefIsLoginKey = booleanPreferencesKey("il")
    val prefUidKey = longPreferencesKey("uid")
    val prefSidKey = stringPreferencesKey("sid")
    val prefSessDataKey = stringPreferencesKey("sd")
    val prefBiliJctKey = stringPreferencesKey("bj")
    val prefUidCkMd5Key = stringPreferencesKey("ucm")
    val prefTokenExpiredDateKey = longPreferencesKey("ted")
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultAudioKey = intPreferencesKey("da")
    val prefDefaultPlaySpeedKey = floatPreferencesKey("dps")
    val prefCurrentPlaySpeedKey = floatPreferencesKey("cps")
    val prefDefaultDanmakuSizeKey = intPreferencesKey("dds")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuTransparencyKey = intPreferencesKey("ddt")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuEnabledKey = booleanPreferencesKey("dde")
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")
    val prefDefaultDanmakuRollingDurationFactorKey = floatPreferencesKey("ddrdf")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefEnabledFirebaseCollectionKey = booleanPreferencesKey("efc")
    val prefIncognitoModeKey = booleanPreferencesKey("im")
    val prefDefaultSubtitleKey = intPreferencesKey("default_subtitle")
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")
    val prefShowFpsKey = booleanPreferencesKey("sf")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")
    val prefPlayerTypeKey = intPreferencesKey("pt")
    val prefDensityKey = floatPreferencesKey("density")
    val prefAlphaKey = booleanPreferencesKey("alpha")
    val prefAccessTokenKey = stringPreferencesKey("access_token")
    val prefRefreshTokenKey = stringPreferencesKey("refresh_token")
    val prefApiTypeKey = intPreferencesKey("api_type")
    val prefEnableProxyKey = booleanPreferencesKey("enable_proxy")
    val prefProxyHttpServerKey = stringPreferencesKey("proxy_http_server")
    val prefProxyGRPCServerKey = stringPreferencesKey("proxy_grpc_server")
    val prefLastVersionCodeKey = intPreferencesKey("last_version_code")
    val prefShowedRemoteControllerPanelDemoKey = booleanPreferencesKey("showed_rcpd")
    val prefPreferOfficialCdn = booleanPreferencesKey("prefer_official_cdn")
    val prefDefaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val prefEnableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")
    val prefBlacklistUserKey = booleanPreferencesKey("blacklist_user")
    val prefThemeTypeKey = intPreferencesKey("theme_type")
    val prefInterfaceModeKey = intPreferencesKey("interface_mode")
    val prefPlayModeKey = intPreferencesKey("play_mode")
    val prefDefaultHomeTabKey = intPreferencesKey("default_home_tab")
    val prefPortraitVideoFixModeKey = intPreferencesKey("portrait_video_fix_mode")
    val prefPlayerShowDebugInfoKey = booleanPreferencesKey("player_show_debug_info")
    val prefPlayerExitWhenAllIsPlayedKey = booleanPreferencesKey("player_exit_when_all_is_played")
    val prefPlayerSeekForwardStepKey = intPreferencesKey("player_seek_forward_step")
    val prefPlayerSeekBackwardStepKey = intPreferencesKey("player_seek_backward_step")
    val prefPlayerShowBottomProgressBarKey = booleanPreferencesKey("player_show_bottom_progress_bar")
    val prefShowUGCVideoInfoKey = booleanPreferencesKey("pref_show_ugc_video_info")
    val prefIsLoopKey = booleanPreferencesKey("player_is_loop")
    val prefShowDanmakuKey = booleanPreferencesKey("player_show_danmaku")
    val prefPlayerLoadNextActionKey = intPreferencesKey("player_load_next_action")
    val prefPlayerDefaultStartPositionKey = intPreferencesKey("player_default_start_position")
    val prefShowOnlineViewerCountKey = intPreferencesKey("show_online_viewer_count")
    val prefShowLiveViewerCountTipKey = intPreferencesKey("show_live_viewer_count_tip")
    val prefHomeNavItemsOrderKey = stringPreferencesKey("home_nav_items_order")
    val prefUgcNavItemsOrderKey = stringPreferencesKey("ugc_nav_items_order")
    val prefPgcNavItemsOrderKey = stringPreferencesKey("pgc_nav_items_order")
    val prefEnableTunneling = booleanPreferencesKey("enable_tunneling")
    val prefEnableAsyncQueueing = booleanPreferencesKey("enable_async_queueing")
    val prefSkipPgcIntroOutroKey = booleanPreferencesKey("skip_pgc_intro_outro")
    val prefPlayerControllerButtonsOrderKey = stringPreferencesKey("player_controller_buttons_order")
    val prefUgcVideoInfoHistoryCountKey = intPreferencesKey("ugc_video_info_history_count")
    val prefVideoInfoHistoryIncludeFromPlayerKey = booleanPreferencesKey("video_info_history_include_from_player")
    val prefUgcVideoPlayerHistoryCountKey = intPreferencesKey("ugc_video_player_history_count")


    val prefIsLoginRequest = PreferenceRequest(prefIsLoginKey, false)
    val prefUidRequest = PreferenceRequest(prefUidKey, 0)
    val prefSidRequest = PreferenceRequest(prefSidKey, "")
    val prefSessDataRequest = PreferenceRequest(prefSessDataKey, "")
    val prefBiliJctRequest = PreferenceRequest(prefBiliJctKey, "")
    val prefUidCkMd5Request = PreferenceRequest(prefUidCkMd5Key, "")
    val prefTokenExpiredDateRequest = PreferenceRequest(prefTokenExpiredDateKey, 0)
    val prefDefaultPlaySpeedRequest = PreferenceRequest(prefDefaultPlaySpeedKey, 1f)
    val prefCurrentPlaySpeedRequest = PreferenceRequest(prefCurrentPlaySpeedKey, 1f)
    val prefDefaultQualityRequest = PreferenceRequest(prefDefaultQualityKey, Resolution.R1080P.code)
    val prefDefaultAudioRequest = PreferenceRequest(prefDefaultAudioKey, Audio.A192K.code)
    val prefDefaultDanmakuSizeRequest = PreferenceRequest(prefDefaultDanmakuSizeKey, 6)
    val prefDefaultDanmakuScaleRequest = PreferenceRequest(prefDefaultDanmakuScaleKey, 1.25f)
    val prefDefaultDanmakuTransparencyRequest =
        PreferenceRequest(prefDefaultDanmakuTransparencyKey, 0)
    val prefDefaultDanmakuOpacityRequest = PreferenceRequest(prefDefaultDanmakuOpacityKey, 0.8f)
    val prefDefaultDanmakuEnabledRequest = PreferenceRequest(prefDefaultDanmakuEnabledKey, true)
    val prefDefaultDanmakuTypesRequest =
        PreferenceRequest(prefDefaultDanmakuTypesKey, "0,1,2,3")
    val prefDefaultDanmakuAreaRequest = PreferenceRequest(prefDefaultDanmakuAreaKey, 0.2f)
    val prefDefaultDanmakuRollingDurationFactorRequest =
        PreferenceRequest(prefDefaultDanmakuRollingDurationFactorKey, 1f)
    val prefDefaultVideoCodecRequest =
        PreferenceRequest(prefDefaultVideoCodecKey, VideoCodec.HEVC.ordinal)
    val prefEnabledFirebaseCollectionRequest =
        PreferenceRequest(prefEnabledFirebaseCollectionKey, false)
    val prefIncognitoModeRequest = PreferenceRequest(prefIncognitoModeKey, false)
    val prefDefaultSubtitleRequest = PreferenceRequest(prefDefaultSubtitleKey, DefaultSubtitle.Off.value)
    val prefDefaultSubtitleFontSizeRequest = PreferenceRequest(prefDefaultSubtitleFontSizeKey, 24)
    val prefDefaultSubtitleBackgroundOpacityRequest =
        PreferenceRequest(prefDefaultSubtitleBackgroundOpacityKey, 0.4f)
    val prefDefaultSubtitleBottomPaddingRequest =
        PreferenceRequest(prefDefaultSubtitleBottomPaddingKey, 12)
    val prefShowFpsRequest = PreferenceRequest(prefShowFpsKey, false)
    val prefBuvidRequest = PreferenceRequest(prefBuvidKey, "")
    val prefBuvid3Request = PreferenceRequest(prefBuvid3Key, "")
    val prefPlayerTypeRequest = PreferenceRequest(prefPlayerTypeKey, PlayerType.Media3.ordinal)
    val prefDensityRequest =
        PreferenceRequest(
            prefDensityKey,
            runCatching { BVApp.context.resources.displayMetrics.widthPixels / 960f }
                .getOrDefault(2f)
        )

    @Suppress("KotlinConstantConditions")
    val prefAlphaRequest = PreferenceRequest(prefAlphaKey, BuildConfig.BUILD_TYPE == "alpha")
    val prefAccessTokenRequest = PreferenceRequest(prefAccessTokenKey, "")
    val prefRefreshTokenRequest = PreferenceRequest(prefRefreshTokenKey, "")
    val prefApiTypeRequest = PreferenceRequest(prefApiTypeKey, 0)
    val prefEnabelProxyRequest = PreferenceRequest(prefEnableProxyKey, false)
    val prefProxyHttpServerRequest = PreferenceRequest(prefProxyHttpServerKey, "")
    val prefProxyGRPCServerRequest = PreferenceRequest(prefProxyGRPCServerKey, "")
    val prefLastVersionCodeRequest = PreferenceRequest(prefLastVersionCodeKey, 0)
    val prefShowedRemoteControllerPanelDemoRequest =
        PreferenceRequest(prefShowedRemoteControllerPanelDemoKey, false)
    val prefPreferOfficialCdnRequest = PreferenceRequest(prefPreferOfficialCdn, false)
    val prefDefaultDanmakuMaskRequest = PreferenceRequest(prefDefaultDanmakuMask, false)
    val prefEnableFfmpegEndererRequest = PreferenceRequest(prefEnableFfmpegAudioRenderer, true)
    val prefBlacklistUserRequest = PreferenceRequest(prefBlacklistUserKey, false)
    val prefThemeTypeRequest = PreferenceRequest(prefThemeTypeKey, ThemeType.Auto.ordinal)
    val prefInterfaceModeRequest = PreferenceRequest(prefInterfaceModeKey, InterfaceMode.Auto.ordinal)
    val prefPlayModeRequest = PreferenceRequest(prefPlayModeKey, PlayMode.Sequential.ordinal)
    val prefDefaultHomeTabRequest = PreferenceRequest(prefDefaultHomeTabKey, 0)
    val prefPortraitVideoFixModeRequest = PreferenceRequest(prefPortraitVideoFixModeKey, 0)
    val prefPlayerShowDebugInfoRequest = PreferenceRequest(prefPlayerShowDebugInfoKey, true)
    val prefPlayerExitWhenAllIsPlayedRequest = PreferenceRequest(prefPlayerExitWhenAllIsPlayedKey, true)
    val prefPlayerSeekForwardStepRequest = PreferenceRequest(prefPlayerSeekForwardStepKey, 10)
    val prefPlayerSeekBackwardStepRequest = PreferenceRequest(prefPlayerSeekBackwardStepKey, 5)
    val prefPlayerShowBottomProgressBarRequest = PreferenceRequest(prefPlayerShowBottomProgressBarKey, false)
    val prefShowUGCVideoInfoRequest = PreferenceRequest(prefShowUGCVideoInfoKey, true)
    val prefIsLoopRequest = PreferenceRequest(prefIsLoopKey, false)
    val prefShowDanmakuRequest = PreferenceRequest(prefShowDanmakuKey, true)
    val prefPlayerLoadNextActionRequest = PreferenceRequest(prefPlayerLoadNextActionKey, PlayerLoadNextAction.DoNothing.value)
    val prefPlayerDefaultStartPositionRequest = PreferenceRequest(prefPlayerDefaultStartPositionKey, PlayerDefaultStartPosition.Beginning.value)
    val prefShowOnlineViewerCountRequest = PreferenceRequest(prefShowOnlineViewerCountKey, 2)  // 0 = 不显示, 1 = 30 秒后隐藏, 2 = 始终显示
    val prefShowLiveViewerCountTipRequest = PreferenceRequest(prefShowLiveViewerCountTipKey, 2)  // 0 = 不显示, 1 = 30 秒后隐藏, 2 = 始终显示
    // 默认留空：表示按枚举原始顺序全部显示（解析侧会处理 blank）
    val prefHomeNavItemsOrderRequest = PreferenceRequest(prefHomeNavItemsOrderKey, "")
    val prefUgcNavItemsOrderRequest = PreferenceRequest(prefUgcNavItemsOrderKey, "")
    val prefPgcNavItemsOrderRequest = PreferenceRequest(prefPgcNavItemsOrderKey, "")
    val prefEnableTunnelingRequest = PreferenceRequest(prefEnableTunneling, false)
    val prefEnableAsyncQueueingRequest = PreferenceRequest(prefEnableAsyncQueueing, false)
    val prefSkipPgcIntroOutroRequest = PreferenceRequest(prefSkipPgcIntroOutroKey, false)
    val prefPlayerControllerButtonsOrderRequest = PreferenceRequest(prefPlayerControllerButtonsOrderKey, "")
    val prefUgcVideoInfoHistoryCountRequest = PreferenceRequest(prefUgcVideoInfoHistoryCountKey, 2)
    val prefVideoInfoHistoryIncludeFromPlayerRequest = PreferenceRequest(prefVideoInfoHistoryIncludeFromPlayerKey, true)
    val prefUgcVideoPlayerHistoryCountRequest = PreferenceRequest(prefUgcVideoPlayerHistoryCountKey, 1)
}