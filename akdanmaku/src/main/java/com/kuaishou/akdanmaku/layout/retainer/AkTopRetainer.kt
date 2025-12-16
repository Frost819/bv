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

package com.kuaishou.akdanmaku.layout.retainer

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.collection.OrderedRangeList
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ext.*
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer

/**
 * 顶部维持器，用来持有顶端对齐的弹幕（Top/Rolling）的垂直位置，并给出适当的高度
 *
 * @author Xana
 * @since 2021-07-05
 */
internal class AkTopRetainer(
  private val startRatio: Float = 1f,
  private val endRatio: Float = 1f
) : DanmakuRetainer {

  private val ranges = OrderedRangeList<DanmakuItem>(0, 0)

  override fun layout(
    drawItem: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Float {
    val drawState = drawItem.drawState
    val danmaku = drawItem.data
    val duration = if (danmaku.mode == DanmakuItemData.DANMAKU_MODE_ROLLING) config.rollingDurationMs
    else config.durationMs
    if (drawItem.isOutside(currentTimeMills)) {
      remove(drawItem)
      return -1f
    }
    val needRelayout = drawState.layoutGeneration != config.layoutGeneration
    val isRunning = ranges.contains(drawItem)
    val topPos: Int
    val visibility: Boolean
    if (needRelayout && !isRunning) {
      var holder = ranges.find(drawState.height.toInt()) {
        it == null || !it.willCollision(
          drawItem,
          displayer,
          currentTimeMills,
          duration
        )
      }

      if (holder.isEmpty()) {
        if (config.allowOverlap) {
          // empty and allow overlap means clean all holder and rearrange it from beginning
          ranges.clear()
          holder = ranges.find(drawState.height.toInt()) { it == null }
        } else if (drawItem.data.isImportant) {
          holder = ranges.min(drawState.height.toInt()) { (it?.drawState?.rect?.left ?: displayer.width).toInt() }
        }
      }
      visibility = if (holder.isEmpty()) {
        topPos = -1
        false
      } else {
        topPos = holder.first().start
        ranges.add(holder, topPos, topPos + drawState.height.toInt(), drawItem)
      }
    } else {
      visibility = drawState.visibility
      topPos = drawItem.drawState.positionY.toInt()
    }

    drawState.layoutGeneration = config.layoutGeneration
    drawState.visibility = visibility
    if (!visibility) return -1f
    drawItem.drawState.positionY = topPos.toFloat()
    return topPos.toFloat()
  }

  override fun clear() {
    ranges.clear()
  }

  override fun remove(item: DanmakuItem) {
    ranges.remove(item)
  }

  override fun update(start: Int, end: Int) {
    ranges.update((start * startRatio).toInt(), (end * endRatio).toInt())
  }
}
