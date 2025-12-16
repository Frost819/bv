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
import com.kuaishou.akdanmaku.layout.retainer.DanmakuRetainer
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer

/**
 * 内部用来封装 Retainer 和  Locator 的基类
 *
 * @author Xana
 * @since 2021-08-04
 */
internal abstract class BaseLayouter(
  private val retainer: DanmakuRetainer,
  private val locator: DanmakuRetainer.Locator
) : DanmakuLayouter {

  override fun preLayout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Boolean {
    val topPos = retainer.layout(item, currentTimeMills, displayer, config)
    item.drawState.positionY = topPos
    return item.drawState.visibility
  }

  override fun layout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ) {
    locator.layout(item, currentTimeMills, displayer, config)
  }

  override fun remove(item: DanmakuItem) {
    retainer.remove(item)
  }

  override fun updateScreenPart(startTop: Int, endTop: Int) {
    retainer.update(startTop, endTop)
  }

  override fun clear() {
    retainer.clear()
  }
}
