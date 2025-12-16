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

package com.kuaishou.akdanmaku.layout

import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer

/**
 * 弹幕的布局器
 *
 * @author Xana
 * @since 2021-08-03
 */
interface DanmakuLayouter {

  /**
   * 预布局，将决定弹幕是否显示
   *
   * @param item 弹幕数据
   * @param currentTimeMills 当前的时间戳
   * @param displayer 弹幕显示的 View 与设置
   * @param config 弹幕配置
   * @return boolean 值，这个弹幕是否显示
   */
  fun preLayout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Boolean

  /**
   * 弹幕的正式布局
   *
   * @param item 弹幕数据
   * @param currentTimeMills 当前的时间戳
   * @param displayer 弹幕显示的 View 与设置
   * @param config 弹幕配置
   */
  fun layout(
    item: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  )

  fun updateScreenPart(startTop: Int, endTop: Int)

  fun clear()

  fun remove(item: DanmakuItem)
}
