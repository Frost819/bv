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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ext.EMPTY_BITMAP

/**
 * 绘制缓存
 *
 * @author Xana
 * @since 2021-06-22
 */
class DrawingCache {
  private val holder = DrawingCacheHolder()
  private var pendingRecycle = false
  private var refCount: Int = 0
  internal var cacheManager: CacheManager? = null

  var size = 0
    private set

  val width: Int
    get() = holder.width
  val height: Int
    get() = holder.height

  fun build(
    w: Int,
    h: Int,
    density: Int,
    checkSize: Boolean,
    bitsPerPixel: Int = 32
  ): DrawingCache {
    synchronized(this) {
      holder.buildCache(w, h, density, checkSize, bitsPerPixel)
      size = getBitmapSize(holder.bitmap)
      return this
    }
  }

  @SuppressLint("ObsoleteSdkInt")
  private fun getBitmapSize(bitmap: Bitmap?): Int {
    bitmap ?: return 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // API 19
      return bitmap.allocationByteCount
    }
    // 在低版本中用一行的字节x高度
    return bitmap.rowBytes * bitmap.height
  }

  fun erase() {
    synchronized(this) {
      holder.erase()
    }
  }

  fun get(): DrawingCacheHolder? =
    holder.takeIf { it.bitmap != EMPTY_BITMAP && !it.bitmap.isRecycled }

  fun destroy() {
    synchronized(this) {
      if (refCount <= 0) {
        recycle()
      } else {
        pendingRecycle = true
      }
    }
  }

  fun increaseReference() {
    synchronized(this) {
      refCount++
    }
  }

  fun decreaseReference() {
    synchronized(this) {
      refCount--
      if (refCount <= 0 && pendingRecycle) {
        cacheManager?.releaseCache(this)
      }
    }
  }

  private fun recycle() {
    synchronized(this) {
      if (Thread.currentThread().name != CacheManager.THREAD_NAME) {
        Log.e(
          DanmakuEngine.TAG,
          "DrawingCache recycle called must on cache thread but now on ${Thread.currentThread().name}",
          Throwable()
        )
      }
      if (refCount > 0) return
      pendingRecycle = false
      holder.recycle()
      size = 0
    }
  }

  companion object {
    val EMPTY_DRAWING_CACHE = DrawingCache()
  }
}
