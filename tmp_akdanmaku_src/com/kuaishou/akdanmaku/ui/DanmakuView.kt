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

package com.kuaishou.akdanmaku.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * 用于显示弹幕的 UI View，与 DanmakuPlayer 绑定并联合实现弹幕的具体展现逻辑。
 * 起关系类似于视频播放场景 ViewView & MediaPlayer 的关系
 */
class DanmakuView : View {
  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  var danmakuPlayer: DanmakuPlayer? = null
  internal val displayer: ViewDisplayer = ViewDisplayer()

  init {
    context.resources.displayMetrics?.let { metrics ->
      displayer.density = metrics.density
      displayer.scaleDensity = metrics.scaledDensity
      displayer.densityDpi = metrics.densityDpi
    }
  }

  override fun onDraw(canvas: Canvas) {
    val width = measuredWidth
    val height = measuredHeight
    // 部分机型存在长按时大小为零的问题（Flyme）
    if (width == 0 || height == 0) return
    danmakuPlayer?.notifyDisplayerSizeChanged(width, height)
    danmakuPlayer?.draw(canvas)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    danmakuPlayer?.notifyDisplayerSizeChanged(w, h)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    danmakuPlayer?.notifyDisplayerSizeChanged(right - left, bottom - top)
  }

  class ViewDisplayer : DanmakuDisplayer {
    override var height: Int = 0
    override var width: Int = 0
    override var margin: Int = 4
    override var allMarginTop: Float = 0f
    override var density: Float = 1f
    override var scaleDensity: Float = 1f
    override var densityDpi: Int = 160
  }
}
