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

import android.os.SystemClock
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ext.isOutside
import com.kuaishou.akdanmaku.ext.isTimeout
import com.kuaishou.akdanmaku.utils.DanmakuTimer
import java.util.*

/**
 *
 *
 * @author Xana
 * @since 2021-06-30
 */
open class DuplicateMergingFilter : DanmakuDataFilter(DanmakuFilters.FILTER_TYPE_DUPLICATE_MERGE) {

  protected val blockedDanmakus = TreeSet<DanmakuItem>()
  protected val currentDanmakus = mutableMapOf<String, DanmakuItem>()
  protected val passedDanmakus = TreeSet<DanmakuItem>()

  private fun removeTimeoutDanmakus(limitTime: Int, currentTimeMills: Long) {
    val startTime: Long = SystemClock.uptimeMillis()
    removeTimeout(blockedDanmakus.iterator(), startTime, limitTime, currentTimeMills)
    removeTimeout(passedDanmakus.iterator(), startTime, limitTime, currentTimeMills)
    removeTimeoutMap(currentDanmakus.iterator(), startTime, limitTime, currentTimeMills)
  }

  private fun removeTimeout(
    iterator: MutableIterator<DanmakuItem>,
    startTime: Long,
    limitTime: Int,
    currentTimeMills: Long
  ) {
    while (iterator.hasNext()) {
      if (SystemClock.uptimeMillis() - startTime <= limitTime) return
      val entity = iterator.next()
      if (entity.isTimeout(currentTimeMills)) iterator.remove()
      else return
    }
  }

  private fun removeTimeoutMap(
    iterator: MutableIterator<Map.Entry<String, DanmakuItem>>,
    startTime: Long,
    limitTime: Int,
    currentTimeMills: Long
  ) {
    while (iterator.hasNext()) {
      if (SystemClock.uptimeMillis() - startTime <= limitTime) return
      val entry = iterator.next()
      val entity = entry.value
      if (entity.isTimeout(currentTimeMills)) iterator.remove()
      else return
    }
  }

  override fun filter(item: DanmakuItem, timer: DanmakuTimer, config: DanmakuConfig): Boolean {
    val data = item.data
    val currentTimeMills = timer.currentTimeMs
    removeTimeoutDanmakus(7, currentTimeMills)
    return if (blockedDanmakus.contains(item) && !item.isOutside(currentTimeMills)) {
      true
    } else if (passedDanmakus.contains(item)) {
      false
    } else if (currentDanmakus.containsKey(data.content)) {
      currentDanmakus[data.content] = item
      blockedDanmakus.remove(item)
      blockedDanmakus.add(item)
      true
    } else {
      currentDanmakus[data.content] = item
      passedDanmakus.add(item)
      true
    }
  }
}
