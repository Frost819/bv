/*
 * 弹幕性能监控工具
 * 
 * 用于收集和分析弹幕系统的性能指标
 */

package com.kuaishou.akdanmaku.utils

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 弹幕性能监控器
 * 
 * 功能：
 * 1. 记录每帧绘制时间
 * 2. 计算缓存命中率
 * 3. 监控内存使用
 * 4. 检测掉帧情况
 */
class DanmakuPerformanceMonitor(
    private val enableLogging: Boolean = true,
    private val logInterval: Int = 60 // 每60帧输出一次日志
) {
    companion object {
        private const val TAG = "DanmakuPerformance"
        private const val TARGET_FRAME_TIME_MS = 16.67f // 60fps
        private const val FRAME_DROP_THRESHOLD_MS = 33f  // 超过33ms认为掉帧
    }
    
    // 性能指标
    data class Metrics(
        var totalFrames: Long = 0,
        var droppedFrames: Long = 0,
        var averageDrawTime: Float = 0f,
        var maxDrawTime: Float = 0f,
        var minDrawTime: Float = Float.MAX_VALUE,
        var cacheHits: Long = 0,
        var cacheMisses: Long = 0,
        var activeDanmakuCount: Int = 0,
        var peakDanmakuCount: Int = 0,
        var totalMemoryUsage: Long = 0
    )
    
    private val metrics = Metrics()
    private val recentFrameTimes = ConcurrentLinkedQueue<Float>()
    private val maxRecentFrames = 100
    
    private var lastLogFrame = 0L
    private var lastFrameTime = 0L
    
    /**
     * 记录一帧的开始
     * @return 帧开始时间（用于后续计算）
     */
    fun frameStart(): Long {
        return SystemClock.elapsedRealtime()
    }
    
    /**
     * 记录一帧的结束
     * @param startTime 帧开始时间（由frameStart()返回）
     * @param cacheHit 是否命中缓存
     * @param danmakuCount 当前弹幕数量
     */
    fun frameEnd(startTime: Long, cacheHit: Boolean, danmakuCount: Int) {
        val currentTime = SystemClock.elapsedRealtime()
        val frameTime = (currentTime - startTime).toFloat()
        
        // 更新基础统计
        metrics.totalFrames++
        
        // 更新绘制时间统计
        updateDrawTimeStats(frameTime)
        
        // 更新缓存统计
        if (cacheHit) {
            metrics.cacheHits++
        } else {
            metrics.cacheMisses++
        }
        
        // 更新弹幕数量统计
        metrics.activeDanmakuCount = danmakuCount
        if (danmakuCount > metrics.peakDanmakuCount) {
            metrics.peakDanmakuCount = danmakuCount
        }
        
        // 检测掉帧
        if (frameTime > FRAME_DROP_THRESHOLD_MS) {
            metrics.droppedFrames++
        }
        
        // 定期输出日志
        if (enableLogging && metrics.totalFrames - lastLogFrame >= logInterval) {
            logMetrics()
            lastLogFrame = metrics.totalFrames
        }
        
        lastFrameTime = currentTime
    }
    
    /**
     * 更新绘制时间统计
     */
    private fun updateDrawTimeStats(frameTime: Float) {
        // 更新最大/最小值
        if (frameTime > metrics.maxDrawTime) {
            metrics.maxDrawTime = frameTime
        }
        if (frameTime < metrics.minDrawTime) {
            metrics.minDrawTime = frameTime
        }
        
        // 维护最近帧时间队列
        recentFrameTimes.offer(frameTime)
        if (recentFrameTimes.size > maxRecentFrames) {
            recentFrameTimes.poll()
        }
        
        // 计算平均值
        metrics.averageDrawTime = recentFrameTimes.average().toFloat()
    }
    
    /**
     * 更新内存使用情况
     */
    fun updateMemoryUsage(memoryBytes: Long) {
        metrics.totalMemoryUsage = memoryBytes
    }
    
    /**
     * 获取当前性能指标
     */
    fun getMetrics(): Metrics = metrics.copy()
    
    /**
     * 计算缓存命中率
     */
    fun getCacheHitRate(): Float {
        val total = metrics.cacheHits + metrics.cacheMisses
        return if (total > 0) {
            metrics.cacheHits.toFloat() / total.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * 计算掉帧率
     */
    fun getDropFrameRate(): Float {
        return if (metrics.totalFrames > 0) {
            metrics.droppedFrames.toFloat() / metrics.totalFrames.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * 获取平均FPS
     */
    fun getAverageFPS(): Float {
        return if (metrics.averageDrawTime > 0) {
            1000f / metrics.averageDrawTime
        } else {
            0f
        }
    }
    
    /**
     * 判断当前性能是否良好
     */
    fun isPerformanceGood(): Boolean {
        val dropRate = getDropFrameRate()
        val cacheHitRate = getCacheHitRate()
        val avgFPS = getAverageFPS()
        
        return dropRate < 0.05f &&  // 掉帧率低于5%
               cacheHitRate > 0.8f && // 缓存命中率高于80%
               avgFPS >= 55f           // 平均FPS大于55
    }
    
    /**
     * 输出性能日志
     */
    private fun logMetrics() {
        val cacheHitRate = getCacheHitRate() * 100
        val dropFrameRate = getDropFrameRate() * 100
        val avgFPS = getAverageFPS()
        val memoryMB = metrics.totalMemoryUsage / (1024 * 1024)
        
        Log.d(TAG, buildString {
            appendLine("┌─────────────────────────────────────────")
            appendLine("│ 弹幕性能报告 (${metrics.totalFrames}帧)")
            appendLine("├─────────────────────────────────────────")
            appendLine("│ 帧率统计:")
            appendLine("│   平均FPS: %.1f".format(avgFPS))
            appendLine("│   掉帧率: %.2f%%".format(dropFrameRate))
            appendLine("│   平均帧时: %.2fms".format(metrics.averageDrawTime))
            appendLine("│   最大帧时: %.2fms".format(metrics.maxDrawTime))
            appendLine("├─────────────────────────────────────────")
            appendLine("│ 缓存统计:")
            appendLine("│   命中率: %.1f%%".format(cacheHitRate))
            appendLine("│   命中: ${metrics.cacheHits}, 未命中: ${metrics.cacheMisses}")
            appendLine("├─────────────────────────────────────────")
            appendLine("│ 弹幕统计:")
            appendLine("│   当前数量: ${metrics.activeDanmakuCount}")
            appendLine("│   峰值数量: ${metrics.peakDanmakuCount}")
            appendLine("├─────────────────────────────────────────")
            appendLine("│ 内存使用: ${memoryMB}MB")
            appendLine("├─────────────────────────────────────────")
            appendLine("│ 性能评级: ${getPerformanceGrade()}")
            appendLine("└─────────────────────────────────────────")
        })
    }
    
    /**
     * 获取性能评级
     */
    private fun getPerformanceGrade(): String {
        val avgFPS = getAverageFPS()
        val cacheHitRate = getCacheHitRate()
        val dropRate = getDropFrameRate()
        
        return when {
            avgFPS >= 58 && cacheHitRate > 0.9f && dropRate < 0.02f -> "优秀 ⭐⭐⭐⭐⭐"
            avgFPS >= 55 && cacheHitRate > 0.85f && dropRate < 0.05f -> "良好 ⭐⭐⭐⭐"
            avgFPS >= 50 && cacheHitRate > 0.75f && dropRate < 0.10f -> "中等 ⭐⭐⭐"
            avgFPS >= 45 && cacheHitRate > 0.65f && dropRate < 0.15f -> "较差 ⭐⭐"
            else -> "糟糕 ⭐"
        }
    }
    
    /**
     * 重置所有统计数据
     */
    fun reset() {
        metrics.totalFrames = 0
        metrics.droppedFrames = 0
        metrics.averageDrawTime = 0f
        metrics.maxDrawTime = 0f
        metrics.minDrawTime = Float.MAX_VALUE
        metrics.cacheHits = 0
        metrics.cacheMisses = 0
        metrics.activeDanmakuCount = 0
        metrics.peakDanmakuCount = 0
        metrics.totalMemoryUsage = 0
        recentFrameTimes.clear()
        lastLogFrame = 0
    }
    
    /**
     * 导出性能报告（JSON格式）
     */
    fun exportReport(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"total_frames\": ${metrics.totalFrames},")
            appendLine("  \"dropped_frames\": ${metrics.droppedFrames},")
            appendLine("  \"drop_rate\": ${getDropFrameRate()},")
            appendLine("  \"average_fps\": ${getAverageFPS()},")
            appendLine("  \"average_draw_time_ms\": ${metrics.averageDrawTime},")
            appendLine("  \"max_draw_time_ms\": ${metrics.maxDrawTime},")
            appendLine("  \"cache_hit_rate\": ${getCacheHitRate()},")
            appendLine("  \"peak_danmaku_count\": ${metrics.peakDanmakuCount},")
            appendLine("  \"memory_usage_mb\": ${metrics.totalMemoryUsage / (1024 * 1024)},")
            appendLine("  \"performance_grade\": \"${getPerformanceGrade()}\"")
            appendLine("}")
        }
    }
}

/*
 * 使用示例（在 RenderSystem.kt 中集成）：
 * 
 * // 1. 创建监控器
 * private val performanceMonitor = DanmakuPerformanceMonitor(
 *     enableLogging = inDebugMode,
 *     logInterval = 60
 * )
 * 
 * // 2. 在draw方法中使用
 * fun draw(canvas: Canvas, onRenderReady: () -> Unit) {
 *     val frameStartTime = performanceMonitor.frameStart()
 *     
 *     // ... 原有绘制代码 ...
 *     
 *     // 记录帧结束
 *     val cacheHit = cacheHit.num > 0
 *     val danmakuCount = renderResult?.renderObjects?.size ?: 0
 *     performanceMonitor.frameEnd(frameStartTime, cacheHit, danmakuCount)
 *     
 *     // 更新内存使用
 *     performanceMonitor.updateMemoryUsage(cachePool.memorySize.toLong())
 *     
 *     // 根据性能决定是否需要降级
 *     if (!performanceMonitor.isPerformanceGood()) {
 *         // 触发降级策略
 *     }
 * }
 */
