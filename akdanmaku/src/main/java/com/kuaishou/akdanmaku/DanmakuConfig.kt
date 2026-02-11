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

/*******************************************************************************
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
 ******************************************************************************/
package com.kuaishou.akdanmaku

import android.util.Log
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuDataFilter
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuLayoutFilter
import com.kuaishou.akdanmaku.ext.RETAINER_AKDANMAKU

/**
 * 弹幕场景设置参数
 */
data class DanmakuConfig(

  var retainerPolicy: Int = RETAINER_AKDANMAKU,

  /**
   * 预加载缓存的时间提前量
   * 优化：增加时间，给密集弹幕更多准备时间
   */
  var preCacheTimeMs: Long = 600L,

  /**
   * 弹幕的显示时长，滚动类型的弹幕为从屏幕一端出现到屏幕另一端完全移出的时间
   */
  var durationMs: Long = DEFAULT_DURATION,

  /**
   * 滚动弹幕持续时间
   */
  var rollingDurationMs: Long = durationMs,

  /**
   * 文本缩放倍数
   */
  var textSizeScale: Float = 1f,

  /**
   * 滚动弹幕持续时间因子（改变滚动速度）
   */
  var rollingDurationFactor: Float = 1f,

  /**
   * 滚动弹幕屏幕的显示区域
   */
  var screenPart: Float = 1f,

  /**
   * 弹幕显示的透明度（不会影响选中的）
   */
  var alpha: Float = 1f,

  /**
   * 弹幕是否以粗体渲染
   */
  var bold: Boolean = true,

  /**
   * 绘图 Bitmap 的密度
   */
  var density: Int = 160,

  /**
   * 弹幕是否可见
   */
  var visibility: Boolean = true,

  /**
   * 是否允许重叠
   */
  var allowOverlap: Boolean = false,

  /**
   * 同屏最大弹幕数量限制（0表示不限制）
   * 在高码率视频场景下，建议设置为100-150，避免过载
   */
  var maxOnScreenDanmaku: Int = 0,

  /**
   * 可见性标记，当可见性发生变化时更新
   */
  var visibilityGeneration: Int = 0,

  /**
   * 布局变化标记位，当需要对弹幕重新布局时更新此值
   */
  var layoutGeneration: Int = 0,

  /**
   * 缓存标记位，当弹幕本身样式发生变化需要对绘制样式与缓存进行更新时更新此值
   */
  var cacheGeneration: Int = 0,

  /**
   * 测量标记位，与缓存类似
   */
  var measureGeneration: Int = 0,

  /**
   * 过滤器标记位，当过滤器发生变动时（个数或具体值）更新此值
   */
  var filterGeneration: Int = 0,

  /**
   * 排布标记位，当需要清空排布器，重新高度排布时更新此值
   */
  var retainerGeneration: Int = 0,

  /**
   * 渲染标记位，一般意义上每一次 Update 后都会更新此值，用于计算跳帧与绘制
   */
  var renderGeneration: Int = 0,

  /**
   * 首次显示标记位，主要用于埋点
   */
  internal var firstShownGeneration: Int = 0,
  var dataFilter: List<DanmakuDataFilter> = emptyList(),
  var layoutFilter: List<DanmakuLayoutFilter> = emptyList()
) {

  var allGeneration =
    visibilityGeneration +
      layoutGeneration +
      cacheGeneration +
      measureGeneration +
      filterGeneration +
      retainerGeneration +
      renderGeneration
    private set

  fun updateVisibility() {
    visibilityGeneration++
    allGeneration++
    logGeneration("visibility", visibilityGeneration)
  }

  fun updateCache() {
    cacheGeneration++
    allGeneration++
    logGeneration("cache", cacheGeneration)
  }

  fun updateFilter() {
    filterGeneration++
    allGeneration++
    logGeneration("filter", filterGeneration)
  }

  fun updateMeasure() {
    measureGeneration++
    allGeneration++
    logGeneration("measure", measureGeneration)
  }

  fun updateLayout() {
    layoutGeneration++
    allGeneration++
    logGeneration("layout", layoutGeneration)
  }

  fun updateRetainer() {
    retainerGeneration++
    allGeneration++
    logGeneration("retainer", retainerGeneration)
  }

  fun updateRender() {
    renderGeneration++
    allGeneration++
  }

  fun updateFirstShown() {
    firstShownGeneration++
  }

  companion object {
    private fun logGeneration(type: String, generation: Int) {
      Log.d(DanmakuEngine.TAG, "Generation[$type] update to $generation")
    }

    // 60M 缓存池
    var CACHE_POOL_MAX_MEMORY_SIZE = 1024 * 1024 * 60

    const val DEFAULT_DURATION = 3800L
  }
}
