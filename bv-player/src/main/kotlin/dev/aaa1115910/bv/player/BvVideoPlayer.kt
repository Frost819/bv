package dev.aaa1115910.bv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer

@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
) {
    when (videoPlayer) {
        is ExoMediaPlayer -> {
            var playerView: PlayerView? by remember { mutableStateOf(null) }
            val lifecycleOwner = LocalLifecycleOwner.current

            // UI 离开时只解绑 View <-> ExoPlayer，不要 release 播放器实例（否则 ViewModel 续播会复用到已销毁实例）
            // 尽量在 onPause 就先解绑 surface，保证早于 Activity.onStop 的 release
            DisposableEffect(lifecycleOwner, videoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE,
                        Lifecycle.Event.ON_STOP -> {
                            runCatching { playerView?.player = null }
                        }

                        Lifecycle.Event.ON_START,
                        Lifecycle.Event.ON_RESUME -> {
                            // 回场时强制把 PlayerView 重新指向新 mPlayer
                            val target = videoPlayer.mPlayer
                            if (playerView?.player !== target) {
                                runCatching { playerView?.player = target }
                            }
                        }

                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    runCatching { playerView?.player = null }
                    playerView = null
                }
            }

            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer.mPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        useController = false
                        playerView = this
                    }
                },
                update = { view ->
                    // 当 ViewModel 里 initPlayer() 重建了新的 mPlayer 后，这里必须重新绑定
                    if (view.player !== videoPlayer.mPlayer) {
                        view.player = videoPlayer.mPlayer
                    }
                }
            )
        }
    }
}