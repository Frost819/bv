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

package com.kuaishou.akdanmaku.cache

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.kuaishou.akdanmaku.ext.EMPTY_BITMAP
import java.lang.Exception
import kotlin.math.max

/**
 *
 *
 * @author Xana
 * @since 2021-06-22
 */
class DrawingCacheHolder {
  var canvas: Canvas = Canvas()
  var bitmap: Bitmap = EMPTY_BITMAP
  var width: Int = 0
  var height: Int = 0

  fun buildCache(w: Int, h: Int, density: Int, checkSize: Boolean, bitsPerPixel: Int = 32) {
    val reuse = if (checkSize) (w == width && h == height) else w <= width && h <= height
    if (bitmap != EMPTY_BITMAP && !bitmap.isRecycled) {
      if (reuse) {
        bitmap.eraseColor(Color.TRANSPARENT)
        canvas.setBitmap(bitmap)
        return
      }
    }
    width = max(1, w)
    height = max(1, h)
    @Suppress("DEPRECATION")
    val config = if (bitsPerPixel == 32) Bitmap.Config.ARGB_8888 else Bitmap.Config.ARGB_4444
    try {
      bitmap = Bitmap.createBitmap(width, height, config).also {
        if (density > 0) {
          it.density = density
        }
        canvas.setBitmap(it)
        canvas.density = density
      }
    } catch (e: Exception) {
      bitmap = EMPTY_BITMAP
      canvas.setBitmap(null)
      width = 0
      height = 0
    }
  }

  fun erase() {
    if (bitmap.isRecycled) return
    bitmap.eraseColor(Color.TRANSPARENT)
  }

  fun recycle() {
    if (bitmap == EMPTY_BITMAP) return
    // 优化: 正确回收Bitmap内存
    if (!bitmap.isRecycled) {
      bitmap.recycle()
    }
    canvas.setBitmap(null)
    bitmap = EMPTY_BITMAP
    width = 0
    height = 0
  }
}
