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

package com.kuaishou.akdanmaku.ui

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Choreographer
import androidx.core.math.MathUtils.clamp
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.data.DataSource
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.system.DataSystem
import com.kuaishou.akdanmaku.ecs.system.RenderSystem
import com.kuaishou.akdanmaku.ext.endTrace
import com.kuaishou.akdanmaku.ext.startTrace
import com.kuaishou.akdanmaku.render.DanmakuRenderer
import com.kuaishou.akdanmaku.utils.Fraction
import com.kuaishou.akdanmaku.utils.ObjectPool
import java.lang.ref.WeakReference
import java.util.concurrent.Semaphore
import kotlin.math.max

/**
 *
 * 弹幕播放器，与 [DanmakuView] 形成类似于视频播放器的弹幕播放结构
 * 此类应当在共享同一个弹幕播放的场景间进行共享，它持有着
 * - 播放上下文（计时器，渲染器，缓存管理等）
 * - 渲染引擎
 * 在与一个 [DanmakuView] 绑定后会通过 [Choreographer] 进行帧同步，通过信号量在绘制和后台计算之间进行同步
 *
 * 内部具有一个用于执行计算的线程，几乎所有对外 API 均为同步返回，具体操作在此线程中进行
 *
 * @param renderer 业务端自定义的弹幕渲染器
 */
@Suppress("unused")
class DanmakuPlayer(renderer: DanmakuRenderer, dataSource: DataSource? = null) {

  companion object {
    internal const val MSG_FRAME_UPDATE = 2101
    internal const val NOTIFY_DISPLAYER_SIZE_CHANGE = 2201

    private const val PLAYER_WIDTH = 682
    const val MIN_DANMAKU_DURATION: Long = 4000
    const val MAX_DANMAKU_DURATION_HIGH_DENSITY: Long = 9000
    /**
     * 是否手动控制 Step 流程
     */
    var isManualStep = false
  }

  private var danmakuView: WeakReference<DanmakuView>? = null
  internal val engine = DanmakuEngine.get(renderer)
  private val actionThread by lazy  { HandlerThread("ActionThread").apply { start() } }
  private val actionHandler by lazy { ActionHandler(actionThread.looper) }
  private val frameCallback by lazy { FrameCallback(actionHandler) }

  private var currentDisplayerWidth = 0
  private var currentDisplayerHeight = 0
  private var currentDisplayerSizeFactor = 1f
  private var config: DanmakuConfig? = null

  private val drawSemaphore = Semaphore(0)

  @Volatile
  private var started = false

  private val dataSystem: DataSystem?
    get() = engine.getSystem(DataSystem::class.java)

  /**
   * 弹幕埋点所需的接口
   */
  var listener: DanmakuListener? = null
    set(value) {
      if (field != value) {
        field = value
        engine.getSystem(RenderSystem::class.java)?.listener = value
      }
    }

  @Volatile
  var isReleased: Boolean = false
    private set

  val cacheHit: Fraction?
    get() = engine.getSystem(RenderSystem::class.java)?.cacheHit

  init {
    dataSource?.setListener(dataSystem)
  }

  /**
   * 运行在[actionHandler] [actionThread] 线程中
   */
  private fun postFrameCallback() {
    Choreographer.getInstance().postFrameCallback(frameCallback)
  }

  private fun updateFrame(deltaTimeSeconds: Float? = null) {
    if (!started || isReleased) {
      return
    }

    if (isManualStep) {
      // Time goes one step for manual debug.
      engine.step(deltaTimeSeconds)
    } else {
      // Prepare next frameCallback.
      postFrameCallback()
      // update entities before system update
      engine.preAct()
      // Wait for acquiring a permit.
      drawSemaphore.acquire()
    }
    if (!started || isReleased) {
      return
    }
    startTrace("updateFrame")
    // Do work in actionThread.
    engine.act()
    // Post invalidate view to force onDraw's call on next frame.
    startTrace("postInvalidate")
    danmakuView?.get()?.postInvalidateOnAnimation()
    endTrace()
    endTrace()
  }

  internal fun draw(canvas: Canvas) {
    if (isReleased) {
      return
    }
    if (!isManualStep) {
      // Time goes one step.
      engine.step()
    }
    drawSemaphore.tryAcquire()
    if (!started) {
      releaseSemaphore()
      return
    }
    engine.draw(canvas) {
      releaseSemaphore()
    }
  }

  private fun releaseSemaphore() {
    // Acquired or on the first draw(with init permit: 0).
    if (drawSemaphore.availablePermits() == 0) {
      drawSemaphore.release()
    }
  }

  /**
   * For debug use, step manually.
   */
  fun step(deltaTimeMs: Int) {
    if (isManualStep) {
      actionHandler.obtainMessage(MSG_FRAME_UPDATE, deltaTimeMs, 0).sendToTarget()
    }
  }

  /**
   * 将播放器与一个 DanmakuView 绑定，前一个被绑定的会自动解锁。
   * 绑定后弹幕的绘制将在此 View 上进行
   */
  fun bindView(danmakuView: DanmakuView) {
    this.danmakuView?.get()?.danmakuPlayer = null
    this.danmakuView = WeakReference(danmakuView)
    danmakuView.danmakuPlayer = this
    engine.context.displayer = danmakuView.displayer
    notifyDisplayerSizeChanged(danmakuView.displayer.width, danmakuView.displayer.height)
    danmakuView.postInvalidate()
  }

