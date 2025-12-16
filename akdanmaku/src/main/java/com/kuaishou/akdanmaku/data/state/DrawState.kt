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

import android.graphics.Matrix
import android.graphics.RectF
import com.kuaishou.akdanmaku.cache.DrawingCache
import com.kuaishou.akdanmaku.utils.onChange

/**
 * 绘图状态
 *
 * @author Xana
 * @since 2021-07-16
 */
internal class DrawState : State() {
  private var rectDirty = false
  internal val rect: RectF = RectF()
    get() {
      if (rectDirty) updateRect(field)
      return field
    }

  private var transformDirty = false
  internal val transform: Matrix = Matrix()
    get() {
      if (transformDirty) updateMatrix(field)
      return field
    }

  private val generationMap = mutableMapOf<String, Int>().withDefault { -1 }

  var layoutGeneration: Int by generationMap
  var measureGeneration: Int by generationMap
  var cacheGeneration: Int by generationMap
  override var generation: Int by generationMap

  private val marker = { _: Float -> markDirty() }
  var drawingCache: DrawingCache = DrawingCache.EMPTY_DRAWING_CACHE
  var visibility: Boolean = false
  var alpha: Float = 1f
  var positionX: Float by onChange(0f, marker)
  var positionY: Float by onChange(0f, marker)
  var width: Float by onChange(0f, marker)
  var height: Float by onChange(0f, marker)

  var translateX: Float by onChange(0f, marker)
  var translateY: Float by onChange(0f, marker)
  var scaleX: Float by onChange(1f, marker)
  var scaleY: Float by onChange(1f, marker)
  var rotation: Float by onChange(0f, marker)

  fun isMeasured(measureGeneration: Int): Boolean = width > 0f && height > 0f &&
    this.measureGeneration == measureGeneration

  fun resetActionProperty() {
    translateX = 0f
    translateY = 0f
    scaleX = 1f
    scaleY = 1f
    rotation = 0f
    alpha = 1f
  }

  override fun reset() {
    super.reset()
    visibility = false
    alpha = 1f
    positionX = 0f
    positionY = 0f
    width = 0f
    height = 0f
    translateX = 0f
    translateY = 0f
    scaleX = 1f
    scaleY = 1f
    rotation = 0f
    rectDirty = false
    transformDirty = false
    rect.setEmpty()
    transform.reset()
    recycle()
  }

  fun recycle() {
    if (drawingCache != DrawingCache.EMPTY_DRAWING_CACHE) {
      drawingCache.decreaseReference()
    }
    drawingCache = DrawingCache.EMPTY_DRAWING_CACHE
    layoutGeneration = -1
    cacheGeneration = -1
    visibility = false
  }

  private fun updateRect(rect: RectF) {
    rectDirty = false
    rect.set(positionX, positionY, positionX + width, positionY + height)
  }

  private fun updateMatrix(matrix: Matrix) {
    transformDirty = false
    matrix.reset()
    matrix.setScale(scaleX, scaleY)
    matrix.postRotate(rotation)
    matrix.postTranslate(positionX + translateX, positionY + translateY)
  }

  private fun markDirty() {
    transformDirty = true
    rectDirty = true
  }

  override fun toString(): String = "DrawState[measure: $measureGeneration, layout: $layoutGeneration]"
}
