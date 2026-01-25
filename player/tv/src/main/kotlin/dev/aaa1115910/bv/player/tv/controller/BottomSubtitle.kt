package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekState

@Composable
fun BottomSubtitle(
    modifier: Modifier = Modifier
) {
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerSeekState = LocalVideoPlayerSeekState.current
    val subtitleData = videoPlayerConfigData.currentSubtitleData
    val time = videoPlayerSeekState.position

    var currentText by remember { mutableStateOf("") }
    var isAI by remember { mutableStateOf(false) }

    val updateCurrentText: () -> Unit = {
        runCatching {
            val currentItem = subtitleData.find { it.isShowing(time) }
            currentText = currentItem?.content ?: ""
            isAI = currentItem?.isAI ?: false
        }
    }

    LaunchedEffect(time) {
        updateCurrentText()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (currentText != "") {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = videoPlayerConfigData.currentSubtitleBottomPadding)
            ) {
                Text(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black.copy(alpha = videoPlayerConfigData.currentSubtitleBackgroundOpacity))
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    text = currentText,
                    fontSize = videoPlayerConfigData.currentSubtitleFontSize,
                    textAlign = TextAlign.Center
                )
                if (isAI) {
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .alpha(0.5f),
                        text = "AI",
                        fontSize = (videoPlayerConfigData.currentSubtitleFontSize.value / 3).sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}