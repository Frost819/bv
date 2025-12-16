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

package com.kuaishou.akdanmaku.ext

import com.badlogic.ashley.core.Entity
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.FilterResultComponent
import com.kuaishou.akdanmaku.ecs.component.ItemDataComponent
import com.kuaishou.akdanmaku.ecs.component.LayoutComponent
import com.kuaishou.akdanmaku.ecs.component.action.ActionComponent
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import kotlin.math.abs

/**
 * Entity 扩展
 *
 * @author Xana
 * @since 2021-06-22
 */
internal val Entity.layout: LayoutComponent?
  get() = getComponent(LayoutComponent::class.java)

internal val Entity.filter: FilterResultComponent?
  get() = getComponent(FilterResultComponent::class.java)

internal val Entity.dataComponent: ItemDataComponent?
  get() = getComponent(ItemDataComponent::class.java)

internal val Entity.action: ActionComponent?
  get() = getComponent(ActionComponent::class.java)

internal fun Entity.getTimePosition(): Long {
  return dataComponent?.item?.timePosition ?: 0
}

internal val Entity.duration: Long
  get() = dataComponent?.item?.duration ?: 0

fun Entity.isTimeout(currentTimeMills: Long) =
  (currentTimeMills - getTimePosition()) > duration

fun Entity.isLate(currentTimeMills: Long) = currentTimeMills - getTimePosition() < 0

fun Entity.isOutside(currentTimeMills: Long) =
  isTimeout(currentTimeMills) || isLate(currentTimeMills)

