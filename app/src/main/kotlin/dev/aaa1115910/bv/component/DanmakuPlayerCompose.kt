package dev.aaa1115910.bv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kuaishou.akdanmaku.ui.DanmakuView

@Composable
fun DanmakuPlayerCompose(
    modifier: Modifier = Modifier,
    onViewCreated: (DanmakuView) -> Unit,
    onViewDisposed: (DanmakuView) -> Unit,
) {
    val context = LocalContext.current
    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }

    val view = danmakuView
    DisposableEffect(view) {
        if (view == null) return@DisposableEffect onDispose { }
        onDispose { onViewDisposed(view) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val v = DanmakuView(context)
                danmakuView = v
                onViewCreated(v)
                v
            }
        )
    }
}
