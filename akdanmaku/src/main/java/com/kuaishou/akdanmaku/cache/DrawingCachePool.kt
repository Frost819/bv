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

import android.util.Log

/**
 *
 *
 * @author Xana
 * @since 2021-06-23
 */
class DrawingCachePool(private val maxMemorySize: Int) {

  private val caches = mutableSetOf<DrawingCache>()

  private var memorySize = 0

  fun acquire(width: Int, height: Int): DrawingCache? {
    synchronized(this) {
      return caches.firstOrNull {
        it.width >= width && it.height >= height &&
          it.width - width < 5 && it.height - height < 5
      }?.also {
        caches.remove(it)
        memorySize -= it.size
      }
    }
  }

  fun release(cache: DrawingCache?): Boolean {
    cache?.get() ?: return true
    if (cache in caches) return false
    return if (cache.size + memorySize > maxMemorySize) {
      Log.v("DrawingCache", "[Release][+] OOM Pool")
      false
    } else {
      synchronized(this) {
        caches.add(cache)
        cache.erase()
        memorySize += cache.size
      }
      true
    }
  }

  fun clear() {
    synchronized(this) {
      caches.forEach { it.destroy() }
      caches.clear()
      memorySize = 0
    }
  }
}
