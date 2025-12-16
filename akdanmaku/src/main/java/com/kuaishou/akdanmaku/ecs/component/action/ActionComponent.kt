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

import android.graphics.Matrix
import android.graphics.PointF
import com.badlogic.gdx.utils.Array
import com.kuaishou.akdanmaku.ecs.base.DanmakuBaseComponent

/**
 * 持有 Action，计算 Action 以及计算后结果的 Component
 *
 * @author Xana
 * @since 2021-07-14
 */
class ActionComponent : DanmakuBaseComponent() {

  val actions = Array<Action>(0)

  val position = PointF()

  var rotation = 0f

  val scale = PointF(1f, 1f)

  var alpha = 1f

  var visibility = true

  fun toTransformMatrix(matrix: Matrix) {
    matrix.setScale(scale.x, scale.y)
    matrix.postRotate(rotation)
    matrix.postTranslate(position.x, position.y)
  }

  fun addAction(action: Action) {
    action.target = this
    action.restart()
    actions.add(action)
  }

  fun act(timeMills: Long) {
    resetProperty()
    val iter = actions.iterator()
    while (iter.hasNext()) {
      val action = iter.next()
      action.act(timeMills)
    }
  }

  override fun reset() {
    super.reset()
    resetProperty()
    actions.clear()
  }

  private fun resetProperty() {
    item.drawState.resetActionProperty()
    position.set(0f, 0f)
    rotation = 0f
    scale.set(1f, 1f)
    alpha = 1f
  }
}
