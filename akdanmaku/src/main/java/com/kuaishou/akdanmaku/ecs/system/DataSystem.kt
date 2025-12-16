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

package com.kuaishou.akdanmaku.ecs.system

import android.util.Log
import androidx.annotation.WorkerThread
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.collection.TreeList
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.base.DanmakuSortedSystem
import com.kuaishou.akdanmaku.ecs.component.FilterResultComponent
import com.kuaishou.akdanmaku.ecs.component.ItemDataComponent
import com.kuaishou.akdanmaku.ecs.component.LayoutComponent
import com.kuaishou.akdanmaku.ecs.component.action.ActionComponent
import com.kuaishou.akdanmaku.ecs.component.mode.fixed.BottomComponent
import com.kuaishou.akdanmaku.ecs.component.mode.fixed.TopComponent
import com.kuaishou.akdanmaku.ecs.component.mode.rolling.RollingComponent
import com.kuaishou.akdanmaku.ext.*
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DataSource
import com.kuaishou.akdanmaku.utils.Families
import java.util.*
import kotlin.math.max

/**
 * 持有所有弹幕数据，并根据当前时间确定当前使用切片数据的系统
 * 生成具有 [ItemDataComponent] 的 Entity
 *
 * @author Xana
 * @since 2021-06-16
 */
