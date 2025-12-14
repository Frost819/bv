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

package com.kuaishou.akdanmaku.utils

/**
 * ECS 框架的计时器
 */
class DanmakuTimer {

  companion object {
    private fun getSystemTime(): Long {
      return System.nanoTime()
    }
  }

  var lastFrameTime = 0L
    private set

  /**
   * 时间倍速
   */
  var factor = 1.0f

  /**
   * 是否已暂停
   */
  var paused = true

  /**
   * 距上一次有效 update 的 deltaTime，单位：秒
   */
  var deltaTimeSeconds = 0f

  /**
   * 当前时间戳
   */
  val currentTimeMs: Long
    get() = currentNanoTime / 1_000_000

  private var currentNanoTime = 0L

  fun start(startTimeMs: Long = currentTimeMs, factor: Float = this.factor) {
    paused = false
    currentNanoTime = startTimeMs * 1_000_000
    this.factor = factor
    lastFrameTime = getSystemTime()
  }

  fun step(deltaTimeSeconds: Float? = null) {
    val time = getSystemTime()
    var deltaNano = if (paused) 0 else
      (deltaTimeSeconds?.times(1_000_000_000)?.toLong() ?: (time - lastFrameTime))
    deltaNano = (deltaNano * factor).toLong()
    currentNanoTime += deltaNano
    this.deltaTimeSeconds = deltaNano / 1_000_000_000f
    /*Log.d(
      "Timer",
      "deltaTimeSeconds=${this.deltaTimeSeconds}, currentTimeMs=$currentTimeMs, time=$time, lastTime=$lastFrameTime, deltaNano=$deltaNano"
    )*/
    lastFrameTime = time
  }
}
