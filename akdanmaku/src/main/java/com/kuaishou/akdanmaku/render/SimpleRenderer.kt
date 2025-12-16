/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Kwai, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.kuaishou.akdanmaku.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.math.MathUtils.clamp
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.Size
import java.util.HashMap
import kotlin.math.roundToInt

/**
 * 一个默认的，实现了简单只绘制文字和描边的弹幕渲染器
 *
 * @author Xana
 */
open class SimpleRenderer : DanmakuRenderer {

  private val textPaint = TextPaint().apply {
    color = Color.WHITE
    style = Paint.Style.FILL
    isAntiAlias = true
  }
  private val strokePaint = TextPaint().apply {
    textSize = textPaint.textSize
    color = Color.BLACK
    strokeWidth = 3f
    style = Paint.Style.FILL_AND_STROKE
    isAntiAlias = true
  }
  private val debugPaint by lazy {
    Paint().apply {
      color = Color.RED
      style = Paint.Style.STROKE
      isAntiAlias = true
      strokeWidth = 6f
    }
  }
  private val borderPaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
    isAntiAlias = true
    strokeWidth = 6f
  }

  override fun updatePaint(
    item: DanmakuItem,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ) {
    val danmakuItemData = item.data
    // update textPaint
    val textSize = clamp(danmakuItemData.textSize.toFloat(), 12f, 25f) * (displayer.density - 0.6f)
    textPaint.color = danmakuItemData.textColor or Color.argb(255, 0, 0, 0)
    textPaint.textSize = textSize * config.textSizeScale
    textPaint.typeface = if (config.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    // update strokePaint
    strokePaint.textSize = textPaint.textSize
    strokePaint.typeface = textPaint.typeface
    strokePaint.color = if (textPaint.color == DEFAULT_DARK_COLOR) Color.WHITE else Color.BLACK
  }

  override fun measure(
    item: DanmakuItem,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Size {
    updatePaint(item, displayer, config)
    val danmakuItemData = item.data
    val textWidth = textPaint.measureText(danmakuItemData.content)
    val textHeight = getCacheHeight(textPaint)
    return Size(textWidth.roundToInt() + CANVAS_PADDING, textHeight.roundToInt() + CANVAS_PADDING)
  }

  override fun draw(
    item: DanmakuItem,
    canvas: Canvas,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ) {
    updatePaint(item, displayer, config)
    val danmakuItemData = item.data
    val x = CANVAS_PADDING * 0.5f
    val y = CANVAS_PADDING * 0.5f - textPaint.ascent()
    canvas.drawText(danmakuItemData.content, x, y, strokePaint)
    canvas.drawText(danmakuItemData.content, x, y, textPaint)
    if (danmakuItemData.danmakuStyle == DanmakuItemData.DANMAKU_STYLE_SELF_SEND) {
      canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), borderPaint)
    }
  }

  companion object {
    private val DEFAULT_DARK_COLOR: Int = Color.argb(255, 0x22, 0x22, 0x22)

    private const val CANVAS_PADDING: Int = 6

    private val sTextHeightCache: MutableMap<Float, Float> = HashMap()

    private fun getCacheHeight(paint: Paint): Float {
      val textSize = paint.textSize
      return sTextHeightCache[textSize] ?: let {
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading
        sTextHeightCache[textSize] = textHeight
        textHeight
      }
    }
  }
}
