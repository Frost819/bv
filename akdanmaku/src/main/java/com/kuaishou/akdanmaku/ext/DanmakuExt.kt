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

import android.graphics.Bitmap
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.Size
import kotlin.math.abs

/**
 * 数据相关扩展
 *
 * @author Xana
 * @since 2021-06-16
 */
val EMPTY_BITMAP: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

fun DanmakuItem.isTimeout(current: Long): Boolean =
  current - timePosition > duration

fun DanmakuItem.isLate(current: Long): Boolean =
  current - timePosition < 0

fun DanmakuItem.isOutside(current: Long): Boolean =
  isTimeout(current) || isLate(current)

internal fun DanmakuItem.willCollision(
  danmaku: DanmakuItem,
  displayer: DanmakuDisplayer,
  current: Long,
  durationMs: Long
): Boolean {
  if (isOutside(current)) return false
  val dt = danmaku.timePosition - timePosition
  if (dt <= 0) return true
  if (abs(dt) >= durationMs ||
    isTimeout(current) ||
    danmaku.isTimeout(current)) {
    return false
  }

  if (data.mode == DanmakuItemData.DANMAKU_MODE_CENTER_TOP || data.mode == DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM) {
    return true
  }

  return checkCollisionAtTime(this, danmaku, displayer, current, durationMs) ||
    checkCollisionAtTime(this, danmaku, displayer, current + durationMs, durationMs)
}

private fun checkCollisionAtTime(
  d1: DanmakuItem,
  d2: DanmakuItem,
  displayer: DanmakuDisplayer,
  current: Long,
  durationMs: Long
): Boolean {
  val width = displayer.width
  val w1 = d1.drawState.width
  val w2 = d2.drawState.width
  val dt1 = current - d1.timePosition
  val dt2 = current - d2.timePosition
  val r1 = width - (width + w1) * (dt1.toFloat() / durationMs) + w1
  val l2 = width - (width + w2) * (dt2.toFloat() / durationMs)
  return l2 < r1
// 这是一个用于加速运算的简化公式，但是没有经过验证
// return current * (w2 - w1) + d1.position * (width + w1) - d2.position * (width + w2) - w1 * duration <= 0
}

const val RETAINER_BILIBILI = 0
const val RETAINER_AKDANMAKU = 1
