package dev.aaa1115910.bv.player.seekbar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

@Composable
fun WavySeekBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    waving: Boolean = true,
    showThumb: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val trackWidth = 10f

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 / 0.3f).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    var pausedPhase by remember { mutableFloatStateOf(0f) }
    val phase by remember(waving) {
        pausedPhase = wavingPhase
        derivedStateOf {
            if (waving) {
                wavingPhase
            } else {
                pausedPhase
            }
        }
    }

    val ampPx by animateFloatAsState(
        targetValue = if (waving) 6f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "amplitude"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        inset(horizontal = 0f, vertical = 0f) {
            val shadowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                alpha = (0.3f * 255).toInt() // 设置透明度
                setShadowLayer(8f, 4f, 4f, android.graphics.Color.BLACK) // 模糊半径和偏移
            }

            // background track shadow
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawLine(
                    size.width * (position / duration.toFloat()),
                    center.y,
                    size.width,
                    center.y,
                    shadowPaint
                )
            }

            // background track
            drawLine(
                color = colors.inactiveTrackColor,
                start = Offset(size.width * (position / duration.toFloat()), center.y),
                end = Offset(size.width - 0f, center.y),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            // buffered track
            val bufferedTrackEnd = max(position / duration.toFloat(), bufferedPercentage / 100f)
            drawLine(
                color = colors.disabledActiveTrackColor,
                start = Offset(size.width * (position / duration.toFloat()), center.y),
                end = Offset(size.width * bufferedTrackEnd, center.y),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            // animated wave line shadow
            val waveLenPx = 80f
            val step = 2f
            val height = size.height / 2
            val shadowAmpPx = ampPx + 4f // 阴影的振幅稍大
            var prevShadowX = 0f
            var prevShadowY = height + shadowAmpPx * sin(phase)
            drawIntoCanvas { canvas ->
                for (x in step.toInt()..(size.width * (position / duration.toFloat())).toInt() step step.toInt()) {
                    val shadowY =
                        height + shadowAmpPx * sin(2 * PI * x / waveLenPx + phase).toFloat()
                    canvas.nativeCanvas.drawLine(
                        prevShadowX,
                        prevShadowY,
                        x.toFloat(),
                        shadowY,
                        shadowPaint
                    )
                    prevShadowX = x.toFloat()
                    prevShadowY = shadowY
                }
            }

            // animated wave line
            var prevX = 0f
            var prevY = height + ampPx * sin(phase)
            for (x in step.toInt()..(size.width * (position / duration.toFloat())).toInt() step step.toInt()) {
                val y = height + ampPx * sin(2 * PI * x / waveLenPx + phase).toFloat()
                drawLine(
                    color = colors.activeTrackColor,
                    start = Offset(prevX, prevY),
                    end = Offset(x.toFloat(), y),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
                prevX = x.toFloat()
                prevY = y
            }

            // thumb indicator
            if (showThumb) {
                val thumbX = size.width * (position / duration.toFloat())
                val thumbHeight = 40f
                val thumbWidth = 14f

                drawLine(
                    color = colors.activeTrackColor,
                    start = Offset(thumbX, (size.height - thumbHeight) / 2),
                    end = Offset(thumbX, (size.height + thumbHeight) / 2),
                    strokeWidth = thumbWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}


@Composable
fun SeekBar(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    colors: SliderColors = SliderDefaults.colors(),
    height: Dp = 10.dp,
    strokeWidth: Dp = height
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.roundToPx().toFloat() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        drawLine(
            color = colors.inactiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width - 0f, center.y),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.disabledInactiveTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width * bufferedPercentage / 100, center.y),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.activeTrackColor,
            start = Offset(0f, center.y),
            end = Offset(size.width * (position / duration.toFloat()), center.y),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
    }
}

@Preview
@Composable
private fun WavySeekPreview() {
    MaterialTheme {
        WavySeekBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            duration = 1000,
            position = 300,
            bufferedPercentage = 50
        )
    }
}

@Preview
@Composable
private fun SeekPreview() {
    MaterialTheme {
        SeekBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            duration = 1000,
            position = 300,
            bufferedPercentage = 50
        )
    }
}