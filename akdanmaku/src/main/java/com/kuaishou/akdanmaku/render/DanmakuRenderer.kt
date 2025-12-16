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

package com.kuaishou.akdanmaku.render

import android.graphics.Canvas
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import com.kuaishou.akdanmaku.utils.Size

/**
 * 弹幕绘制渲染器接口
 *
 * 此接口的实现应当是一个无状态的实现
 *
 * @author Xana
 * @since 2021-06-17
 */
interface DanmakuRenderer {

  /**
   * 更新弹幕画笔，此方法会在 measure 和 draw 执行前调用此方法来更新内部的画笔
   *
   * @param item 弹幕具体的数据
   * @param displayer 弹幕当前显示器，描述了相关的信息
   * @param config 由业务方传入的当前弹幕配置
   */
  fun updatePaint(item: DanmakuItem, displayer: DanmakuDisplayer, config: DanmakuConfig)

  /**
   * 对弹幕进行测量
   *
   * @param item 弹幕具体的数据
   * @param displayer 弹幕当前显示器，描述了相关的信息
   * @param config 由业务方传入的当前弹幕配置
   * @return 一个 [Size] 类型的数据。
   */
  fun measure(item: DanmakuItem, displayer: DanmakuDisplayer, config: DanmakuConfig): Size

  /**
   * 对弹幕进行绘制
   * > 这里可能绘制在 Cache 上，也可能直接绘制在最终的 View 上，canvas 的尺寸并不代表实际的尺寸
   *
   * @param item 弹幕具体的数据
   * @param canvas 绘制的 Canvas
   * @param displayer 弹幕当前显示器，描述了相关的信息
   * @param config 由业务方传入的当前弹幕配置
   */
  fun draw(item: DanmakuItem, canvas: Canvas, displayer: DanmakuDisplayer, config: DanmakuConfig)
}
