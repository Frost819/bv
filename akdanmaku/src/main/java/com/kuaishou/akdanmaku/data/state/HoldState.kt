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

package com.kuaishou.akdanmaku.data.state

import com.kuaishou.akdanmaku.utils.DanmakuTimer

/**
 * 悬停状态
 *
 * @author Xana
 * @since 2021-07-15
 */
internal class HoldState(private val timer: DanmakuTimer) : State() {
  private var holdStartTime: Long = 0L
  private var holdOffsetTime: Long = 0L

  val holdTime: Long
    get() = holdOffsetTime + if (holdStartTime > 0) timer.currentTimeMs - holdStartTime else 0
  val isHolding: Boolean
    get() = holdStartTime > 0

  fun hold() {
    if (holdStartTime == 0L) {
      holdStartTime = timer.currentTimeMs
    }
  }

  fun unhold() {
    if (holdStartTime > 0) {
      val holdTime = timer.currentTimeMs - holdStartTime
      holdStartTime = 0
      if (holdTime > 0) {
        holdOffsetTime += holdTime
      }
    }
  }

  override fun reset() {
    super.reset()
    holdStartTime = 0
    holdOffsetTime = 0
  }
}
