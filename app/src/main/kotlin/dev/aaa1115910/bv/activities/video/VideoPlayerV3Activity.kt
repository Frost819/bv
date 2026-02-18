package dev.aaa1115910.bv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.Author
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.impl.exo.ExoPlayerFactory
import dev.aaa1115910.bv.screen.VideoPlayerV3Screen
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.viewmodel.player.VideoPlayerV3ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoPlayerV3Activity : ComponentActivity() {
    companion object {
        private val logger = KotlinLogging.logger { }
        private var currentInstance: VideoPlayerV3Activity? = null
        fun actionStart(
            context: Context,
            avid: Long,
            cid: Long,
            title: String,
            partTitle: String,
            played: Int,
            fromSeason: Boolean,
            subType: Int? = null,
            epid: Int? = null,
            seasonId: Int? = null,
            proxyArea: ProxyArea = ProxyArea.MainLand,
            author: Author? = null
        ) {
            currentInstance?.finish()
            context.startActivity(
                Intent(context, VideoPlayerV3Activity::class.java).apply {
                    putExtra("avid", avid)
                    putExtra("cid", cid)
                    putExtra("title", title)
                    putExtra("partTitle", partTitle)
                    putExtra("played", played)
                    putExtra("fromSeason", fromSeason)
                    putExtra("subType", subType)
                    putExtra("epid", epid)
                    putExtra("seasonId", seasonId)
                    putExtra("proxy_area", proxyArea.ordinal)
                    putExtra("author_mid", author?.mid)
                    putExtra("author_name", author?.name)
                }
            )
        }
    }

    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = this  // 记录当前实例
        initVideoPlayer()
        //initDanmakuPlayer()
        getParamsFromIntent()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            BVTheme {
                VideoPlayerV3Screen()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // 回到前台：先进入抑制期，避免 attach surface 阶段闪抽风
        playerViewModel.setSuppressPlayerErrors(true)
        Log.i("BugDebug", "VideoPlayerV3Activity onStart: suppressPlayerErrors=true (resuming)")

        // 快恢复（不重建）或按需重建
        playerViewModel.onHostStartFastResumeOrRecreate()

        lifecycleScope.launch {
            delay(500)
            // 只有当页面还在前台时才恢复
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                // 退出抑制期
                playerViewModel.setSuppressPlayerErrors(false)
                Log.i("BugDebug", "VideoPlayerV3Activity onStart: suppressPlayerErrors=false")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 视频全屏播放，隐藏状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance === this) {
            currentInstance = null
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isFinishing) {
            playerViewModel.dettachPlayer()
            playerViewModel.releaseDanmakuPlayer()
        }
    }

    override fun onPause() {
        super.onPause()

        // 进入后台/跳转别的 Activity 前：抑制 stop/surfaceDestroyed 期间的 surface-detach 类错误进入 UI
        playerViewModel.setSuppressPlayerErrors(true)
        Log.i("BugDebug", "VideoPlayerV3Activity onPause: suppressPlayerErrors=true")

        // 恢复状态栏
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())

        playerViewModel.videoPlayer?.pause()
        playerViewModel.danmakuPlayer?.pause()
    }

    override fun onStop() {
        // 尽量早做，避免 stop 阶段 surfaceDestroyed 触发后再处理
        Log.i("BugDebug", "VideoPlayerV3Activity onStop: isFinishing=$isFinishing")

        if (!isFinishing) {
            playerViewModel.onHostStopFastResume()
        } else {
            // 退出 Activity 时也抑制错误，避免“退出前闪抽风”
            playerViewModel.setSuppressPlayerErrors(true)
        }

        super.onStop()
    }

    private fun initVideoPlayer() {
        logger.info { "Init video player: ${Prefs.playerType.name}" }
        val options = VideoPlayerOptions(
            userAgent = when (Prefs.apiType) {
                ApiType.Web -> getString(R.string.video_player_user_agent_http)
                ApiType.App -> getString(R.string.video_player_user_agent_client)
            },
            referer = when (Prefs.apiType) {
                ApiType.Web -> getString(R.string.video_player_referer)
                ApiType.App -> null
            },
            enableFfmpegAudioRenderer = Prefs.enableFfmpegAudioRenderer,
            enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder
        )
        val videoPlayer = when (Prefs.playerType) {
            PlayerType.Media3 -> ExoPlayerFactory().create(this.applicationContext, options)
        }
        playerViewModel.attachPlayer(videoPlayer)
        playerViewModel.initDanmakuPlayer()
    }

    /*private fun initDanmakuPlayer() {
        logger.info { "Init danamku player" }
        runBlocking { playerViewModel.initDanmakuPlayer() }
    }*/

    private fun getParamsFromIntent() {
        if (intent.hasExtra("avid")) {
            val aid = intent.getLongExtra("avid", 170001)
            val cid = intent.getLongExtra("cid", 170001)
            val title = intent.getStringExtra("title") ?: "Unknown Title"
            val played = intent.getIntExtra("played", 0)
            val fromSeason = intent.getBooleanExtra("fromSeason", false)
            val subType = intent.getIntExtra("subType", 0)
            val epid = intent.getIntExtra("epid", 0)
            val seasonId = intent.getIntExtra("seasonId", 0)
            val proxyArea = ProxyArea.entries[intent.getIntExtra("proxy_area", 0)]
            val author_mid = intent.getLongExtra("author_mid", 0)
            val author_name = intent.getStringExtra("author_name")
            logger.fInfo { "Launch parameter: [aid=$aid, cid=$cid]" }
            playerViewModel.init(
                aid = aid,
                cid = cid,
                epid = epid.takeIf { it != 0 },
                title = title,
                lastPlayed = played,
                fromSeason = fromSeason,
                subType = subType,
                seasonId = seasonId,
                proxyArea = proxyArea,
                authorMid = author_mid,
                authorName = author_name ?: ""
            )
        } else {
            logger.fInfo { "Null launch parameter" }
        }
    }
}
