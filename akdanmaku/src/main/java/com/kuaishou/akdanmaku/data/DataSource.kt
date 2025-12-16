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

package com.kuaishou.akdanmaku.data

import com.kuaishou.akdanmaku.collection.TreeList
import java.lang.ref.WeakReference

/**
 * 弹幕的 DataSource，用于提供数据
 *
 * @author Xana
 * @since 2021-08-03
 */
@Suppress("unused")
open class DataSource {

  val danmakuItems = TreeList<DanmakuItem>()

  protected var changeListener = WeakReference<DataChangeListener>(null)

  fun setListener(listener: DataChangeListener?) {
    if (listener != changeListener.get()) {
      changeListener = WeakReference<DataChangeListener>(listener)
    }
  }

  open fun addItems(items: List<DanmakuItem>) {
    danmakuItems.addAll(items)
  }

  protected fun notifyItemsAdded(items: List<DanmakuItem>) {
    changeListener.get()?.onDataAdded(items)
  }

  interface DataChangeListener {

    fun onDataAdded(additionalItems: List<DanmakuItem>)

    fun onDataRemoved(removalItems: List<DanmakuItem>)
  }
}
