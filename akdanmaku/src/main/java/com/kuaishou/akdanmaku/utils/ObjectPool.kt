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

import android.graphics.PointF
import android.graphics.RectF
import androidx.core.util.Pools
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ui.DanmakuPlayer

/**
 * 对象池
 *
 * @author Xana
 * @since 2021-07-07
 */
object ObjectPool {

  private val rectPool = Pools.SimplePool<RectF>(200)
  private val pointPool = Pools.SimplePool<PointF>(200)
  // 优化: 添加Matrix对象池
  private val matrixPool = Pools.SimplePool<android.graphics.Matrix>(200)
  private val itemPool = Pools.SimplePool<DanmakuItem>(1500) // 从1000增加到1500

  private fun DanmakuItem.bind(data: DanmakuItemData, timer: DanmakuTimer) {
    this.data = data
    this.timer = timer
  }

  fun obtainRectF(): RectF = rectPool.acquire() ?: RectF()

  fun releaseRectF(rectF: RectF) {
    if (rectPool.release(rectF)) {
      rectF.setEmpty()
    }
  }

  fun obtainPointF(): PointF = pointPool.acquire() ?: PointF()

  fun releasePointF(point: PointF) {
    if (pointPool.release(point)) {
      point.set(0f, 0f)
    }
  }

  // 优化: Matrix对象池方法
  fun obtainMatrix(): android.graphics.Matrix = matrixPool.acquire() ?: android.graphics.Matrix()

  fun releaseMatrix(matrix: android.graphics.Matrix) {
    if (matrixPool.release(matrix)) {
      matrix.reset()
    }
  }

  internal fun obtainItem(data: DanmakuItemData, player: DanmakuPlayer): DanmakuItem {
    val timer = player.engine.timer
    return itemPool.acquire()?.apply { bind(data, timer) } ?: DanmakuItem(data, player)
  }

  internal fun obtainItem(data: DanmakuItemData, engine: DanmakuEngine): DanmakuItem {
    val timer = engine.timer
    return itemPool.acquire()?.apply { bind(data, timer) } ?: DanmakuItem(data).also {
      it.timer = timer
    }
  }

  fun releaseItem(item: DanmakuItem) {
    if (itemPool.release(item)) {
      item.recycle()
    }
  }
}