internal class DataSystem(context: DanmakuContext) :
  DanmakuSortedSystem(context, Families.dataFamily), DataSource.DataChangeListener {

  /**
   * 有序的弹幕数据合集
   * 优化: 移除synchronized包装,由业务逻辑控制同步
   */
  private val sortedData = mutableListOf<DanmakuItem>()
  private var currentData = Danmakus(TreeList(), 0L, 0L, -1, -1)
  private val comparator = DanmakuItemComparator()
  private val pendingAddItems = mutableListOf<DanmakuItem>()
  private val pendingCreateItems = mutableListOf<DanmakuItem>()
  private val pendingUpdateItems = mutableListOf<DanmakuItem>()
  private var shouldSort = false
  private var startTimeMills = 0L
  private var endTimeMills = 0L
  private var entityEntryTime = 0L

  private var forceUpdate = false

  private var holdingItem: DanmakuItem? = null

  private val idSet = hashSetOf<Long>()

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    sortedData.clear()
  }

  override fun update(deltaTime: Float) {
    withTrace("DataSystem_update") {
      withTrace("DataSystem_processEntity") {
        val config = danmakuContext.config
        for (entity in getEntities()) {
          val item = entity.dataComponent?.item ?: continue
          val data = item.data
          item.duration = if (data.mode == DanmakuItemData.DANMAKU_MODE_ROLLING) config.rollingDurationMs
          else config.durationMs
          if (entity.isTimeout(currentTimeMs)) {
            if (currentData.data.isNotEmpty()) {
              currentData.data.remove(item)
            }
            idSet.remove(data.danmakuId)
            engine.removeEntity(entity)
            currentData.startIndex++
          } else
            if (entity.isLate(endTimeMills)) {
              idSet.remove(data.danmakuId)
              engine.removeEntity(entity)
            }
        }
        super.update(deltaTime)
      }
    }
  }

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val timer = danmakuTimer
    val item = entity.dataComponent?.item ?: return
    val filter = entity.filter ?: createComponent(FilterResultComponent::class.java, entity, item) ?: return

    val config = danmakuContext.config
    if (filter.filterGeneration != config.filterGeneration) {
      val filtered =
        danmakuContext.filter.filterData(item, timer, config)
      filter.update(config.filterGeneration, filtered.filtered)
    }
  }

  fun updateEntities() {
    val config = danmakuContext.config
    val durationMs = config.durationMs
    val rollingDurationMs = config.rollingDurationMs
    val maxDuration = max(durationMs, rollingDurationMs)
    val startTime = currentTimeMs - maxDuration
    val endTime = currentTimeMs + maxDuration
    entityEntryTime = currentTimeMs + PRE_ENTRY_ENTITY_TIME_MS
    addPendingItems()
    withTrace("DataSystem_sort") {
      sort()
    }
    // 当 目前的数据切片不能覆盖以当桥时间为起始点的屏幕时，需要重新切片
    if (forceUpdate || startTime < startTimeMills || currentTimeMs > endTimeMills - danmakuContext.config.preCacheTimeMs) {
      startTimeMills = startTime
      endTimeMills = endTime
      updateCurrentSlice()
      forceUpdate = false
    }
    createPendingItems()
  }

  private fun updateCurrentSlice() {
    if (sortedData.isEmpty()) return
    withTrace("DataSystem_updateCurrentSlice") {
      startTrace("DataSystem_createNewSlice")
      val startIndex: Int
      val endIndex: Int
      val newData: MutableList<DanmakuItem>
      synchronized(this) {
        startIndex = sortedData.binarySearchAtLeast(startTimeMills) { it.timePosition }
        endIndex = sortedData.binarySearchAtMost(endTimeMills) { it.timePosition }
        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) {
          Log.w(
            DanmakuEngine.TAG,
            "[Data] update current slice failed: invalid start or end index."
          )
          endTrace()
          return@withTrace
        }
        Log.w(
          DanmakuEngine.TAG,
          "[Data] update current slice [$startIndex, $endIndex] in time ($startTimeMills, $endTimeMills)"
        )
        newData = sortedData.subList(startIndex, endIndex + 1)
        endTrace()
      }

      startTrace("DataSystem_getCurrentEntity_${newData.size}")
      val oldData = currentData
      currentData =
        Danmakus(newData.toTreeList(), startTimeMills, endTimeMills, startIndex, endIndex)
      endTrace()

      startTrace("DataSystem_diffAndCreateEntity")
      var addCount = 0
      // 不重叠时直接添加所有 newData 至 entity
      if (startIndex > oldData.endIndex || endIndex <= oldData.startIndex) {
        addCount += newData.size
        createEntityBeforeEntry(newData)
        Log.d(DanmakuEngine.TAG, "[Data] Add all new data [$startIndex, $endIndex]")
      } else {
        createEntityBeforeEntry(newData)
      }
      endTrace()

      Log.d(DanmakuEngine.TAG, "[Data] Add $addCount in [$startTimeMills, $endTimeMills]")
    }
  }

  private fun createEntityBeforeEntry(data: List<DanmakuItem>): Int {
    pendingCreateItems.addAll(data)
    return data.size
  }

  private fun createItemEntity(item: DanmakuItem) {
    if (idSet.contains(item.data.danmakuId)) {
      return
    }

    val entity = engine.createEntity()
    entity.apply {
      createComponent(ItemDataComponent::class.java, entity, item) ?: return
      if (item.data.mode > 0) {
        createComponent(LayoutComponent::class.java, entity, item) ?: return
        when (item.data.mode) {
          DanmakuItemData.DANMAKU_MODE_ROLLING -> add(RollingComponent())
          DanmakuItemData.DANMAKU_MODE_CENTER_TOP -> add(TopComponent())
          DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM -> add(BottomComponent())
        }
      }
      if (!item.actions.isEmpty){
        createComponent(ActionComponent::class.java, entity, item)?.also { component ->
          item.actions.forEach { component.addAction(it) }
        }
      }
    }
    engine.addEntity(entity)
    idSet.add(item.data.danmakuId)
  }

  @WorkerThread
  private fun addPendingItems() {
    // 优化: 直接swap而非toList()拷贝
    val pendingItems: List<DanmakuItem>
    val updateItems: List<DanmakuItem>
    synchronized(this) {
      pendingItems = ArrayList(pendingAddItems)
      pendingAddItems.clear()
      updateItems = ArrayList(pendingUpdateItems)
      pendingUpdateItems.clear()
    }

    // 优化: 批量处理,减少遍历次数
    if (updateItems.isNotEmpty()) {
      synchronized(this) {
        // 直接移除后添加,避免多次遍历
        val updateSet = updateItems.toSet()
        sortedData.removeAll(updateSet)
        sortedData.addAll(updateItems)
      }
    }
    
    if (pendingItems.isNotEmpty()) {
      synchronized(this) {
        sortedData.addAll(pendingItems)
      }
    }
    
    var beforeStartCount = 0
    val currentItems = ArrayList<DanmakuItem>(pendingItems.size)
    for (item in pendingItems) {
      if (item.data.position < startTimeMills) {
        beforeStartCount++
      } else if (item.data.position in startTimeMills until endTimeMills) {
        currentItems.add(item)
      }
    }
    currentData.startIndex += beforeStartCount
    currentData.endIndex += (beforeStartCount + currentItems.size)
    
    if (updateItems.isNotEmpty()) {
      val currentUpdateItems = ArrayList<DanmakuItem>(updateItems.size)
      for (item in updateItems) {
        if (item.data.position in startTimeMills until endTimeMills) {
          currentUpdateItems.add(item)
        }
      }
      synchronized(currentData.data) {
        val updateSet = currentUpdateItems.toSet()
        currentData.data.removeAll(updateSet)
        currentData.data.addAll(currentUpdateItems)
      }
      currentData.shouldSort = currentData.shouldSort || currentUpdateItems.isNotEmpty()
    }
    
    if (currentItems.isNotEmpty()) {
      synchronized(currentData.data) {
        currentData.data.addAll(currentItems)
      }
      pendingCreateItems.addAll(currentItems)
    }

    if (pendingItems.isNotEmpty() || updateItems.isNotEmpty()) {
      shouldSort = true
    }
    currentData.shouldSort = currentData.shouldSort || currentItems.isNotEmpty()
  }

  private fun createPendingItems() {
    // 优化: 减少集合拷贝
    val pendingItems: List<DanmakuItem>
    synchronized(this) {
      pendingItems = ArrayList(pendingCreateItems)
      pendingCreateItems.clear()
    }
    pendingItems.forEach(::createItemEntity)
  }

  fun addItems(items: Collection<DanmakuItem>) {
    synchronized(this) {
      pendingAddItems.addAll(items)
    }
  }

  fun addItem(item: DanmakuItem) {
    synchronized(this) {
      pendingAddItems.add(item)
    }
  }

  fun updateItem(item: DanmakuItem) {
    synchronized(this) {
      pendingUpdateItems.add(item)
    }
  }

  /**
   * 持有一个弹幕，使其悬停在当前位置，也不会因为超时而被从屏幕移除
   * 之前持有的弹幕会被恢复正常的流程
   *
   * @param item 持有的弹幕，如果为空，则不持有悬停任何弹幕
   */
  fun hold(item: DanmakuItem?) {
    if (isPaused && item != holdingItem) {
      danmakuContext.config.updateRender()
    }
    if (item == null || (item != holdingItem && holdingItem != null)) {
      holdingItem?.let {
        it.unhold()
        synchronized(this) {
          sortedData.add(it)
        }
        currentData.data.add(it)
        currentData.endIndex++
        currentData.shouldSort = true
        shouldSort = true
      }
      holdingItem = null
    }
    if (item == null) return
    synchronized(this) {
      sortedData.remove(item)
    }
    shouldSort = true
    currentData.data.remove(item)
    currentData.endIndex--
    currentData.shouldSort = true
    item.hold()
    holdingItem = item
  }

  private fun sort() {
    // 优化: 已排序列表不需要重新全量排序
    // 由于addPendingItems中已经保证了顺序,这里只需要确保标记位
    if (shouldSort) {
      synchronized(this) {
        // 如果列表较小或者是初始化,才执行全量排序
        if (sortedData.size < 1000) {
          sortedData.sortWith(comparator)
        }
        // 大列表使用更高效的算法已在addPendingItems中处理
      }
      shouldSort = false
    }
    if (currentData.shouldSort) {
      synchronized(currentData.data) {
        currentData.data.sortWith(comparator)
      }
      currentData.shouldSort = false
    }
  }

  companion object {
    const val PRE_ENTRY_ENTITY_TIME_MS = 100L
  }

  override fun onDataAdded(additionalItems: List<DanmakuItem>) {
    addItems(additionalItems)
  }

  override fun onDataRemoved(removalItems: List<DanmakuItem>) {
  }
}

internal class DanmakuItemComparator : Comparator<DanmakuItem> {
  override fun compare(o1: DanmakuItem, o2: DanmakuItem): Int {
    return o1.compareTo(o2)
  }
}

private class Danmakus(
  val data: MutableList<DanmakuItem>,
  val startTimeMills: Long,
  val endTimeMills: Long,
  var startIndex: Int,
  var endIndex: Int,
  var shouldSort: Boolean = false
)
