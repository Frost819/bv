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
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.base.DanmakuBaseComponent
import com.kuaishou.akdanmaku.ecs.base.DanmakuEntitySystem
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.DanmakuTimer

internal val DanmakuEntitySystem.currentTimeMs: Long
  get() = danmakuTimer.currentTimeMs

internal val DanmakuEntitySystem.danmakuTimer: DanmakuTimer
  get() = danmakuContext.timer

internal val DanmakuEntitySystem.danmakuDisplayer: DanmakuDisplayer
  get() = danmakuContext.displayer

internal val DanmakuEntitySystem.isPaused: Boolean
  get() = (engine as? DanmakuEngine)?.isPaused ?: true

internal fun <T : DanmakuBaseComponent> DanmakuEntitySystem.createComponent(
  type: Class<T>,
  entity: Entity,
  item: DanmakuItem
): T? =
  engine?.createComponent(type)?.also {
    entity.add(it)
    it.item = item
  }

internal fun <T : DanmakuEntitySystem> createSystem(type: Class<T>, context: DanmakuContext): T {
  val constructor = requireNotNull(type.getConstructor(DanmakuContext::class.java)) {
    "DanmakuEntitySystem must have a constructor with DanmakuContext parameter"
  }
  return constructor.newInstance(context)
}
