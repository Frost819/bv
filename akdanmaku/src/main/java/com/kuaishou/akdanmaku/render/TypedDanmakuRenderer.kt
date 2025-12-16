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
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.Size

/**
 * 默认根据 [DanmakuItemData.danmakuStyle] 为 type 的注册类型 Renderer，与 RecyclerView.Adapter 类似
 *
 * @author Xana
 * @since 2021-08-03
 */
open class TypedDanmakuRenderer(private val defaultRenderer: DanmakuRenderer, vararg renderers: Pair<Int, DanmakuRenderer>) : DanmakuRenderer {

  protected val renderers = mutableMapOf(*renderers)

  open fun getDanmakuType(item: DanmakuItem): Int = item.data.danmakuStyle

  fun registerRenderer(type: Int, renderer: DanmakuRenderer) {
    renderers[type] = renderer
  }

  override fun updatePaint(item: DanmakuItem, displayer: DanmakuDisplayer, config: DanmakuConfig) {
    val type = getDanmakuType(item)
    (renderers[type] ?: defaultRenderer).updatePaint(item, displayer, config)
  }

  override fun measure(
    item: DanmakuItem,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Size {
    val type = getDanmakuType(item)
    return (renderers[type] ?: defaultRenderer).measure(item, displayer, config)
  }

  override fun draw(
    item: DanmakuItem,
    canvas: Canvas,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ) {
    val type = getDanmakuType(item)
    (renderers[type] ?: defaultRenderer).draw(item, canvas, displayer, config)
  }
}
