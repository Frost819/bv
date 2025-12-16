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

import kotlin.properties.Delegates

/**
 * 延迟执行的 Action
 *
 * @author Xana
 * @since 2021-07-15
 */
class DelayAction : DelegateAction {

  var delay: Long by Delegates.observable(0L) { _, _, _ ->
    updateDuration(action?.duration ?: 0L)
  }

  constructor(delay: Long) : super() {
    this.delay = delay
    duration = delay + (action?.duration ?: 0)
  }

  constructor() : super()

  override fun delegate(deltaTimeMs: Long): Boolean {

    val offsetTimeMills = when {
      deltaTimeMs < delay -> 0
      deltaTimeMs > duration -> duration - delay
      else -> deltaTimeMs - delay
    }
    return action?.act(offsetTimeMills) ?: true
  }

  override fun updateDuration(newDuration: Long) {
    duration = delay + newDuration
  }
}
