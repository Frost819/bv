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

package com.kuaishou.akdanmaku.layout.retainer

import android.util.Log
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItem
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ext.*
import com.kuaishou.akdanmaku.ui.DanmakuDisplayer
import java.util.*

/**
 * 底部保持器
 * 用于维持一个底部对齐的垂直可用空间，用于寻找可以显示目标 entity 的最佳位置
 *
 * @author Xana
 * @since 2021-06-23
 */
internal class BottomRetainer(endRatio: Float) : DanmakuRetainer {

  private val bilibiliRetainer by lazy { BilibiliRetainer() }
  private val akRetainer by lazy { AkRetainer(endRatio) }

  override fun layout(
    drawItem: DanmakuItem,
    currentTimeMills: Long,
    displayer: DanmakuDisplayer,
    config: DanmakuConfig
  ): Float {
    return if (config.retainerPolicy == RETAINER_BILIBILI)
      bilibiliRetainer.layout(drawItem, currentTimeMills, displayer, config)
    else
      akRetainer.layout(drawItem, currentTimeMills, displayer, config)
  }

  override fun clear() {
    bilibiliRetainer.clear()
    akRetainer.clear()
  }

  override fun remove(item: DanmakuItem) {
    bilibiliRetainer.remove(item)
    akRetainer.remove(item)
  }

  override fun update(start: Int, end: Int) {
    bilibiliRetainer.update(start, end)
    akRetainer.update(start, end)
  }

  private class BilibiliRetainer : DanmakuRetainer {
    private var cancelFlag = false
    private val lastVisibleEntities = TreeSet(DanmakuRetainer.YPosDescComparator())

    override fun layout(
      drawItem: DanmakuItem,
      currentTimeMills: Long,
      displayer: DanmakuDisplayer,
      config: DanmakuConfig
    ): Float {

      val drawState = drawItem.drawState
      if (drawItem.isOutside(currentTimeMills)) {
        remove(drawItem)
        return -1f
      }
      val drawHolder = DanmakuRetainer.SpaceHolder(
        drawItem,
        drawItem.timePosition,
        drawState.positionY.toInt(),
        drawState.positionX.toInt(),
        drawState.width.toInt(),
        drawState.height.toInt()
      )
      var isShown = drawState.visibility && drawState.layoutGeneration == config.layoutGeneration
      var willHit: Boolean
      var topPos: Float =
        if (!isShown || drawState.positionY < displayer.allMarginTop)
          (displayer.height - drawState.height)
        else drawState.positionY
      var isOutOfVerticalEdge = false
      val state = DanmakuRetainer.RetainerState()
      if (!isShown) {
        cancelFlag = false
        lastVisibleEntities.asSequence()
          .takeWhile { !cancelFlag && !state.found }
          .forEach { holder ->
            state.lines++
            if (drawHolder == holder) {
              with(state) {
                removeEntity = null
                willHit = false
                found = true
                return@forEach
              }
            }
            if (state.firstEntity == null) {
              state.firstEntity = holder
              if (drawState.rect.bottom.toInt() != displayer.height) {
                state.found = true
                return@forEach
              }
            }
            if (topPos < displayer.allMarginTop) {
              state.removeEntity = null
              state.found = true
              return@forEach
            }
            willHit = holder.item.willCollision(
              holder.item,
              displayer,
              currentTimeMills,
              config.durationMs
            )
            if (!willHit) {
              state.removeEntity = holder
              state.found = true
              return@forEach
            }
            topPos = holder.top.toFloat() - displayer.margin - drawState.height
          }

        isOutOfVerticalEdge =
          (topPos < displayer.allMarginTop || state.firstEntity?.bottom != displayer.height)
        if (isOutOfVerticalEdge) {
          topPos = (displayer.height - drawState.height)
          willHit = true
          state.lines = 1
        } else {
          if (topPos == displayer.allMarginTop) {
            isShown = false
          }
        }
      }

//      if (verifier != null && verifier.skipLayout(drawItem, willHit)) return -1f
      if (isOutOfVerticalEdge) clear()

//      if (verifier != null && verifier.skipDraw(drawItem, topPos, state.lines, willHit)) return -1f

      if (!isShown) {
        state.removeEntity?.let { lastVisibleEntities.remove(it) }
        lastVisibleEntities.add(drawHolder)
        drawHolder.index = state.lines
      }
      return topPos
    }