  /**
   * 播放弹幕
   *
   * @param danmakuConfig 弹幕配置
   */
  fun start(danmakuConfig: DanmakuConfig? = null) {
    danmakuConfig?.let {
      updateConfig(it)
    }
    engine.start()
    if (!started) {
      started = true
      if (!isManualStep) {
        actionHandler.post { postFrameCallback() }
      }
    }
  }

  fun pause() {
    engine.pause()
  }

  fun stop() {
    engine.pause()
    seekTo(0)
  }

  /**
   * 释放弹幕播放器，释放后弹幕播放器将不再可用。
   */
  fun release() {
    if (isReleased) {
      return
    }
    danmakuView = null
    isReleased = true
    actionHandler.removeCallbacksAndMessages(null)
//    Choreographer.getInstance().removeFrameCallback(frameCallback)
    started = false
    actionThread.quitSafely()
    actionThread.join(50L)
    engine.release()
  }

  fun seekTo(positionMs: Long) {
    Log.d(DanmakuEngine.TAG, "[Player] SeekTo($positionMs)")
    getConfig()?.updateFirstShown()
    engine.seekTo(max(positionMs, 0L))
  }

  fun getCurrentTimeMs(): Long {
    return engine.getCurrentTimeMs()
  }

  fun updatePlaySpeed(speed: Float) {
    engine.updateTimerFactor(speed)
  }

  fun updateData(dataList: List<DanmakuItemData>): List<DanmakuItem> {
    val items = dataList.map { obtainItem(it) }
    dataSystem?.addItems(items)
    return items
  }

  /**
   * 弹幕目前统一的数据结构就是 DanmakuItem，他是 DanmakuItemData 的超集，也是被定义为
   * 可以进行扩展的
   */
  fun updateItems(items: List<DanmakuItem>) {
    dataSystem?.addItems(items)
  }

  fun send(danmaku: DanmakuItemData): DanmakuItem {
    val item = obtainItem(danmaku)
    dataSystem?.addItem(item)
    return item
  }

  fun send(item: DanmakuItem) {
    dataSystem?.addItem(item)
  }

  /**
   * 更新一个弹幕
   */
  fun updateItem(item: DanmakuItem) {
    dataSystem?.updateItem(item)
  }

  fun updateConfig(danmakuConfig: DanmakuConfig?) {
    config = danmakuConfig
    engine.updateConfig(danmakuConfig ?: return)
  }

  fun getConfig(): DanmakuConfig? = engine.getConfig()

  fun getDanmakusAtPoint(point: Point): List<DanmakuItem>? {
    return engine.getSystem(RenderSystem::class.java)?.getDanmakus(point)
  }

  fun getDanmakusInRect(hitRect: RectF): List<DanmakuItem>? {
    return engine.getSystem(RenderSystem::class.java)?.getDanmakus(hitRect)
  }

  fun hold(item: DanmakuItem?) {
    dataSystem?.hold(item)
  }

  fun obtainItem(danmaku: DanmakuItemData): DanmakuItem =
    ObjectPool.obtainItem(danmaku, this)

  fun releaseItem(item: DanmakuItem) {
    ObjectPool.releaseItem(item)
  }

  internal fun notifyDisplayerSizeChanged(width: Int, height: Int) {
    val displayer = engine.context.displayer
    updateViewportState(width, height, displayer.getViewportSizeFactor())
    updateMaxDanmakuDuration()
    if (displayer.width != width || displayer.height != height) {
      Log.d(DanmakuEngine.TAG, "notifyDisplayerSizeChanged($width, $height)")
      displayer.width = width
      displayer.height = height
      actionHandler.obtainMessage(NOTIFY_DISPLAYER_SIZE_CHANGE).sendToTarget()
    }
  }

  private fun updateViewportState(width: Int, height: Int, viewportSizeFactor: Float) {
    val config = this.config ?: return
    if (currentDisplayerWidth != width ||
      currentDisplayerHeight != height ||
      currentDisplayerSizeFactor != viewportSizeFactor) {
      val duration = clamp(
        (DanmakuConfig.DEFAULT_DURATION * (viewportSizeFactor * width / PLAYER_WIDTH)).toLong(),
        MIN_DANMAKU_DURATION,
        MAX_DANMAKU_DURATION_HIGH_DENSITY
      )
      if (config.rollingDurationMs != duration) {
        config.rollingDurationMs = duration
        config.updateRetainer()
        config.updateLayout()
        config.updateVisibility()
      }
      Log.d("XanaDanmaku", "[Factor] update rolling duration to $duration")
      currentDisplayerWidth = width
      currentDisplayerHeight = height
      currentDisplayerSizeFactor = viewportSizeFactor
    }
  }

  private fun updateMaxDanmakuDuration() {
    // FIXME distinguish differ danmaku type duration
  }

  private inner class ActionHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        MSG_FRAME_UPDATE -> {
          val deltaTimeSeconds = if (msg.arg1 > 0) {
            msg.arg1 / 1000.0f
          } else null
          updateFrame(deltaTimeSeconds)
        }
        NOTIFY_DISPLAYER_SIZE_CHANGE -> {
          val newConfig = engine.context.config
          newConfig.updateLayout()
          newConfig.updateMeasure()
          newConfig.updateCache()
          newConfig.updateRetainer()
        }
      }
    }
  }

  private class FrameCallback(handler: Handler) : Choreographer.FrameCallback {
    private val handlerWeakReference = WeakReference(handler)
    override fun doFrame(frameTimeNanos: Long) {
      val handler = handlerWeakReference.get() ?: return
      handler.removeMessages(MSG_FRAME_UPDATE)
      handler.sendEmptyMessage(MSG_FRAME_UPDATE)
    }
  }
}
