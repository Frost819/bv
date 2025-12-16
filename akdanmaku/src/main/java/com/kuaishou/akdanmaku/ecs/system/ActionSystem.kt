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

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.base.DanmakuSortedSystem
import com.kuaishou.akdanmaku.ecs.component.action.ActionComponent
import com.kuaishou.akdanmaku.ext.action
import com.kuaishou.akdanmaku.ext.currentTimeMs
import com.kuaishou.akdanmaku.ext.dataComponent

/**
 * 动画系统
 *
 * @author Xana
 * @since 2021-07-14
 */
internal class ActionSystem(context: DanmakuContext) :
  DanmakuSortedSystem(context, Family.one(ActionComponent::class.java).get()) {

  override fun processEntity(entity: Entity, deltaTime: Float) {
    val item = entity.dataComponent?.item ?: return
    val actionComponent = entity.action ?: return
    val timeMills = currentTimeMs
    if (timeMills >= item.data.position) {
      actionComponent.visibility = true
      actionComponent.act(currentTimeMs - item.data.position)
    } else {
      actionComponent.visibility = false
    }
  }
}
