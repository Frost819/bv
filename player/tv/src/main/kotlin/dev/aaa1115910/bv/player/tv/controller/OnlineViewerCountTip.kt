package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * 在线观看人数显示组件
 *
 * @param modifier 修饰符
 * @param show 是否显示
 * @param count 在线人数
 */
@Composable
fun OnlineViewerCountTip(
    modifier: Modifier = Modifier,
    show: Boolean,
    count: String
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
                val color = Color.White.copy(alpha = 0.6f)

                Icon(
                    modifier = Modifier.scale(0.8f),
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = color
                )
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    text = "$count 人正在看",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
