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

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.graphics.withTranslation
import com.badlogic.gdx.utils.Pool
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.cache.DrawingCache
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.ItemState
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.base.DanmakuEntitySystem
import com.kuaishou.akdanmaku.ext.*
import com.kuaishou.akdanmaku.inDebugMode
import com.kuaishou.akdanmaku.render.RenderObject
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.ui.DanmakuListener
import com.kuaishou.akdanmaku.utils.Families
import com.kuaishou.akdanmaku.utils.Fraction
import com.kuaishou.akdanmaku.utils.ObjectPool

/**
 * 渲染系统
 * - 生成 RenderObject 列表
 *
 * 此系统不需要排序
 *
 * @author Xana
 * @since 2021-06-23
 */
internal class RenderSystem(context: DanmakuContext) : DanmakuEntitySystem(context),
  Handler.Callback {

  private val entities by lazy { engine.getEntitiesFor(Families.renderFamily) }

  private val renderObjectPool =
    RenderObjectPool(INITIAL_POOL_SIZE, MAX_RENDER_OBJECT_POOL_SIZE).apply {
      fill(INITIAL_POOL_SIZE)
    }
  private var pendingDiscardResults = mutableListOf<RenderResult>()
  private var lastAllGeneration = 0

  private var renderResult: RenderResult? = null
  private val drawPaint = Paint().apply {
    isAntiAlias = true
  }
  private val callbackHandler = Handler(requireNotNull(Looper.myLooper()), this)
  private var resultGeneration = 0

  internal var listener: DanmakuListener? = null

  var cacheHit = Fraction(1, 1)

  override fun update(deltaTime: Float) {
    val config = danmakuContext.config
    if (isPaused && config.allGeneration == lastAllGeneration) {
      // 暂停，且没有任何配置变化时不进行 RenderObject 的更新
      return
    }

    if (isPaused) {
      Log.d(DanmakuEngine.TAG, "[Render] update on pause")
    }
    startTrace("RenderSystem_update")
    lastAllGeneration = config.allGeneration
    releaseDiscardResults()

    val newRenderObjects = entities
      .filter { entity ->
        val item = entity.dataComponent?.item ?: return@filter false
        val drawState = item.drawState
        entity.filter?.filtered == false &&
          item.state >= ItemState.Measured &&
          drawState.visibility &&
          drawState.measureGeneration == config.measureGeneration &&
          drawState.layoutGeneration == config.layoutGeneration
      }
      .let { filteredEntities ->
        // 优化：限制同屏最大弹幕数量，避免过载
        if (config.maxOnScreenDanmaku > 0 && filteredEntities.size > config.maxOnScreenDanmaku) {
          // 优先保留最新的弹幕
          filteredEntities.drop(filteredEntities.size - config.maxOnScreenDanmaku)
        } else {
          filteredEntities
        }
      }
      .mapNotNullTo(ArrayList(entities.size())) { entity ->
        val item = entity.dataComponent?.item ?: return@mapNotNullTo null
        val drawState = item.drawState
        val cache = item.drawState.drawingCache
        val action = entity.action
        if (listener != null && item.shownGeneration != config.firstShownGeneration) {
          item.shownGeneration = config.firstShownGeneration
          callbackHandler.obtainMessage(MSG_DANMAKU_SHOWN, item).sendToTarget()
        }
        renderObjectPool.obtain().apply {
          cache.increaseReference()
          this.item = item
          drawingCache = cache
          transform.reset()
          if (action != null) {
            position.set(action.position)
            rect.setEmpty()
            action.toTransformMatrix(transform)
            alpha = action.alpha
            transform.postConcat(drawState.transform)
          } else {
            transform.set(drawState.transform)
          }
          position.set(drawState.positionX, drawState.positionY)
          rect.set(drawState.rect)
          if (item.isHolding) {
            alpha = 1f
            holding = true
          }
        }
      }

    synchronized(this) {
      renderResult?.let {
        pendingDiscardResults.add(it)
      }
      renderResult = RenderResult(
        newRenderObjects,
        resultGeneration++,
        config.visibilityGeneration
      )
    }
    endTrace()
  }

  override fun release() {
    renderResult?.let { pendingDiscardResults.add(it) }
    renderResult = null
    releaseDiscardResults()
  }

  private fun releaseDiscardResults() {
    val results = synchronized(this) {
      val results = pendingDiscardResults.toList()
      pendingDiscardResults.clear()
      results
    }

    results.forEach {
      it.renderObjects.forEach(renderObjectPool::free)
    }
  }

  private var lastRenderGeneration = -1

  private val debugPaint: Paint? by lazy {
    if (inDebugMode) {
      Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 2f
      }
    } else null
  }
  private var lastDrawTime = 0L

  /**
   * 由主线程在 onDraw 时调用
   */
  @MainThread
  fun draw(canvas: Canvas, onRenderReady: () -> Unit) {
    val startTime = SystemClock.elapsedRealtime()
    val interval = startTime - lastDrawTime
    // 调用 renderSystem 的 draw 方法完成 renderObjectList 遍历 draw
    val renderResult = renderResult
    // 准备好 draw 的物料后通知 unlock action task.
    startTrace("notify_monitor")
    onRenderReady()
    endTrace()
    val config = danmakuContext.config
    // 不可见时直接不绘图
    if (!config.visibility) {
      return
    }
    if (renderResult == null ||
      renderResult.visibilityGeneration != config.visibilityGeneration
    ) {
      return
    }

    if (renderResult.renderObjects.isEmpty()) {
      lastRenderGeneration = renderResult.renderGeneration
      return
    }

    startTrace("RenderSystem_draw")
    val renderGeneration = renderResult.renderGeneration
    // 继续 draw 流程
    val skipped = renderGeneration - lastRenderGeneration - 1
    if (!isPaused) {
      when {
        skipped > 0 -> {
          Log.w(DanmakuEngine.TAG, "[Engine] skipped $skipped frames results")
        }
        renderGeneration == lastRenderGeneration && !isPaused -> {
          Log.w(DanmakuEngine.TAG, "[Engine] render same frame")
        }
      }
    }
    lastRenderGeneration = renderGeneration
    var cacheHitCount = 0
    try {
      var holdingObj: RenderObject? = null
      val displayer = danmakuDisplayer
      // 优化: 使用for循环代替forEach,避免迭代器创建
      val renderObjects = renderResult.renderObjects
      for (i in renderObjects.indices) {
        val renderObj = renderObjects[i]
        debugPaint?.let {
          canvas.drawRect(renderObj.rect, it)
        }
        if (renderObj.holding) {
          holdingObj = renderObj
          continue
        }
        drawPaint.alpha = (config.alpha * renderObj.alpha * 255).toInt()
        if (drawRenderObject(canvas, renderObj, displayer, config)) {
          cacheHitCount++
        }
      }
      holdingObj?.let { obj ->
        drawPaint.alpha = 255
        if (drawRenderObject(canvas, obj, displayer, config)) {
          cacheHitCount++
        }
      }
    } catch (e: Exception) {
      Log.e(DanmakuEngine.TAG, "[Exception] onDraw", e)
    }
    val cost = SystemClock.elapsedRealtime() - startTime
    if (!isPaused && (cost > OVERLOAD_INTERVAL)) {
      Log.w(DanmakuEngine.TAG, "[RenderSystem][DRAW] OVERLOAD! interval: $interval, cost: $cost")
    }
    lastDrawTime = startTime
    cacheHit.num = cacheHitCount
    cacheHit.den = renderResult.renderObjects.size
    endTrace()
  }

  fun getDanmakus(point: Point): List<DanmakuItem>? {
    if (!danmakuContext.config.visibility) return null
    val renderResult = this.renderResult ?: return null
    return renderResult.renderObjects.mapNotNull { r ->
      if (r.rect.contains(point.x.toFloat(), point.y.toFloat())) r.item else null
    }
  }

  fun getDanmakus(rect: RectF): List<DanmakuItem>? {
    if (!danmakuContext.config.visibility) return null
    val renderResult = this.renderResult ?: return null
    return renderResult.renderObjects.mapNotNull { r ->
      if (r.rect.intersects(rect.left, rect.top, rect.right, rect.bottom)) r.item else null
    }
  }

  private fun drawRenderObject(
    canvas: Canvas,
    obj: RenderObject,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Boolean {
    return if (obj.drawingCache != DrawingCache.EMPTY_DRAWING_CACHE &&
      obj.drawingCache.get() != null &&
      obj.item.drawState.cacheGeneration == config.cacheGeneration &&
      obj.item.state >= ItemState.Rendered) {
      obj.drawingCache.get()?.bitmap?.let {
        if (it.isRecycled) return false
        canvas.drawBitmap(it, obj.transform, drawPaint)
        true
      } ?: false
    } else {
      // 缓存未命中时降级绘制,避免弹幕消失
      canvas.withTranslation(obj.position.x, obj.position.y) {
        danmakuContext.renderer.updatePaint(obj.item, displayer, config)
        danmakuContext.renderer.draw(obj.item, canvas, displayer, config)
      }
      false
    }
  }

  private inner class RenderObjectPool(initialCapacity: Int, max: Int) :
    Pool<RenderObject>(initialCapacity, max) {

    override fun newObject(): RenderObject = RenderObject(
      DanmakuItem.DANMAKU_ITEM_EMPTY,
      DrawingCache.EMPTY_DRAWING_CACHE,
      ObjectPool.obtainPointF(),
      ObjectPool.obtainRectF(),
      ObjectPool.obtainMatrix()  // 优化: 使用对象池
    )

    override fun reset(obj: RenderObject?) {
      if (obj == null) return
      if (obj.drawingCache != DrawingCache.EMPTY_DRAWING_CACHE) {
        obj.drawingCache.decreaseReference()
      }
      with(obj) {
        item = DanmakuItem.DANMAKU_ITEM_EMPTY
        drawingCache = DrawingCache.EMPTY_DRAWING_CACHE
        rect.setEmpty()
        position.set(0f, 0f)
        transform.reset()
        alpha = 1f
        holding = false
      }
      // 优化: 不释放Matrix,由对象池管理
      // ObjectPool.releaseMatrix(obj.transform)
    }
  }

  override fun handleMessage(msg: Message): Boolean {
    val listener = this.listener ?: return false
    if (msg.what == MSG_DANMAKU_SHOWN) {
      val item = msg.obj as? DanmakuItem ?: return false
      listener.onDanmakuShown(item)
    }
    return false
  }

  private class RenderResult(
    val renderObjects: List<RenderObject>,
    val renderGeneration: Int,
    val visibilityGeneration: Int
  )

  companion object {
    // 优化：增加对象池容量以应对密集弹幕场景
    private const val MAX_RENDER_OBJECT_POOL_SIZE = 1000  // 从500增加到1000
    private const val INITIAL_POOL_SIZE = 300  // 从200增加到300

    // 优化：提高过载检测阈值，减少误报
    private const val OVERLOAD_INTERVAL = 33  // 从20ms提高到33ms (约30fps)

    private const val MSG_DANMAKU_SHOWN = 0x01
  }
}
