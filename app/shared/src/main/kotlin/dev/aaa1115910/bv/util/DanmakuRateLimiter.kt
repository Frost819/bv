package dev.aaa1115910.bv.util

import dev.aaa1115910.biliapi.http.entity.live.DanmakuEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 直播弹幕流控器
 * 
 * 使用滑动窗口算法限制弹幕发送速率，防止弹幕过多影响性能
 * 
 * @param maxPerSecond 每秒最大弹幕数量，默认 100
 */
class DanmakuRateLimiter(
    private val maxPerSecond: Int = 100
) {
    private val danmakuChannel = Channel<DanmakuEvent>(Channel.UNLIMITED)
    private val timestamps = mutableListOf<Long>()
    private val mutex = Mutex()
    
    /**
     * 提交弹幕到流控器
     * 
     * @param event 弹幕事件
     * @return true 如果成功提交，false 如果 Channel 已关闭
     */
    suspend fun submitDanmaku(event: DanmakuEvent): Boolean {
        return danmakuChannel.trySend(event).isSuccess
    }
    
    /**
     * 启动流控器
     * 
     * @param scope 协程作用域
     * @param onEmit 弹幕发送回调
     */
    fun start(scope: CoroutineScope, onEmit: (DanmakuEvent) -> Unit) {
        scope.launch {
            try {
                while (isActive) {
                    val result = danmakuChannel.receiveCatching()
                    val event = result.getOrNull() ?: break
                    
                    // 使用滑动窗口检查是否可以发送
                    val canEmit = mutex.withLock {
                        val now = System.currentTimeMillis()
                        
                        // 移除超过 1 秒的时间戳
                        timestamps.removeAll { now - it > 1000 }
                        
                        // 检查是否超过限制
                        if (timestamps.size < maxPerSecond) {
                            timestamps.add(now)
                            true
                        } else {
                            false // 丢弃弹幕
                        }
                    }
                    
                    if (canEmit) {
                        onEmit(event)
                    }
                }
            } catch (e: Exception) {
                // Channel 关闭或其他异常，优雅退出
            }
        }
    }
    
    /**
     * 停止流控器并清理资源
     */
    fun stop() {
        danmakuChannel.close()
        timestamps.clear()
    }
}
