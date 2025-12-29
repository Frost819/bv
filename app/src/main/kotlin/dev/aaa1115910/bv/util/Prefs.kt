@file:Suppress("SpellCheckingInspection", "UNCHECKED_CAST")

package dev.aaa1115910.bv.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.util.generateBuvid
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.controllers.DanmakuType
import dev.aaa1115910.bv.component.controllers.playermenu.PlaySpeedItem
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import dev.aaa1115910.bv.screen.settings.content.ActionAfterPlayItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Prefs {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val flowMap = ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>()

    /**
     * 基本类型委托 (String, Int, Boolean, Float, Long)
     */
    private fun <T> pref(key: Preferences.Key<T>, default: T): PrefDelegate<T, T> {
        return PrefDelegate(key, default, flowMap)
    }

    /**
     * 对象映射委托 (Enum, Date, Dp, etc.)
     * @param save 转换成基本类型存入 DataStore
     * @param restore 从 DataStore 的基本类型还原为对象
     */
    private fun <T, P> pref(
        key: Preferences.Key<P>,
        default: T,
        save: (T) -> P,
        restore: (P) -> T
    ): PrefDelegate<T, P> {
        return PrefDelegate(key, default, flowMap, save, restore)
    }

    // 基础类型
    var isLogin by pref(PrefKeys.prefIsLoginKey, false)
    var uid by pref(PrefKeys.prefUidKey, 0L)
    var sid by pref(PrefKeys.prefSidKey, "")
    var sessData by pref(PrefKeys.prefSessDataKey, "")
    var biliJct by pref(PrefKeys.prefBiliJctKey, "")
    var uidCkMd5 by pref(PrefKeys.prefUidCkMd5Key, "")

    // 复杂类型
    var tokenExpiredData by pref(
        PrefKeys.prefTokenExpiredDateKey,
        Date(0),
        save = { it.time },
        restore = { Date(it) }
    )

    var defaultQuality by pref(
        PrefKeys.prefDefaultQualityKey,
        Resolution.R1080P,
        save = { it.code },
        restore = { Resolution.fromCode(it) }
    )

    var defaultPlaySpeed by pref(
        PrefKeys.prefDefaultPlaySpeedKey,
        PlaySpeedItem.x1,
        save = { it.code },
        restore = { PlaySpeedItem.fromCode(it) }
    )

    var defaultAudio by pref(
        PrefKeys.prefDefaultAudioKey,
        Audio.A192K,
        save = { it.code },
        restore = { Audio.fromCode(it) }
    )

    var defaultDanmakuScale by pref(PrefKeys.prefDefaultDanmakuScaleKey, 1.75f)
    var defaultDanmakuOpacity by pref(PrefKeys.prefDefaultDanmakuOpacityKey, 0.7f)
    var defaultDanmakuEnabled by pref(PrefKeys.prefDefaultDanmakuEnabledKey, true)

    // 列表类型映射
    var defaultDanmakuTypes by pref(
        PrefKeys.prefDefaultDanmakuTypesKey,
        listOf(
            DanmakuType.All,
            DanmakuType.Rolling,
            DanmakuType.Top,
            DanmakuType.Bottom
        ),
        save = { list -> list.map { it.ordinal }.joinToString(",") },
        restore = { str ->
            if (str.isEmpty()) emptyList()
            else str.split(",")
                .mapNotNull { runCatching { DanmakuType.entries[it.toInt()] }.getOrNull() }
        }
    )

    var defaultDanmakuArea by pref(PrefKeys.prefDefaultDanmakuAreaKey, 0.5f)

    var defaultVideoCodec by pref(
        PrefKeys.prefDefaultVideoCodecKey,
        VideoCodec.AVC,
        save = { it.ordinal },
        restore = { VideoCodec.fromCode(it) }
    )

    var enableFirebaseCollection by pref(PrefKeys.prefEnabledFirebaseCollectionKey, false)
    var incognitoMode by pref(PrefKeys.prefIncognitoModeKey, false)

    // DP/SP 类型映射
    var defaultSubtitleFontSize by pref(
        PrefKeys.prefDefaultSubtitleFontSizeKey,
        24.sp,
        save = { it.value.roundToInt() },
        restore = { it.sp }
    )

    var defaultSubtitleBackgroundOpacity by pref(
        PrefKeys.prefDefaultSubtitleBackgroundOpacityKey,
        0.4f
    )

    var defaultSubtitleBottomPadding by pref(
        PrefKeys.prefDefaultSubtitleBottomPaddingKey,
        12.dp,
        save = { it.value.roundToInt() },
        restore = { it.dp }
    )

    var showFps by pref(PrefKeys.prefShowFpsKey, false)

    var buvid by pref(PrefKeys.prefBuvidKey, "")
    var buvid3 by pref(PrefKeys.prefBuvid3Key, "")

    var playerType by pref(
        PrefKeys.prefPlayerTypeKey,
        PlayerType.Media3,
        save = { it.ordinal },
        restore = { PlayerType.entries.getOrElse(it) { PlayerType.Media3 } }
    )

    // 暴露 Flow 给 Compose 使用的示例
//    val playerTypeFlow = flowMap[PrefKeys.prefPlayerTypeKey]!!.asStateFlow() // 需强转类型使用，或封装 helper

    var density by pref(
        PrefKeys.prefDensityKey,
        BVApp.context.resources.displayMetrics.widthPixels / 960f
    )
    val densityFlow = flowMap[PrefKeys.prefDensityKey]!!.asStateFlow() as StateFlow<Float>

    var firstHomeTopNavItem by pref(
        PrefKeys.prefFirstHomeTopNavItemKey,
        HomeTopNavItem.Dynamics,
        save = { it.code },
        restore = { HomeTopNavItem.fromCode(it) }
    )

    var showVideoInfo by pref(PrefKeys.prefShowVideoInfoKey, true)
    var showPersistentSeek by pref(PrefKeys.prefShowPersistentSeekKey, false)
    var showHotword by pref(PrefKeys.prefShowHotwordKey, true)
    var accessToken by pref(PrefKeys.prefAccessTokenKey, "")
    var refreshToken by pref(PrefKeys.prefRefreshTokenKey, "")

    var apiType by pref(
        PrefKeys.prefApiTypeKey,
        ApiType.Web,
        save = { it.ordinal },
        restore = { ApiType.entries.getOrElse(it) { ApiType.Web } }
    )

    var enableProxy by pref(PrefKeys.prefEnableProxyKey, false)
    var proxyHttpServer by pref(PrefKeys.prefProxyHttpServerKey, "")
    var proxyGRPCServer by pref(PrefKeys.prefProxyGRPCServerKey, "")
    var lastVersionCode by pref(PrefKeys.prefLastVersionCodeKey, 0)
    var preferOfficialCdn by pref(PrefKeys.prefPreferOfficialCdn, false)
    var defaultDanmakuMask by pref(PrefKeys.prefDefaultDanmakuMask, false)
    var enableFfmpegAudioRenderer by pref(PrefKeys.prefEnableFfmpegAudioRenderer, false)
    var enableSoftwareVideoDecoder by pref(PrefKeys.prefEnableSoftwareVideoDecoder, false)

    var actionAfterPlay by pref(
        PrefKeys.prefActionAfterPlayKey,
        ActionAfterPlayItems.PlayNext,
        save = { it.code },
        restore = { ActionAfterPlayItems.fromCode(it) }
    )

    /**
     * [必须调用] 在 Application onCreate 中调用此方法。
     * 作用：首先阻塞读取硬盘内DataStore到内存，用于其他模块初始化；
     * 再启动一个长连接监听 DataStore 变化，并自动同步到内存缓存。
     */
    fun init() {
        val initialPrefs = runBlocking {
            BVApp.dataStoreManager.dataStore.data.first()
        }
        updateMemoryCache(initialPrefs)
        checkAndInitBuvid(initialPrefs)

        scope.launch {
            BVApp.dataStoreManager.dataStore.data.collect { preferences ->
                updateMemoryCache(preferences)
            }
        }
    }

    private fun updateMemoryCache(preferences: Preferences) {
        flowMap.forEach { (key, flow) ->
            if (preferences.contains(key)) {
                val newValue = preferences[key]
                flow.value = newValue
            }
        }
    }

    private fun checkAndInitBuvid(prefs: Preferences) {
        if (!prefs.contains(PrefKeys.prefBuvidKey) || prefs[PrefKeys.prefBuvidKey].isNullOrEmpty()) {
            val randomBuvid = generateBuvid()
            buvid = randomBuvid
        }
        if (!prefs.contains(PrefKeys.prefBuvid3Key) || prefs[PrefKeys.prefBuvid3Key].isNullOrEmpty()) {
            val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
            buvid3 = randomBuvid3
        }
    }
}

