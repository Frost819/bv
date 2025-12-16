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

import androidx.annotation.CallSuper

/**
 * 临时 Action
 *
 * @author Xana
 * @since 2021-07-14
 */
abstract class TemporalAction(
  duration: Long = 0,
  var interpolation: Interpolation = Interpolation.linear
) : Action() {

  var reverse = false
  var completed = false
    private set

  init {
    this.duration = duration
  }

  override fun act(timeMills: Long): Boolean {
    if (timeMills < duration) {
      completed = false
    }
    if (timeMills < 0) {
      return false
    }
    val pool = holdPool()
    try {
      start()
      completed = timeMills >= duration
      val percent = interpolation.apply(if (completed) 1f else timeMills.toFloat() / duration)
      update(if (reverse) 1 - percent else percent)
      if (completed) complete()
      return completed
    } finally {
      this.pool = pool
    }
  }

  protected open fun start() {}

  protected open fun complete() {}

  override fun restart() {
    completed = false
  }

  protected abstract fun update(percent: Float)

  @CallSuper
  override fun reset() {
    super.reset()
    completed = false
    reverse = false
  }
}
