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
 * 绝对移动 Action
 *
 * @author Xana
 * @since 2021-07-14
 */
class MoveToAction : TemporalAction() {

  var startX = 0f
  var startY = 0f
  var endX = 0f
  var endY = 0f

  override fun update(percent: Float) {
    val x: Float
    val y: Float
    when (percent) {
      0f -> {
        x = startX
        y = startY
      }
      1f -> {
        x = endX
        y = endY
      }
      else -> {
        x = startX + (endX - startX) * percent
        y = startY + (endY - startY) * percent
      }
    }
    target?.position?.set(x, y)
  }

  override fun start() {
    val target = target ?: return
    startX = target.position.x
    startY = target.position.y
  }

  fun setPosition(x: Float, y: Float) {
    endX = x
    endY = y
  }

  fun setStartPosition(x: Float, y: Float) {
    startX = x
    startY = y
  }
}
