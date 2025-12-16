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

import android.util.Log
import com.badlogic.ashley.core.Engine
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ecs.DanmakuContext
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ecs.base.DanmakuEntitySystem

/**
 * 弹幕场景对应的 System，确保其在 update 周期的第一顺位调用
 *
 * @author changyi
 * @since 2021/6/21
 */
internal class DanmakuSystem(context: DanmakuContext) : DanmakuEntitySystem(context) {

  var newConfig: DanmakuConfig? = null
    private set

  override fun release() {
  }

  override fun addedToEngine(engine: Engine) {
  }

  override fun update(deltaTime: Float) {
    updateConfig()
  }

  private fun updateConfig() {
    newConfig?.let { config ->

      val currentConfig = danmakuContext.config

      if (currentConfig.density != config.density ||
        currentConfig.bold != config.bold) {

        Log.w(DanmakuEngine.TAG, "[Config] density from ${currentConfig.density} to ${config.density}")
        config.updateMeasure()
        config.updateRetainer()
        config.updateLayout()
        config.updateCache()
      }

      if (currentConfig.textSizeScale != config.textSizeScale) {
        Log.w(DanmakuEngine.TAG, "[Config] textSizeScale change from ${currentConfig.textSizeScale} to ${config.textSizeScale}")
        config.updateRetainer()
        config.updateLayout()
        config.updateMeasure()
        config.updateCache()
      }

      if (currentConfig.visibility != config.visibility) {
        config.updateVisibility()
      }

      if (currentConfig.screenPart != config.screenPart ||
        currentConfig.allowOverlap != config.allowOverlap) {
        config.updateLayout()
        config.updateVisibility()
        config.updateRetainer()
      }
      if (currentConfig.layoutFilter.size != config.layoutFilter.size ||
        currentConfig.filterGeneration != config.filterGeneration) {
        config.updateFilter()
      }
      danmakuContext.updateConfig(config)
    }
    newConfig = null
  }

  fun updateDanmakuConfig(danmakuConfig: DanmakuConfig) {
    this.newConfig = danmakuConfig
  }
}
