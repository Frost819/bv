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

package com.kuaishou.akdanmaku.ecs.base

import com.badlogic.ashley.core.*
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import java.util.*

internal abstract class DanmakuSortedSystem(
  context: DanmakuContext,
  private val family: Family,
  private val comparator: Comparator<Entity> = DanmakuItemEntityComparator()
) : DanmakuEntitySystem(context), EntityListener {
  private val sortedEntities = mutableListOf<Entity>()
  private var shouldSort = false

  override fun addedToEngine(engine: Engine) {
    sortedEntities.clear()
    val newEntities = engine.getEntitiesFor(family)
    if (newEntities.size() > 0) {
      sortedEntities.addAll(newEntities)
    }
    sortedEntities.sortWith(comparator)
    shouldSort = false
    engine.addEntityListener(family, this)
  }

  override fun removedFromEngine(engine: Engine) {
    super.removedFromEngine(engine)
    engine.removeEntityListener(this)
    sortedEntities.clear()
    shouldSort = false
  }

  override fun entityAdded(entity: Entity) {
    sortedEntities.add(entity)
    shouldSort = true
  }

  override fun entityRemoved(entity: Entity) {
    sortedEntities.remove(entity)
    shouldSort = true
  }

  override fun update(deltaTime: Float) {
    sort()
    for (i in sortedEntities.indices) {
      processEntity(sortedEntities[i], deltaTime)
    }
  }

  fun getEntities(): List<Entity> {
    sort()
    return sortedEntities
  }

  private fun sort() {
    if (shouldSort) {
      sortedEntities.sortWith(comparator)
      shouldSort = false
    }
  }

  /**
   * This method is called on every entity on every update call of the EntitySystem. Override this to implement your system's
   * specific processing.
   * @param entity The current Entity being processed
   * @param deltaTime The delta time between the last and current frame
   */
  protected abstract fun processEntity(entity: Entity, deltaTime: Float)

  override fun release() {

  }
}

