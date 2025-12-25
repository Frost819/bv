package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text


// TODO 跳转历史记录 done
@Composable
fun BackToStartTip(
    modifier: Modifier = Modifier,
    show: Boolean,
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = "跳转到上次播放位置，按确认键从头播放"
    )
}


// TODO 跳过片头
@Composable
fun SkipOpTip(
    modifier: Modifier = Modifier,
    show: Boolean
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = "跳过片头"
    )
}

// TODO 跳过片尾
@Composable
fun SkipEdTip(
    modifier: Modifier = Modifier,
    show: Boolean
) {
    SkipTip(
        modifier = modifier,
        show = show,
        text = "跳过片尾"
    )
}

//播放结束跳到下一集
@Composable
fun SkipToNextEpTip(
    modifier: Modifier = Modifier,
    show: Boolean
){
    SkipTip(
        modifier = modifier,
        show = show,
        text = "播放结束，即将播放下一集"
    )
}

@Composable
fun SkipTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    text: String
) {
    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Surface(
                modifier = modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 32.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = MaterialTheme.shapes.medium.copy(
                    topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp)
                )
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = text,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun SkipTips(
    modifier: Modifier = Modifier,
    historyTime: Long,
    showBackToStart: Boolean,
    showSkipOp: Boolean = false,
    showSkipEd: Boolean = false,
    showSkipToNextEp: Boolean
) {
    Box(modifier = modifier.fillMaxSize()) {
        BackToStartTip(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp),
            show = showBackToStart,
        )
        SkipToNextEpTip(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 64.dp),
            show = showSkipToNextEp,
        )
    }
}