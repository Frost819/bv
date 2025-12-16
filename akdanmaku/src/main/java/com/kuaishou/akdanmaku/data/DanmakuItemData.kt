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

/**
 *
 * 单个弹幕数据结构
 */
class DanmakuItemData(

  /**
   * 单个弹幕的唯一 Id
   */
  val danmakuId: Long,
  /**
   * 弹幕的相对时间戳，单位：毫秒
   */
  val position: Long,
  /**
   * 弹幕文本内容
   */
  val content: String,

  /**
   * 弹幕的局排版的方式
   * 0 -- 默认显示方式：直接按时间戳转化的坐标显示，不考虑覆盖重叠等问题
   * 1 [DANMAKU_MODE_ROLLING] -- 智能滚动显示模式：根据时间戳转化为坐标，在保证在最大延迟范围内，不重叠的方式显示
   * 4 [DANMAKU_MODE_CENTER_BOTTOM] -- 底部居中显示：排在队列中，在屏幕的 centerBottom 显示，一般会在规定的停留时间「showTime」后消失
   * 5 [DANMAKU_MODE_CENTER_TOP] -- 顶部居中显示：排在队列中，在屏幕的 centerTop 显示，一般会在规定的停留时间「showTime」后消失
   */
  val mode: Int,
  /**
   * 弹幕字体大小
   */
  val textSize: Int,
  /**
   * 弹幕字体颜色
   */
  val textColor: Int,

  /**
   * 弹幕的分值标识，用于排序计算等
   */
  val score: Int = 0,

  /**
   * 弹幕定制样式:
   * [DANMAKU_STYLE_NONE, DANMAKU_STYLE_ICON_UP, DANMAKU_STYLE_USER_AVATAR]
   */
  val danmakuStyle: Int = DANMAKU_STYLE_NONE,

  /**
   * Deprecated!!
   * 弹幕显示优先级，数字越大优先级越高
   * 默认为 0 - 表示普通优先级
   * 1 - 一般为自己添加的弹幕，要求高优（通常为「必须」）显示
   * @deprecated
   */
  val rank: Int = 0,

  /**
   * 弹幕作者 id
   */
  var userId: Long? = null,

  var mergedType: Int = MERGED_TYPE_NORMAL
) : Comparable<DanmakuItemData> {

  val isImportant: Boolean
    get() = score > 0

  companion object {
    const val DANMAKU_MODE_ROLLING = 0x1   // 00000001(1)
    const val DANMAKU_MODE_CENTER_BOTTOM = 0x4   // 00000100(4)
    const val DANMAKU_MODE_CENTER_TOP = 0x5   // 00000101(5)

    const val DANMAKU_STYLE_NONE = 0x1   // 00000001(1)
    const val DANMAKU_STYLE_ICON_UP = 0x2   // 00000010(2)
    const val DANMAKU_STYLE_USER_AVATAR = 0x4   // 00000100(4)
    const val DANMAKU_STYLE_SELF_SEND = 0x8   // 00001000(8)

    const val MERGED_TYPE_NORMAL = 0 // 合并弹幕-默认(非合并弹幕)
    const val MERGED_TYPE_ORIGINAL = 1 // 合并弹幕-原件(合并过的弹幕)
    const val MERGED_TYPE_MERGED = 2 // 合并弹幕-合并后的弹幕(合并后生成的合并弹幕)

    private const val DANMAKU_ITEM_DATA_ID_INVALID = Long.MIN_VALUE

    /**
     * 缺省的 Position 设为最大值，将其排序到最末尾
     */
    private const val DANMAKU_ITEM_DATA_POSITION_INVALID = Long.MAX_VALUE

    private fun createSimpleDanmakuItemData(position: Long): DanmakuItemData {
      return DanmakuItemData(
        DANMAKU_ITEM_DATA_ID_INVALID, position,
        "", 0, 0, 0
      )
    }

    val DANMAKU_ITEM_DATA_EMPTY =
      createSimpleDanmakuItemData(DANMAKU_ITEM_DATA_POSITION_INVALID)
  }

  override fun compareTo(other: DanmakuItemData): Int {
    return (position - other.position).toInt()
  }

  override fun toString(): String = "Danmaku{id: $danmakuId, content: ${content.take(5)}, position: $position, mode: $mode, rank: $rank}"
}
