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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 一些代理方法
 *
 * @author Xana
 * @since 2021-07-16
 */

/**
 * 当值发生变化时执行一个方法，是 [kotlin.properties.Delegates.observable] 的类似封装
 */
class ChangeObserverDelegate<T : Comparable<T>>(initial: T, private val onChange: (new: T) -> Unit) : ReadWriteProperty<Any, T> {
  var value: T = initial

  override fun getValue(thisRef: Any, property: KProperty<*>): T {
    return value
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    val oldValue = this.value
    this.value = value
    if (oldValue != value) {
      onChange(value)
    }
  }

  override fun toString(): String = value.toString()
}

fun <T : Comparable<T>> onChange(initial: T, change: (new: T) -> Unit) = ChangeObserverDelegate(initial, change)
