package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.draw.scale

/**
 * 直播观看人气显示组件（左下角常驻）
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param popularityText 人气文本，如 "2.5万人气"
 * @param onlineCount 高能观众文本，如 "4333 高能观众"
 */
@Composable
fun LiveViewerCountTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    popularityText: String,
    onlineCount: String = ""
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contentColor = Color.White.copy(alpha = 0.6f)

                Icon(
                    modifier = Modifier.scale(0.8f),
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = contentColor
                )
                val displayText = buildString {
                    if (popularityText.isNotEmpty()) append(popularityText)
                    if (onlineCount.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(onlineCount)
                    }
                }
                if (displayText.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = displayText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = contentColor,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
