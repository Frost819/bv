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
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.layout.retainer.DanmakuRetainer.Locator
import com.kuaishou.akdanmaku.layout.retainer.DanmakuRetainer.Verifier
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import java.util.*
import kotlin.math.abs

/**
 * 弹幕保持器，此处定义了保持器所需要的各接口
 * - [Locator] 根据给定弹幕和对应的 y 坐标
 * - [Verifier] 在排布时决定是否要跳过 layout 或 draw
 * - [DanmakuRetainer] 保持器，每个层级（Top/Bottom/Rolling）一个实例，用于排布弹幕的高度
 *
 * @author Xana
 * @since 2021-06-22
 */
internal interface DanmakuRetainer {

  fun layout(
    drawItem: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Float

  fun clear()

  fun remove(item: DanmakuItem)

  fun update(start: Int, end: Int)

  /**
   * 对弹幕在确定 Y 左边时进行 X 坐标的定位计算
   */
  interface Locator {
    fun layout(
      item: DanmakuItem,
      currentTimeMills: Long,
      displayer: DanmakuDisplayer,
      config: DanmakuConfig
    )
  }

  interface Verifier {
    fun skipLayout(item: DanmakuItem, willHit: Boolean): Boolean

    fun skipDraw(item: DanmakuItem, topMargin: Float, lines: Int, willHit: Boolean): Boolean
  }

  class RangeHolder(
    var holder: SpaceHolder?,
    var range: IntRange
  )

  class RetainerState(
    var lines: Int = 0,
    var insertEntity: SpaceHolder? = null,
    var firstEntity: SpaceHolder? = null,
    var lastEntity: SpaceHolder? = null,
    var minRightRow: SpaceHolder? = null,
    var removeEntity: SpaceHolder? = null,
    var overwriteInsert: Boolean = false,
    var shown: Boolean = false,
    var willHit: Boolean = false,
    var found: Boolean = false
  )

  class SpaceHolder(
    val item: DanmakuItem,
    val position: Long,
    var top: Int,
    var left: Int,
    val width: Int,
    val height: Int,
    var index: Int = -1,
    val mode: Int = 0,
    var positionOffset: Long = 0
  ) {
    val bottom: Int
      get() = top + height

    val right: Int
      get() = left + width

    override fun equals(other: Any?): Boolean {
      return (other as? SpaceHolder)?.item == item
    }



    override fun toString(): String {
      return "{time: $position, range: [$top..${top + height}], index: $index, w: $width}"
    }

    val timePosition: Long
      get() = position + positionOffset

    fun isTimeout(currentTimeMills: Long, durationMs: Long) =
      (currentTimeMills - timePosition) > durationMs

    fun isLate(currentTimeMills: Long) = currentTimeMills - timePosition < 0

    fun isOutside(currentTimeMills: Long, durationMs: Long) =
      isTimeout(currentTimeMills, durationMs) || isLate(currentTimeMills)

    fun willCollision(
      holder: SpaceHolder,
      displayer: DanmakuDisplayer,
      currentTimeMills: Long,
      durationMills: Long
    ): Boolean {
      if (isOutside(currentTimeMills, durationMills)) {
        return false
      }
      val dt = holder.timePosition - timePosition
      if (dt <= 0) return true
      if (abs(dt) >= durationMills ||
        isTimeout(currentTimeMills, durationMills) ||
        holder.isTimeout(currentTimeMills, durationMills)
      ) {
        return false
      }
      if (mode == DanmakuItemData.DANMAKU_MODE_CENTER_TOP || mode == DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM) {
        return true
      }

      return checkCollisionAtTime(this, holder, displayer, currentTimeMills, durationMills) ||
        checkCollisionAtTime(this, holder, displayer, currentTimeMills + durationMills, durationMills)
    }

    override fun hashCode(): Int {
      var result = item.hashCode()
      result = 31 * result + position.hashCode()
      result = 31 * result + top
      result = 31 * result + left
      result = 31 * result + width
      result = 31 * result + height
      result = 31 * result + index
      result = 31 * result + mode
      result = 31 * result + positionOffset.hashCode()
      result = 31 * result + bottom
      result = 31 * result + right
      result = 31 * result + timePosition.hashCode()
      return result
    }

    companion object {

      private fun checkCollisionAtTime(
        h1: SpaceHolder,
        h2: SpaceHolder,
        displayer: DanmakuDisplayer,
        current: Long,
        duration: Long
      ): Boolean {
        val width = displayer.width
        val w1 = h1.width
        val w2 = h2.width
        val dt1 = current - h1.timePosition
        val r1 = width - (width + w1) * (dt1.toFloat() / duration) + w1
        val dt2 = current - h2.timePosition
        val l2 = width - (width + w2) * (dt2.toFloat() / duration)
        return l2 < r1
        // 这是一个用于加速运算的简化公式，但是没有经过验证
        // return current * (w2 - w1) + d1.position * (width + w1) - d2.position * (width + w2) - w1 * duration <= 0
      }
    }
  }

  /**
   * Y 轴排序
   */
  class YPosComparator : Comparator<SpaceHolder> {
    override fun compare(o1: SpaceHolder, o2: SpaceHolder): Int {
      return o1.top - o2.top
    }
  }

  /**
   * Y 轴逆序排序
   */
  class YPosDescComparator : Comparator<SpaceHolder> {
    override fun compare(o1: SpaceHolder, o2: SpaceHolder): Int {
      return o2.top - o1.top
    }
  }
}
