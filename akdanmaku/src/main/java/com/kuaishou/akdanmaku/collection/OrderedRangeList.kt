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

package com.kuaishou.akdanmaku.collection

import androidx.core.util.Pools
import java.util.*

/**
 * 有序范围列表，其中持有并维护一个有序连续的线段
 *
 * @author Xana
 * @since 2021-07-05
 */
class OrderedRangeList<T>(var start: Int, var end: Int, private val margin: Int = 0) {

  private val holderPool = Pools.SimplePool<Holder<T>>(100).apply {
    repeat(100) { release(Holder()) }
  }

  private val holders = mutableListOf<Holder<T>>().apply {
    add(Holder(start, end))
  }

  private val dataHolderMap = mutableMapOf<T, Holder<T>>()

  fun isEmpty() = holders.size == 1 && holders.firstOrNull()?.data == null

  fun update(start: Int, end: Int) {
    this.start = start
    this.end = end
    clear()
  }

  fun clear() {
    holders.clear()
    holders.add(Holder(start, end))
    dataHolderMap.clear()
  }

  fun contains(data: T): Boolean = dataHolderMap.contains(data)

  /**
   * O(n)
   *
   * 寻找的内容可以在 add/remove 时做动态的维护，维护一个 length - position 的 Map
   */
  fun find(length: Int, predicate: (T?) -> Boolean): List<Holder<T>> {
    if (holders.isEmpty()) return Collections.emptyList()
    // 寻找第一个满足条件的作为起始点
    var startIndex = holders.indexOfFirst { predicate(it.data) }
    var endIndex = startIndex
    while (endIndex >= 0 && endIndex < holders.size) {
      val start = holders[startIndex].start
      val end = holders[endIndex].end
      if (end - start < length) {
        // 空间不足
        endIndex++
        while (endIndex < holders.size && !predicate(holders[endIndex].data)) {
          // 空间不足且条件不满足，查找下一个连续区间
          endIndex++
          startIndex = endIndex
        }
      } else {
        // 空间足够
        return holders.subList(startIndex, endIndex + 1).toList()
      }
    }
    // 超出范围或者没有第一个满足条件的起始点
    return Collections.emptyList()
  }

  fun min(length: Int, selector: (T?) -> Int): List<Holder<T>> {
    if (holders.isEmpty()) return Collections.emptyList()
    var minLeft = Int.MAX_VALUE
    var minStart = 0
    var minEnd = 0
    var startIndex = 0
    var endIndex = startIndex
    while (endIndex >= 0 && endIndex < holders.size) {
      val start = holders[startIndex].start
      val end = holders[endIndex].end
      if (end - start < length) {
        // 空间不足
        endIndex++
      } else {
        val h = holders.subList(startIndex, endIndex + 1)
        // 空间足够
        if (h.any { selector(it.data) < minLeft }) {
          minLeft = h.minOf { selector(it.data) }
          minStart = startIndex
          minEnd = endIndex
        }
        startIndex++
        endIndex++
      }
    }
    return if (minEnd >= minStart) {
      holders.subList(minStart, minEnd + 1).toList()
    } else Collections.emptyList()
  }

  fun add(place: List<Holder<T>>, start: Int, end: Int, data: T): Boolean {
    if (place.isEmpty() ||
      !place.all { it.invalid } ||
      start < place.first().start ||
      end > place.last().end ||
      start >= end
    ) {
//      Log.w(DanmakuEngine.TAG, "[Retainer] add failed $place, start: $start, end: $end")
      return false
    }

    if (!checkContinuous(place)) {
      return false
    }

    val placeStart = place.first().start
    var placeEnd = place.last().end
    val index = holders.binarySearchBy(placeStart) { it.start }
    if (index < 0) {
//      Log.w(DanmakuEngine.TAG, "[Retainer] add failed: cannot find place: ${place.first()} in $holders")
      return false
    }
    // 检查二分查找正确性
//    if (holders[index].start != placeStart || holders[index].end != place.first().end) {
//      Log.w(DanmakuEngine.TAG, "[Retainer] binary search index incorrect. ${holders[index]} vs ${place.first()}")
//    }
//    Log.d(DanmakuEngine.TAG, "[Retainer] add place $place with [$start..$end]")

    // 移除被占用的空间
    place.forEach { h ->
      h.data?.let { dataHolderMap.remove(it) }
      holders.removeAt(index)
    }
//    Log.d(DanmakuEngine.TAG, "[Retainer] remove place $place")

    // 将剩余空间添加，并与相邻空间合并
    if (end + margin < placeEnd) {
      // 寻找后部可以合并的空间
      while (index + 1 < holders.size && holders[index].data == null) {
        placeEnd = holders[index].end
        recycle(holders.removeAt(index))
      }
      val h = obtain(end + margin, placeEnd)
//      Log.d(DanmakuEngine.TAG, "[Retainer] add remain range $h")
      holders.add(index, h)
    }
    val h = obtain(start, end, data)
    holders.add(index, h)
//    Log.d(DanmakuEngine.TAG, "[Retainer] add range $h")
    dataHolderMap[data] = h
    place.forEach(::recycle)
    return true
  }

  fun remove(data: T) {
    val h = dataHolderMap[data] ?: let {
//      Log.d(DanmakuEngine.TAG, "[Retainer] remove failed: cannot find holder for $data")
      return
    }
    remove(h)
  }

  fun remove(holder: Holder<T>) {
    var index = holders.binarySearchBy(holder.start) { it.start }
    if (index < 0) {
//      Log.w(DanmakuEngine.TAG, "[Retainer] cannot remove range $holder")
      return
    }
    var start = holder.start
    var end = holder.end
    if (index > 0) {
      val beforeHolder = holders[index - 1]
      if (beforeHolder.data == null) {
        start = beforeHolder.start
        index--
        holders.removeAt(index)
//        Log.d(DanmakuEngine.TAG, "[Retainer] remove range $beforeHolder")
        recycle(beforeHolder)
      }
    }
//    Log.d(DanmakuEngine.TAG, "[Retainer] remove range $holder")
    holder.data?.let { dataHolderMap.remove(it) }
    holders.removeAt(index)
    recycle(holder)
    if (index < holders.size) {
      val afterHolder = holders[index]
      if (afterHolder.data == null) {
        end = afterHolder.end
        holders.removeAt(index)
//        Log.d(DanmakuEngine.TAG, "[Retainer] remove range $afterHolder")
        recycle(afterHolder)
      }
    }
    holders.add(index, obtain(start, end))
//    Log.d(DanmakuEngine.TAG, "[Retainer] add merged range [$start..$end]")

  }

  private fun obtain(start: Int, end: Int, data: T? = null): Holder<T> =
    holderPool.acquire()?.also {
      it.start = start
      it.end = end
      it.data = data
    } ?: Holder(start, end, data)

  private fun recycle(holder: Holder<T>) {
    if (holderPool.release(holder)) {
      holder.data = null
      holder.start = -1
      holder.end = -1
    }
  }

  private fun checkContinuous(holders: List<Holder<T>>): Boolean {
    // 检查连续性
    if (holders.zipWithNext().any { (last, next) ->
        last.end != next.start || last.start >= next.start
      }) {
      return false
    }
    return true
  }

  class Holder<T>(
    var start: Int = -1,
    var end: Int = -1,
    var data: T? = null
  ) {
    val invalid: Boolean
      get() = start != -1 && end != -1

    override fun toString(): String = "[$start..$end]${data?.let { "-Data" }.orEmpty()}"
  }
}
