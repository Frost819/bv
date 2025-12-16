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

import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * 并行 Action
 *
 * @author Xana
 * @since 2021-07-15
 */
open class ParallelAction : Action {
  val actions = Array<Action>(4)
  var completed: Boolean = false
    private set

  override var target by Delegates.observable<ActionComponent?>(null) {_, _, new ->
    actions.forEach { it.target = new }
  }

  constructor(vararg action: Action) : super() {
    action.forEach { addAction(it) }
  }

  constructor() : super()

  override fun act(timeMills: Long): Boolean {
    if (timeMills < duration) {
      completed = false
    }
    completed = true
    val pool = holdPool()
    try {
      val iter = actions.iterator()
      while (iter.hasNext()) {
        val current = iter.next()
        if (current.target != null && !current.act(timeMills)) {
          completed = false
        }
        if (target == null) return true
      }
      return completed
    } finally {
      this.pool = pool
    }
  }

  override fun restart() {
    completed = false
    actions.forEach { it.restart() }
  }

  override fun reset() {
    super.reset()
    actions.clear()
  }

  open fun addAction(action: Action) {
    actions.add(action)
    target?.let { action.target = it }
    onActionAdded(action)
  }

  open fun addActions(actions: Array<out Action>) {
    this.actions.addAll(actions)
    target?.let { t -> actions.forEach {
      it.target = t
      onActionAdded(it)
    } }
  }

  protected open fun onActionAdded(action: Action) {
    duration = max(duration, action.duration)
  }
}
