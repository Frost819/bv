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

package com.kuaishou.akdanmaku.ecs

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.cache.CacheManager
import com.kuaishou.akdanmaku.collection.TreeList
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuFilters
import com.kuaishou.akdanmaku.ecs.system.DanmakuItemComparator
import com.kuaishou.akdanmaku.ext.binarySearchAtLeast
import com.kuaishou.akdanmaku.ext.binarySearchAtMost
import com.kuaishou.akdanmaku.render.DanmakuRenderer
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.DanmakuTimer
import com.kuaishou.akdanmaku.utils.Size
import java.util.*

/**
 * 弹幕上下文，存储所有相关信息
 *
 * @author Xana
 * @since 2021-07-15
 */
@Suppress("unused")
internal class DanmakuContext(val renderer: DanmakuRenderer) {
  val timer = DanmakuTimer()
  val cacheManager = CacheManager(CacheCallbackHandler(Looper.myLooper()!!), renderer)
  var config = DanmakuConfig()
  val filter = DanmakuFilters()

  val danmakus: MutableList<DanmakuItem> = TreeList()
  var slice: MutableList<DanmakuItem> = mutableListOf()
  val running: MutableList<DanmakuItem> = TreeList()

  internal var displayer: DanmakuDisplayer = object : DanmakuDisplayer {
    override var height: Int = 0
    override var width: Int = 0
    override val margin: Int = 4
    override val allMarginTop: Float = 0f
    override val density: Float = 1f
    override val scaleDensity: Float = 1f
    override val densityDpi: Int = 200
  }

  var sliceStartTime = 0L
  var sliceEndTime = 0L

  private var shouldSort = false
  private var sliceShouldSort = false
  private var sliceIter = slice.iterator()

  private var pendingAddItems = mutableListOf<DanmakuItem>()
  private var pendingCreateItems = mutableListOf<DanmakuItem>()
  private val comparator = DanmakuItemComparator()

  @AnyThread
  fun add(items: Collection<DanmakuItem>) {
    synchronized(this) {
      pendingAddItems.addAll(items)
    }
  }

  @AnyThread
  fun add(item: DanmakuItem) {
    synchronized(this) {
      pendingAddItems.add(item)
    }
  }

  fun updateConfig(config: DanmakuConfig) {
    this.config = config
    if (filter.dataFilter.size != config.dataFilter.size) {
      filter.dataFilter = config.dataFilter.toList()
    }
    if (filter.layoutFilter.size != config.layoutFilter.size) {
      filter.layoutFilter = config.layoutFilter.toList()
    }
  }

  @WorkerThread
  internal fun updateData() {
    val data = synchronized(this) {
      val d = pendingAddItems
      pendingAddItems = mutableListOf()
      d
    }
    // update full dataset
    danmakus.addAll(data)
    danmakus.sortWith(comparator)
  }

  @WorkerThread
  internal fun updateSlice() {
    if (timer.currentTimeMs > sliceEndTime || timer.currentTimeMs - config.durationMs < sliceStartTime) {
      val startTime = timer.currentTimeMs - config.durationMs
      val endTime = timer.currentTimeMs + config.durationMs
      val startIndex = danmakus.binarySearchAtLeast(startTime) { it.timePosition }
      val endIndex = danmakus.binarySearchAtMost(endTime) { it.timePosition }
      if (startIndex == -1 || endIndex == -1) {
        Log.e(DanmakuEngine.TAG, "[Context][Slice] failed update because cannot found corrected index.")
        return
      }
      sliceStartTime = startTime
      sliceEndTime = endTime
      slice = danmakus.subList(startIndex, endIndex + 1)
      sliceIter = slice.iterator()
    }
  }

  @WorkerThread
  internal fun updateEntity() {

  }

  @WorkerThread
  private fun sort() {
    if (shouldSort) {
      shouldSort = false
      danmakus.sortWith(comparator)
    }
    if (sliceShouldSort) {
      sliceShouldSort = false
      slice.sortWith(comparator)
    }
  }

  private inner class CacheCallbackHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        CacheManager.MSG_CACHE_RENDER -> {
          Log.w(DanmakuEngine.TAG, "[Context] onCacheSign, updateRender")
          config.updateRender()
        }
      }
    }
  }

  companion object {
    private val NONE_RENDERER = object : DanmakuRenderer {
      override fun updatePaint(
        item: DanmakuItem,
        displayer: DanmakuDisplayer,
        config: DanmakuConfig
      ) {}

      override fun measure(
        item: DanmakuItem,
        displayer: DanmakuDisplayer,
        config: DanmakuConfig
      ): Size { return Size(0, 0)}

      override fun draw(
        item: DanmakuItem,
        canvas: Canvas,
        displayer: DanmakuDisplayer,
        config: DanmakuConfig
      ) {}
    }
    val NONE_CONTEXT = DanmakuContext(NONE_RENDERER)
  }
}
