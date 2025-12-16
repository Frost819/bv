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
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.utils.DanmakuTimer
import java.util.*

/**
 * 集合类型的过滤
 *
 * @author Xana
 * @since 2021-06-30
 */
abstract class SimpleDanmakuFilter<T>(filterParams: Int, private val reverse: Boolean = false) :
  DanmakuDataFilter(filterParams) {

  private val mFilterSet = Collections.synchronizedSet(mutableSetOf<T>())

  var enable: Boolean = true

  val filterSet: Set<T>
    get() = mFilterSet

  fun addFilterItem(item: T) {
    mFilterSet.add(item)
  }

  fun removeFilterItem(item: T) {
    mFilterSet.remove(item)
  }

  fun clear() {
    mFilterSet.clear()
  }

  override fun filter(item: DanmakuItem, timer: DanmakuTimer, config: DanmakuConfig): Boolean {
    if (!enable) return false
    val data = item.data
    val filtered = mFilterSet.contains(filterField(data))
    return if (reverse) !filtered else filtered
  }

  abstract fun filterField(data: DanmakuItemData): T?
}
