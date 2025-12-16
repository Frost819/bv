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

package com.kuaishou.akdanmaku.layout

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer

/**
 * 默认的根据 mode 来区分的 Layouter
 *
 * @author Xana
 * @since 2021-08-03
 */
open class TypedDanmakuLayouter(
  private val defaultLayouter: DanmakuLayouter,
  vararg layouter: Pair<Int, DanmakuLayouter>
) : DanmakuLayouter {

  private val layouters = mutableMapOf(*layouter)

  protected open fun getDanmakuLayoutType(item: DanmakuItem): Int =
    item.data.mode

  private fun getLayouter(item: DanmakuItem): DanmakuLayouter {
    val type = getDanmakuLayoutType(item)
    return layouters[type] ?: defaultLayouter
  }

  override fun preLayout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Boolean {
    return getLayouter(item).preLayout(item, currentTimeMills, displayer, config)
  }

  override fun layout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ) {
    getLayouter(item).layout(item, currentTimeMills, displayer, config)
  }

  override fun updateScreenPart(startTop: Int, endTop: Int) {
    defaultLayouter.updateScreenPart(startTop, endTop)
    layouters.values.forEach { it.updateScreenPart(startTop, endTop) }
  }

  override fun clear() {
    defaultLayouter.clear()
    layouters.values.forEach { it.clear() }
  }

  override fun remove(item: DanmakuItem) {
    getLayouter(item).remove(item)
  }
}
