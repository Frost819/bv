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

package com.kuaishou.akdanmaku.utils

import com.badlogic.ashley.core.Family
import com.kuaishou.akdanmaku.ecs.component.FilterResultComponent
import com.kuaishou.akdanmaku.ecs.component.ItemDataComponent
import com.kuaishou.akdanmaku.ecs.component.LayoutComponent
import com.kuaishou.akdanmaku.ecs.component.action.ActionComponent

/**
 * 各子系统所需要过滤的 Component 类型组成的 Family 常量
 */
object Families {
  val dataFamily: Family = Family.all(ItemDataComponent::class.java).get()

  val layoutComponentTypes = arrayOf(
    ItemDataComponent::class.java,
    FilterResultComponent::class.java
  )

  val renderFamily: Family = Family.all(
    ItemDataComponent::class.java,
    FilterResultComponent::class.java
  ).one(
    LayoutComponent::class.java,
    ActionComponent::class.java
  ).get()
}
