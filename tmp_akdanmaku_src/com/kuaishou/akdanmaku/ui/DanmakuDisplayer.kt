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

/**
 * 弹幕显示器，是显示 View 和属性的抽象
 *
 * @author Xana
 * @since 2021-06-22
 */
interface DanmakuDisplayer {
  /**
   * 显示区域高度
   */
  var height: Int

  /**
   * 显示区域宽度
   */
  var width: Int

  /**
   * 弹幕之间的间距（lineSpacing）
   */
  val margin: Int

  /**
   * 所有弹幕距离顶端的间距（topPadding）
   */
  val allMarginTop: Float

  /**
   * 所属设备的密度，来自于 displayMetrics
   */
  val density: Float

  /**
   * 所属设备的缩放密度，来自于 displayMetrics
   */
  val scaleDensity: Float

  /**
   * 所属设备的密度 Dpi，来自于 displayMetrics
   */
  val densityDpi: Int

  /**
   * 一个缩放比例，继承自原算法，是一个根据当前屏幕密度计算出来的经验数值
   */
  fun getViewportSizeFactor(): Float = 1 / (density - 0.6f)
}
