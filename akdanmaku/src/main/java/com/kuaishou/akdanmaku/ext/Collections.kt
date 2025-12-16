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

package com.kuaishou.akdanmaku.ext

import com.kuaishou.akdanmaku.collection.TreeList

/**
 * 集合扩展
 *
 * @author Xana
 * @since 2021-07-02
 */


/**
 * 二分查找，找到 >= 值的 index。需要列表已排序
 *
 * @param key 需要查找的值
 * @param selector 获取列表数据类型中需要比较的值的选择器
 * @return 如果集合为空，返回 -1，否则返回第一个大于等于 [key] 的 index
 */
fun <T, K : Comparable<K>> List<T>.binarySearchAtLeast(key: K, selector: (T) -> K): Int {

  var low = 0
  var high = size - 1
  if (isEmpty()) return -1

  while (low < high) {
    val mid = (low + high).ushr(1) // safe from overflows
    val midVal = get(mid)
    val cmp = compareValues(selector(midVal), key)

    when {
      cmp < 0 -> low = mid + 1
      cmp > 0 -> high = mid
      else -> return mid - 1
    } // key found
  }
  return low  // key not found
}

/**
 * 二分查找，找到 <= 值的 index。需要列表已排序
 *
 * @param key 需要查找的值
 * @param selector 获取列表数据类型中需要比较的值的选择器
 * @return 如果集合为空，返回 -1，否则返回第一个小雨等于 [key] 的 index
 */
fun <T, K : Comparable<K>> List<T>.binarySearchAtMost(key: K, selector: (T) -> K): Int {

  var low = 0
  var high = size - 1
  if (isEmpty()) return -1

  while (low < high) {
    val mid = (low + high).ushr(1) // safe from overflows
    val midVal = get(mid)
    val cmp = compareValues(selector(midVal), key)

    when {
      cmp < 0 -> low = mid + 1
      cmp > 0 -> high = mid
      else -> return mid - 1
    } // key found
  }
  return high  // key not found
}

fun <T : Comparable<T>> Collection<T>.toTreeList(): TreeList<T> {
  return TreeList(this)
}
