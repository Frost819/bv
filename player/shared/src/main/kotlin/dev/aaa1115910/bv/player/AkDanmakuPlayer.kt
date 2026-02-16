package dev.aaa1115910.bv.player

import android.graphics.Color
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView

@Composable
fun AkDanmakuPlayer(
    modifier: Modifier = Modifier,
    danmakuPlayer: DanmakuPlayer?
) {
    val context = LocalContext.current
    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }

    DisposableEffect(danmakuPlayer) {
        danmakuView?.let { view ->
            danmakuPlayer?.bindView(view)
        }

        onDispose {
            danmakuPlayer?.release()
        }
    }

//    // 调试重组次数: AtomicInteger，不被 Compose 追踪，只记录真实由外部状态引起的重组次数。
//    val recomposeCounter = remember { java.util.concurrent.atomic.AtomicInteger(0) }
//    SideEffect {
//        val value = recomposeCounter.incrementAndGet()
//        println("Recompose(DanmakuPlayer): $value")
//    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            danmakuView = DanmakuView(ctx).apply {
                // 确保View会被绘制
                setWillNotDraw(false)
                
                // 硬件加速层，减少弹幕绘制的 CPU 开销
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                // 兼容性优化：对于旧版本Android使用绘制缓存
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    isDrawingCacheEnabled = true
                }
            }
            danmakuView!!
        }
    )
}