    override fun clear() {
      cancelFlag = true
      lastVisibleEntities.clear()
    }

    override fun remove(item: DanmakuItem) {
      lastVisibleEntities.removeAll { it.item == item }
    }

    override fun update(start: Int, end: Int) {

    }
  }

  private class AkRetainer(var ratio: Float) : DanmakuRetainer {

    private val lastVisibleEntities = TreeSet(DanmakuRetainer.YPosDescComparator())

    override fun layout(
      drawItem: DanmakuItem,
      currentTimeMills: Long,
      displayer: DanmakuDisplayer,
      config: DanmakuConfig
    ): Float {
      val drawState = drawItem.drawState
      if (drawItem.isOutside(currentTimeMills)) {
        remove(drawItem)
        return -1f
      }
      val drawHolder = DanmakuRetainer.SpaceHolder(
        drawItem,
        drawItem.timePosition,
        drawState.positionY.toInt(),
        drawState.positionX.toInt(),
        drawState.width.toInt(),
        drawState.height.toInt()
      )
      synchronized(lastVisibleEntities) {
        val shown = drawState.visibility && drawState.layoutGeneration == config.layoutGeneration
        val (topPos, index) = if (!shown) {
          val (topPos, index) =
            if (lastVisibleEntities.any { it == drawHolder } && drawState.layoutGeneration == config.layoutGeneration) {
              drawState.positionY to 0
            } else {
              val margin = displayer.margin
              val targetHolders = sequence {
                var lastTop = displayer.height
                lastVisibleEntities.forEach { h ->
                  if (h.bottom + margin < lastTop) {
                    yield(DanmakuRetainer.RangeHolder(null, h.bottom + margin..lastTop))
                  }
                  yield(DanmakuRetainer.RangeHolder(h, h.top..h.bottom))
                  lastTop = h.top - margin
                }
                if (lastTop > displayer.allMarginTop + displayer.height * ratio) yield(
                  DanmakuRetainer.RangeHolder(
                    null,
                    displayer.allMarginTop.toInt()..lastTop
                  )
                )
              }.toList()
              val targetHolder = targetHolders.find { h ->
                val targetHeight = h.range.last - h.range.first
                drawHolder.height < targetHeight && h.holder?.item?.willCollision(
                  drawHolder.item,
                  displayer,
                  currentTimeMills,
                  config.durationMs
                ) != true
              }
              if (targetHolder == null) {
                Log.v(DanmakuEngine.TAG, "[Retainer] no room for this")
                // if overlapped clear and arrange it from beginning.
                drawState.layoutGeneration = config.layoutGeneration
                drawState.visibility = false
                return -1f
              }

              drawHolder.top = targetHolder.range.last - drawHolder.height
              Log.v(
                DanmakuEngine.TAG,
                "[Retainer] range: ${drawHolder.top..drawHolder.bottom} with targetHolder ${targetHolder.range}"
              )
              targetHolder.holder?.let { lastVisibleEntities.remove(it) }
              drawHolder.top.toFloat() to (targetHolder.holder?.index
                ?: lastVisibleEntities.size + 1)
            }
          topPos to index
        } else (drawState.positionY to 0)
        val willHit = index < 0
        lastVisibleEntities.add(drawHolder)
        if (willHit && !config.allowOverlap) {
          drawState.layoutGeneration = config.layoutGeneration
          drawState.visibility = false
          return -1f
        }
        drawState.visibility = true
        return topPos
      }
    }

    override fun clear() {
      lastVisibleEntities.clear()
    }

    override fun remove(item: DanmakuItem) {
      synchronized(lastVisibleEntities) {
        lastVisibleEntities.removeAll { it.item == item }
      }
    }

    override fun update(start: Int, end: Int) {

    }
  }
}
