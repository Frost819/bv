package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun VideoProgressSeek(
    modifier: Modifier = Modifier,
    duration: Long,
    position: Long,
    bufferedPercentage: Int,
    isPersistentSeek: Boolean
) {
    val colors: SliderColors = SliderDefaults.colors()
    val trackWidthDp = if (isPersistentSeek) 2.dp else 8.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackWidthDp)
    ) {
        val trackWidthPx = trackWidthDp.toPx()

        drawLine(
            color = colors.inactiveTrackColor,
            start = Offset(trackWidthPx / 2, center.y),
            end = Offset(size.width - trackWidthPx / 2, center.y),
            strokeWidth = trackWidthPx,
            cap = StrokeCap.Round
        )
        if (!isPersistentSeek) {
            drawLine(
                color = colors.disabledActiveTrackColor,
                start = Offset(trackWidthPx / 2, center.y),
                end = Offset(size.width * bufferedPercentage / 100, center.y),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = colors.activeTrackColor,
            start = Offset(trackWidthPx / 2, center.y),
            end = Offset(size.width * (position / duration.toFloat()), center.y),
            strokeWidth = trackWidthPx,
            cap = StrokeCap.Round
        )
    }

}


@Preview(device = "id:tv_1080p")
@Composable
private fun SeekPreview() {
    BVTheme {
        VideoProgressSeek(
            duration = 1000,
            position = 300,
            bufferedPercentage = 50,
            isPersistentSeek = true
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SeekWithThumbPreview(@PreviewParameter(ProgressProvider::class) data: Triple<Long, Long, Int>) {
    BVTheme {
        VideoProgressSeek(
            duration = data.first,
            position = data.second,
            bufferedPercentage = data.third,
            isPersistentSeek = false
        )
    }
}

private class ProgressProvider : PreviewParameterProvider<Triple<Long, Long, Int>> {
    override val values = sequenceOf(
        Triple(1234_000L, 0L, 3),
        Triple(1234_000L, 234_000L, 24),
        Triple(1234_000L, 555_000L, 57),
        Triple(1234_000L, 1234_000L, 100)
    )
}