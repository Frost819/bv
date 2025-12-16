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
import android.os.SystemClock
import android.util.Log
import com.badlogic.ashley.core.PooledEngine
import com.kuaishou.akdanmaku.*
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.system.*
import com.kuaishou.akdanmaku.ecs.system.layout.LayoutSystem
import com.kuaishou.akdanmaku.ext.createSystem
import com.kuaishou.akdanmaku.ext.endTrace
import com.kuaishou.akdanmaku.ext.startTrace
import com.kuaishou.akdanmaku.layout.*
import com.kuaishou.akdanmaku.layout.TopCenterLayouter
import com.kuaishou.akdanmaku.layout.TopRollingLayouter
import com.kuaishou.akdanmaku.render.DanmakuRenderer
import com.kuaishou.akdanmaku.utils.DanmakuTimer

/**
 * 弹幕 ECS 架构的管理器
 * 1. 对外提供弹幕引擎核心 API 的调用入口
 * 2. 统一管理弹幕引擎内部的 Systems
 * 3. 通过 [act] 方法进行内部 Systems 的整体运算调度，通过 [draw] 方法发起渲染流程
 *
 * 同时，继承自 [PooledEngine]，支持 Entities 与 Components 的 Pooling，优化空间和性能。
 */
class DanmakuEngine(
  renderer: DanmakuRenderer,
  layouter: DanmakuLayouter = createDefaultLayouter()
) : PooledEngine(
  ENTITY_POOL_INITIAL_SIZE,
  ENTITY_POOL_MAX_SIZE,
  COMPONENT_POOL_INITIAL_SIZE,
  COMPONENT_POOL_MAX_SIZE
) {

  companion object {
    const val TAG = "DanmakuEngine"

    /**
     * 弹幕基础 System，数据相关
     */
    private val BASE_SYSTEMS = arrayOf(
      DanmakuSystem::class.java,
      DataSystem::class.java
    )

    /**
     * 展现相关的 System
     */
    private val VISUAL_SYSTEMS = arrayOf(
      LayoutSystem::class.java,
      ActionSystem::class.java,
      RenderSystem::class.java
    )

    private fun createDefaultLayouter() = TypedDanmakuLayouter(
      SimpleLayouter(),
      DanmakuItemData.DANMAKU_MODE_CENTER_TOP to TopCenterLayouter(),
      DanmakuItemData.DANMAKU_MODE_ROLLING to TopRollingLayouter(),
      DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM to BottomCenterLayouter()
    )

    // 以 Provider 的形式提供 Engine 示例
    internal fun get(
      renderer: DanmakuRenderer,
      layouter: DanmakuLayouter = createDefaultLayouter()
    ) = DanmakuEngine(renderer, layouter)
  }

  internal val isPaused: Boolean
    get() = timer.paused

  internal val context = DanmakuContext(renderer)
  internal val timer: DanmakuTimer = context.timer

  init {
    // Add systems
    var order = 1
    BASE_SYSTEMS.forEach {
      addSystem(createSystem(it, context).apply {
        this.priority = order++
      })
    }
    VISUAL_SYSTEMS.forEach {
      addSystem(createSystem(it, context).apply {
        this.priority = order++
      })
    }
    getSystem(LayoutSystem::class.java)?.layouter = layouter
  }

  internal fun step(deltaTimeSeconds: Float? = null) {
    startTrace("Engine_step")
    timer.step(deltaTimeSeconds)
    endTrace()
  }

  private var lastActTime = 0L

  /**
   * 调用内置 System 进行运算处理
   */
  internal fun act() {
    startTrace("act")
    val startTime = SystemClock.elapsedRealtime()
    timer.let {
      val interval = it.currentTimeMs - lastActTime
      update(it.deltaTimeSeconds)
      val cost = SystemClock.elapsedRealtime() - startTime
      if (cost >= 20) {
        Log.w(
          TAG,
          "[Engine][ACT] overload act: interval: $interval, cost: $cost"
        )
      }
      lastActTime = it.currentTimeMs
    }
    endTrace()
  }

  internal fun preAct() {
    getSystem(DataSystem::class.java)?.updateEntities()
  }

  /**
   * 负责绘制流程
   */
  internal fun draw(canvas: Canvas, onRenderReady: () -> Unit) {
    getSystem(RenderSystem::class.java)?.draw(canvas, onRenderReady)
  }

  internal fun getCurrentTimeMs(): Long {
    return timer.currentTimeMs
  }

  internal fun start() {
    timer.start()
    timer.paused = false
  }

  internal fun pause() {
    timer.paused = true
  }

  internal fun release() {
    timer.paused = true
    systems.forEach { system ->
      removeSystem(system)
    }

  }

  internal fun seekTo(positionMs: Long) {
    timer.start(positionMs)
    context.config.updateVisibility()
    context.config.updateRetainer()
    context.config.updateLayout()
  }

  internal fun updateConfig(danmakuConfig: DanmakuConfig) {
    getSystem(DanmakuSystem::class.java)?.updateDanmakuConfig(danmakuConfig)
  }

  internal fun getConfig(): DanmakuConfig? {
    return getSystem(DanmakuSystem::class.java)?.newConfig
  }

  internal fun updateTimerFactor(factor: Float) {
    timer.factor = factor
  }
}
