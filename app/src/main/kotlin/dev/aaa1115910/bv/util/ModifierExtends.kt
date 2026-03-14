package dev.aaa1115910.bv.util

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.border
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame

/**
 * 获取到焦点时显示白色边框
 */
fun Modifier.focusedBorder(
    shape: Shape = ShapeDefaults.Large,
    animate: Boolean = false
): Modifier = composed {
    var hasFocus by remember { mutableStateOf(false) }
    val pink = Color(0xFFFF69B4)
    val borderColor = if (hasFocus) pink else Color.Transparent

    onFocusChanged { hasFocus = it.hasFocus }
        .border(
            width = 3.dp,
            color = borderColor,
            shape = shape
        )
}

/**
 * 在没有获取到焦点的时候缩小，以便在获取到焦点的时候”放大”
 * 已禁用缩放动画以提升性能，仅保留接口兼容
 */
fun Modifier.focusedScale(
    scale: Float = 0.9f
): Modifier = this

fun Modifier.bitmapMask(
    bitmap: Bitmap,
    videoAspectRatio: Float, // 视频的宽高比 (例如 1920/1080 ≈ 1.77, 21/9 ≈ 2.33)
    areaRatio: Float         // 弹幕区域占屏幕高度的比例 (0.0 - 1.0)
): Modifier = composed {
    val imageBitmap = bitmap.asImageBitmap()

    drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            drawContent()

            val safeArea = if (areaRatio <= 0f) 1f else areaRatio
            val screenWidth = size.width
            val screenHeight = size.height / safeArea
            val screenAspectRatio = screenWidth / screenHeight

            val dstWidth: Float
            val dstHeight: Float
            val offsetX: Float
            val offsetY: Float

            if (videoAspectRatio > screenAspectRatio) {
                dstWidth = screenWidth
                dstHeight = dstWidth / videoAspectRatio

                offsetX = 0f
                offsetY = (screenHeight - dstHeight) / 2f
            } else {
                dstHeight = screenHeight
                dstWidth = dstHeight * videoAspectRatio

                offsetY = 0f
                offsetX = (screenWidth - dstWidth) / 2f
            }

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
                blendMode = BlendMode.DstIn
            )

            canvas.restore()
        }
    }
}

fun Modifier.danmakuWebMask(
    frame: DanmakuWebMaskFrame,
    aspectRatio: Float,
    areaRatio: Float
): Modifier = composed {
    val svgObj = runCatching {
        SVG.getFromString(frame.svg)
    }.getOrNull() ?: return@composed this

    val svgWidth = svgObj.documentWidth.toInt()
    val svgHeight = svgObj.documentHeight.toInt()

    val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svgObj.renderToCanvas(canvas)

    bitmapMask(bitmap, aspectRatio, areaRatio)
}

fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame,
    aspectRatio: Float,
    areaRatio: Float
): Modifier = composed {
    val binaryBitmap= Bitmap.createBitmap(40, 180, Bitmap.Config.ARGB_8888)
    frame.image.forEachIndexed { index, byte ->
        val y= index / 40
        val x= index % 40
        binaryBitmap.setPixel(x, y, if (byte.toInt() == 0) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT)
    }

    bitmapMask(binaryBitmap, aspectRatio, areaRatio)
}

fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?,
    aspectRatio: Float,
    areaRatio: Float
): Modifier = composed {
    if (frame == null) return@composed this

    when(frame){
        is DanmakuWebMaskFrame -> danmakuWebMask(frame, aspectRatio, areaRatio)
        is DanmakuMobMaskFrame -> danmakuMobMask(frame, aspectRatio, areaRatio)
    }
}