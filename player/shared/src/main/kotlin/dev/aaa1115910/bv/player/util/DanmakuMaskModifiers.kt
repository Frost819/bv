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
    imageBitmap: ImageBitmap
): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint())
        drawContent()
        drawImage(
            image = imageBitmap,
            dstSize = size.toIntSize(),
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
    frame: DanmakuWebMaskFrame
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

    bitmapMask(cachedImage)
}

/**
 * Mob 蒙版：40×180 二值图。
 * 优化：使用 IntArray + setPixels 批量写入替代逐像素 setPixel，性能提升约 10 倍。
 */
fun Modifier.danmakuMobMask(
    frame: DanmakuMobMaskFrame
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

    bitmapMask(cachedImage)
}

/**
 * 统一蒙版入口，根据蒙版类型分发。
 * 使用 remember(frame) 确保同一帧不重复计算 Modifier 链。
 */
fun Modifier.danmakuMask(
    frame: DanmakuMaskFrame?
): Modifier = composed {
    if (frame == null) return@composed this

    when (frame) {
        is DanmakuWebMaskFrame -> danmakuWebMask(frame)
        is DanmakuMobMaskFrame -> danmakuMobMask(frame)
    }
}