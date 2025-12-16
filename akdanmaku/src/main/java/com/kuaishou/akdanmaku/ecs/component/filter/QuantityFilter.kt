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

package com.kuaishou.akdanmaku.ecs.component.filter

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ext.isTimeout
import com.kuaishou.akdanmaku.utils.DanmakuTimer
import kotlin.properties.Delegates

/**
 * 根据同屏数量过滤弹幕
 *
 * @author Xana
 * @since 2021-06-30
 */
class QuantityFilter(maxCount: Int = -1) : DanmakuDataFilter(DanmakuFilters.FILTER_TYPE_QUANTITY) {

  private var maxCount by Delegates.observable(maxCount) { _, _, new ->
    filterFactor = 1f / new
  }
  private var lastSkipped: DanmakuItem? = null
  private var filterFactor = 1f

  override fun filter(item: DanmakuItem, timer: DanmakuTimer, config: DanmakuConfig): Boolean {
    if (maxCount <= 0) return false
    val currentTimeMills = timer.currentTimeMs
    if (lastSkipped?.isTimeout(currentTimeMills) != false) {
      lastSkipped = item
      return false
    }
    val lastSkipped = this.lastSkipped ?: return false
    val gapTime = item.timePosition - lastSkipped.timePosition
    val duration = config.durationMs
    if (gapTime >= 0 && duration >= 0 && gapTime < (duration * filterFactor)) {
      return true
    }
    return false
  }
}
