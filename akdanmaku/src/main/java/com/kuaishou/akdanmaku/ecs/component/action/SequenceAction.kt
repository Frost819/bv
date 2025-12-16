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
 * 序列执行的 Action
 *
 * @author Xana
 * @since 2021-07-15
 */
class SequenceAction : ParallelAction {
  private val ranges = mutableListOf<LongRange>()

  constructor(vararg action: Action) : super(*action)
  constructor() : super()

  override fun act(timeMills: Long): Boolean {
    var index = ranges.indexOfFirst { timeMills in it }
    if (timeMills < 0) return false
    else if (index == -1) index = actions.size
    val pool = holdPool()
    try {
      var i = 0
      while (i < index) {
        val r = ranges[i]
        actions[i]?.act(r.last - r.first)
        i++
      }
      if (index < actions.size) {
        val action = actions[index]
        val range = ranges[index]
        if (action?.act(timeMills - range.first) != false) {
          if (target == null) return true
          if (index >= actions.size) return true
        }
        return false
      } else return true
    } finally {
      this.pool = pool
    }
  }

  override fun reset() {
    super.reset()
    ranges.clear()
  }

  override fun onActionAdded(action: Action) {
    ranges.add(duration until duration + action.duration)
    duration += action.duration
  }
}
