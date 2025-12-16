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

package com.kuaishou.akdanmaku.ecs.component.action

import com.badlogic.gdx.utils.Pools

/**
 * 方便生成各类 Action 工具类
 *
 * @author Xana
 * @since 2021-07-14
 */
@Suppress("unused")
object Actions {

  fun <T : Action> action(type: Class<T>): T {
    val pool = Pools.get(type)
    val action = pool.obtain()
    action.pool = pool
    return action
  }

  fun moveTo(
    x: Float,
    y: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): MoveToAction {
    val action = action(MoveToAction::class.java)
    action.setPosition(x, y)
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun moveBy(
    amountX: Float,
    amountY: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): MoveByAction {
    val action = action(MoveByAction::class.java)
    action.setAmount(amountX, amountY)
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  // consider whether size change action is necessary

  fun scaleTo(
    x: Float,
    y: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): ScaleToAction {
    val action = action(ScaleToAction::class.java)
    action.setScale(x, y)
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun scaleBy(
    amountX: Float,
    amountY: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): ScaleByAction {
    val action = action(ScaleByAction::class.java)
    action.setAmount(amountX, amountY)
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun rotateTo(
    rotation: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): RotateToAction {
    val action = action(RotateToAction::class.java)
    action.rotation = rotation
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun rotateBy(
    rotationAmount: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): RotateByAction {
    val action = action(RotateByAction::class.java)
    action.amount = rotationAmount
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun alpha(
    a: Float,
    durationMs: Long = 0,
    interpolation: Interpolation = Interpolation.linear
  ): AlphaAction {
    val action = action(AlphaAction::class.java)
    action.alpha = a
    action.duration = durationMs
    action.interpolation = interpolation
    return action
  }

  fun fadeOut(
    durationMs: Long,
    interpolation: Interpolation = Interpolation.linear
  ): AlphaAction = alpha(0f, durationMs, interpolation)

  fun fadeIn(
    durationMs: Long,
    interpolation: Interpolation = Interpolation.linear
  ): AlphaAction = alpha(1f, durationMs, interpolation)

  fun delay(
    delay: Long,
    delayedAction: Action? = null
  ): DelegateAction {
    val action = action(DelayAction::class.java)
    action.delay = delay
    action.action = delayedAction
    return action
  }

  fun sequence(vararg actions: Action): SequenceAction {
    val action = action(SequenceAction::class.java)
    actions.forEach { action.addAction(it) }
    return action
  }

  fun parallel(vararg actions: Action): ParallelAction {
    val action = action(ParallelAction::class.java)
    actions.forEach { action.addAction(it) }
    return action
  }

  fun repeat(count: Int, repeatedAction: Action): RepeatAction {
    val action = action(RepeatAction::class.java)
    action.count = count
    action.action = repeatedAction
    return action
  }

  fun forever(repeatedAction: Action): RepeatAction = repeat(RepeatAction.FOREVER, repeatedAction)

}
