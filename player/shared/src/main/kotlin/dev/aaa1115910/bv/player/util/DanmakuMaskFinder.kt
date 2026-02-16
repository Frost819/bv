package dev.aaa1115910.bv.player.util

import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.aaa1115910.biliapi.entity.danmaku.DanmakuMaskSegment

/**
 * 弹幕蒙版查找工具类
 * 使用二分查找
 *
 */
object DanmakuMaskFinder {
    /**
     * 在 segments 中定位包含指定时间的帧
     *
     * @param segments 蒙版 segment 列表
     * @param positionMs 当前播放位置（毫秒）
     * @return 包含该时间的蒙版帧，如果未找到则返回 null
     */
    fun findMaskFrame(
        segments: List<DanmakuMaskSegment>,
        positionMs: Long
    ): DanmakuMaskFrame? {
        if (segments.isEmpty()) return null

        val segment = binarySearchSegment(segments, positionMs) ?: return null

        return binarySearchFrame(segment.frames, positionMs)
    }

    /**
     * 二分查找指定时间的 segment
     */
    private fun binarySearchSegment(
        segments: List<DanmakuMaskSegment>,
        positionMs: Long
    ): DanmakuMaskSegment? {
        var left = 0
        var right = segments.size - 1

        while (left <= right) {
            val mid = (left + right) ushr 1
            val segment = segments[mid]

            when {
                positionMs < segment.range.first -> right = mid - 1
                positionMs > segment.range.last -> left = mid + 1
                else -> return segment
            }
        }
        return null
    }

    /**
     * 二分查找指定时间的 frame
     */
    private fun binarySearchFrame(
        frames: List<DanmakuMaskFrame>,
        positionMs: Long
    ): DanmakuMaskFrame? {
        if (frames.isEmpty()) return null

        var left = 0
        var right = frames.size - 1

        while (left <= right) {
            val mid = (left + right) ushr 1
            val frame = frames[mid]

            when {
                positionMs < frame.range.first -> right = mid - 1
                positionMs > frame.range.last -> left = mid + 1
                else -> return frame
            }
        }
        return null
    }
}
