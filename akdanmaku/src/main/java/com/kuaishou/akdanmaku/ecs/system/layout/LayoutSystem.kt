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

package com.kuaishou.akdanmaku.ecs.system.layout

import android.util.Log
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.ItemState
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.base.DanmakuSortedSystem
import com.kuaishou.akdanmaku.ecs.component.LayoutComponent
import com.kuaishou.akdanmaku.ecs.component.filter.DanmakuLayoutFilter
import com.kuaishou.akdanmaku.ext.*
import com.kuaishou.akdanmaku.layout.DanmakuLayouter
import com.kuaishou.akdanmaku.layout.SimpleLayouter
import com.kuaishou.akdanmaku.layout.retainer.DanmakuRetainer
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.Families

/**
 * 布局系统
 * - 输入：拥有 Data, Filter, Cache 的实体
 * - 输出：添加或维护 [LayoutComponent]
 *
 * @author Xana
 * @since 2021-06-22
 */
internal class LayoutSystem(
  context: DanmakuContext
) : DanmakuSortedSystem(
  context,
  Family.all(*Families.layoutComponentTypes).get()
) {

  private var retainerGeneration = -1
  private var layoutGeneration = -1
  private var verifier = Verifier()

  private val cacheManager = context.cacheManager
  internal var layouter: DanmakuLayouter = SimpleLayouter()

  private val displayer: DanmakuDisplayer
    get() = danmakuDisplayer

  override fun update(deltaTime: Float) {
    startTrace("LayoutSystem_update")
    val config = danmakuContext.config
    val forceUpdate = retainerGeneration != config.retainerGeneration ||
      layoutGeneration != config.layoutGeneration
    if (isPaused && !forceUpdate) {
      endTrace()
      return
    }
    if (retainerGeneration != config.retainerGeneration) {
      Log.v(DanmakuEngine.TAG, "[Layout] RetainerGeneration change, clear retainer.")
      layouter.updateScreenPart(0, (displayer.height * config.screenPart).toInt())
      layouter.clear()
      retainerGeneration = config.retainerGeneration
    }
    if (verifier.filterGeneration != config.filterGeneration) {
      verifier.filterGeneration = config.filterGeneration
      // 优化: 避免拷贝,直接使用原list
      verifier.layoutFilters = config.layoutFilter
    }
    val currentTimeMills = currentTimeMs
    var needSync = false

    // 优化: 合并多层遍历为单次循环,减少中间集合和迭代器创建
    val entities = getEntities()
    for (i in 0 until entities.size) {
      val entity = entities[i]
      // 第一层filter: 检查过滤状态
      val filter = entity.filter ?: continue
      if (filter.filtered) continue
      
      // onEach: 测量逻辑
      val item = entity.dataComponent?.item ?: continue
      if (item.state != ItemState.Measuring) {
        val needRemeasure = !item.drawState.isMeasured(config.measureGeneration)
        if (item.state < ItemState.Measuring || needRemeasure) {
          if (needRemeasure && item.state >= ItemState.Measuring) {
            Log.v(DanmakuEngine.TAG, "[Layout] re-measure ${item.data}")
          }
          item.state = ItemState.Measuring
          cacheManager.requestMeasure(item, displayer, config)
          needSync = true
        }
      }
      
      // 第二层filter + forEach: 布局逻辑
      if (item.state < ItemState.Measured) continue
      val drawState = item.drawState
      val layout = entity.layout ?: createComponent(LayoutComponent::class.java, entity, item) ?: continue
      val needRelayout = drawState.layoutGeneration != config.layoutGeneration
      if (needRelayout) {
        drawState.visibility = false
        layout.visibility = layouter.preLayout(item, currentTimeMills, displayer, config)
      }
      if (layout.visibility) {
        // 优化: 使用drawState作为锁对象,更合理
        synchronized(drawState) {
          if (item.state < ItemState.Rendering) {
            needSync = true
            item.state = ItemState.Rendering
            cacheManager.requestBuildCache(item, displayer, config)
          }
        }
        layouter.layout(item, currentTimeMills, displayer, config)
        drawState.layoutGeneration = config.layoutGeneration
      }
      layout.position.set(drawState.positionX, drawState.positionY)
    }
    if (isPaused) {
      if (needSync) {
        cacheManager.requestBuildSign()
      } else {
        config.updateRender()
        layoutGeneration = config.layoutGeneration
      }
    }
    endTrace()
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {}

  override fun entityRemoved(entity: Entity) {
    super.entityRemoved(entity)
    layouter.remove(entity.dataComponent?.item ?: return)
  }

  private inner class Verifier : DanmakuRetainer.Verifier {

    var filterGeneration = -1
    var layoutFilters = emptyList<DanmakuLayoutFilter>()

    override fun skipLayout(item: DanmakuItem, willHit: Boolean): Boolean {
      return layoutFilters.any {
        it.filter(
          item,
          willHit,
          danmakuTimer.currentTimeMs,
          danmakuContext.config
        )
      }
    }

    override fun skipDraw(
      item: DanmakuItem,
      topMargin: Float,
      lines: Int,
      willHit: Boolean
    ): Boolean =
      false

  }
}