/**
 * 核心委托类：
 * 1. 维护内存缓存 (via MutableStateFlow)
 * 2. Get: 直接读内存 (同步，无锁，极快)
 * 3. Set: 更新内存 + 异步写入 DataStore (不阻塞 UI)
 */
class PrefDelegate<T, P>(
    private val key: Preferences.Key<P>,
    private val defaultValue: T,
    map: ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>,
    private val save: (T) -> P = { it as P }, // 默认不转换
    private val restore: (P) -> T = { it as T } // 默认不转换
) : ReadWriteProperty<Any?, T> {

    // 初始化 Flow，存入 map 供 Prefs.init 统一更新
    private val _flow = MutableStateFlow<Any?>(save(defaultValue))

    init {
        map[key] = _flow
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // 从 StateFlow 读取当前的最新的原始值 (P)，然后还原为对象 (T)
        val rawValue = _flow.value as? P
        return if (rawValue != null) restore(rawValue) else defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val persistValue = save(value)

        // 1. 立即更新内存，UI 瞬间响应
        _flow.value = persistValue

        // 2. 异步持久化
        BVApp.dataStoreManager.run {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                editPreference(key, persistValue)
            }
        }
    }
}

private object PrefKeys {
    val prefIsLoginKey = booleanPreferencesKey("il")
    val prefUidKey = longPreferencesKey("uid")
    val prefSidKey = stringPreferencesKey("sid")
    val prefSessDataKey = stringPreferencesKey("sd")
    val prefBiliJctKey = stringPreferencesKey("bj")
    val prefUidCkMd5Key = stringPreferencesKey("ucm")
    val prefTokenExpiredDateKey = longPreferencesKey("ted")
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultAudioKey = intPreferencesKey("da")
    val prefDefaultPlaySpeedKey = intPreferencesKey("dps")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuEnabledKey = booleanPreferencesKey("dde")
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefEnabledFirebaseCollectionKey = booleanPreferencesKey("efc")
    val prefIncognitoModeKey = booleanPreferencesKey("im")
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")
    val prefShowFpsKey = booleanPreferencesKey("sf")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")
    val prefPlayerTypeKey = intPreferencesKey("pt")
    val prefDensityKey = floatPreferencesKey("density")
    val prefFirstHomeTopNavItemKey = intPreferencesKey("first_home_top_nav")
    val prefShowVideoInfoKey = booleanPreferencesKey("show_video_info")
    val prefShowPersistentSeekKey = booleanPreferencesKey("show_persistent_seek")
    val prefShowHotwordKey = booleanPreferencesKey("shw")
    val prefAccessTokenKey = stringPreferencesKey("access_token")
    val prefRefreshTokenKey = stringPreferencesKey("refresh_token")
    val prefApiTypeKey = intPreferencesKey("api_type")
    val prefEnableProxyKey = booleanPreferencesKey("enable_proxy")
    val prefProxyHttpServerKey = stringPreferencesKey("proxy_http_server")
    val prefProxyGRPCServerKey = stringPreferencesKey("proxy_grpc_server")
    val prefLastVersionCodeKey = intPreferencesKey("last_version_code")
    val prefPreferOfficialCdn = booleanPreferencesKey("prefer_official_cdn")
    val prefDefaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val prefEnableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")
    val prefEnableSoftwareVideoDecoder = booleanPreferencesKey("enable_software_video_decoder")
    val prefActionAfterPlayKey = intPreferencesKey("action_after_play")
}