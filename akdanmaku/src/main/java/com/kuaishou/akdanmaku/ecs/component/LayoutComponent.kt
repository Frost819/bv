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

package com.kuaishou.akdanmaku.ecs.component

import android.graphics.PointF
import android.graphics.RectF
import com.kuaishou.akdanmaku.ecs.base.DanmakuBaseComponent

/**
 * 用于记录布局信息的 Component
 *
 * @author Xana
 * @since 2021-06-22
 */
class LayoutComponent : DanmakuBaseComponent() {
  var visibility: Boolean = false

  var layoutGeneration: Int = -1

  var position: PointF = PointF()

  var rect: RectF = RectF()

  var index: Int = -1

  override fun reset() {
    super.reset()
    visibility = false
    layoutGeneration = -1
    // 优化: 复用现有对象,避免频繁创建
    position.set(0f, 0f)
    rect.setEmpty()
    index = -1
  }

  fun update(
    visibility: Boolean = this.visibility,
    generation: Int = this.layoutGeneration,
    position: PointF = this.position,
    rect: RectF = this.rect,
    index: Int = this.index
  ): LayoutComponent {
    this.visibility = visibility
    this.layoutGeneration = generation
    if (this.position != position) {
      this.position.set(position)
    }
    if (this.rect != rect) {
      this.rect.set(rect)
    }
    this.index = index
    return this
  }

}
