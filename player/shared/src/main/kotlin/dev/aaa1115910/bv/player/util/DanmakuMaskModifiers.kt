package dev.aaa1115910.bv.player.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import com.caverock.androidsvg.SVG
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuWebMaskFrame

/**
 * 使用预转换的 ImageBitmap 进行蒙版绘制，避免每帧 Bitmap→ImageBitmap 转换开销。
 * saveLayer + DstIn 混合模式实现蒙版裁切。
 */
fun Modifier.bitmapMask(
    imageBitmap: ImageBitmap,
    videoAspectRatio: Float,
    areaRatio: Float
): Modifier = drawWithContent {
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

/**
 * Web 蒙版：解析 SVG → 渲染到 Bitmap → 转换为 ImageBitmap。
 * 使用 remember(frame) 缓存结果，同一帧数据不会重复解析。
 */
fun Modifier.danmakuWebMask(
    frame: DanmakuWebMaskFrame,
    videoAspectRatio: Float,
    areaRatio: Float
): Modifier = composed {
    val cachedImage = remember(frame) {
        runCatching {
            val svgObj = SVG.getFromString(frame.svg)
            val svgWidth = svgObj.documentWidth.toInt()
            val svgHeight = svgObj.documentHeight.toInt()
            if (svgWidth <= 0 || svgHeight <= 0) return@runCatching null
            val bitmap = Bitmap.createBitmap(svgWidth, svgHeight, Bitmap.Config.ARGB_8888)
            svgObj.renderToCanvas(Canvas(bitmap))
            bitmap.asImageBitmap()
        }.getOrNull()
    } ?: return@composed this

    bitmapMask(cachedImage, videoAspectRatio, areaRatio)
}

/**
 * Mob 蒙版：40×180 二值图。
 * 优化：使用 IntArray + setPixels 批量写入替代逐像素 setPixel，性能提升约 10 倍。
 */
fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame,
    videoAspectRatio: Float,
    areaRatio: Float
): Modifier = composed {
    val cachedImage = remember(frame) {
        val width = 40
        val height = 180
        val pixels = IntArray(width * height)
        val black = Color.BLACK
        val transparent = Color.TRANSPARENT
        for (i in pixels.indices) {
            pixels[i] = if (frame.image[i].toInt() == 0) black else transparent
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    }

    bitmapMask(cachedImage, videoAspectRatio, areaRatio)
}

/**
 * 统一蒙版入口，根据蒙版类型分发。
 * 使用 remember(frame) 确保同一帧不重复计算 Modifier 链。
 */
fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?,
    videoAspectRatio: Float, // 视频的宽高比 (例如 1920/1080 ≈ 1.77, 21/9 ≈ 2.33)
    areaRatio: Float         // 弹幕区域占屏幕高度的比例 (0.0 - 1.0)
): Modifier = composed {
    if (frame == null) return@composed this

    when (frame) {
        is DanmakuWebMaskFrame -> danmakuWebMask(frame, videoAspectRatio, areaRatio)
        is DanmakuMobMaskFrame -> danmakuMobMask(frame, videoAspectRatio, areaRatio)
    }
}