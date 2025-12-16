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

package com.kuaishou.akdanmaku.ecs.component.action

/**
 * 循环重复 Action
 *
 * @author Xana
 * @since 2021-07-15
 */
class RepeatAction : DelegateAction() {

  private var repeatCount: Int = 0
  private var executedCount: Int = 0
  var finished = false
    private set
  var count: Int
    get() = repeatCount
    set(value) { repeatCount = value }

  init {
    updateDuration(action?.duration ?: 0)
  }

  override fun delegate(deltaTimeMs: Long): Boolean {
    if (executedCount == repeatCount) return true
    val action = this.action ?: return true
    if (action.act(deltaTimeMs)) {
      if (finished) return true
      if (repeatCount > 0) executedCount++
      if (executedCount == repeatCount) return true
      action.restart()
    }
    return false
  }

  override fun updateDuration(newDuration: Long) {
    duration = if (repeatCount < 0) Long.MAX_VALUE else newDuration * repeatCount
  }

  fun finish() {
    finished = true
  }

  override fun restart() {
    super.restart()
    executedCount = 0
    finished = false
  }

  companion object {
    const val FOREVER = -1
  }
}
