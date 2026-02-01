package dev.aaa1115910.bv.player.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import dev.aaa1115910.biliapi.entity.video.VideoShot
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

suspend fun VideoShot.getSpriteFrame(time: Int, cache: VideoShotImageCache): SpriteFrame {
    val index = findClosestValueIndex(times, time.toUShort())
    val singleImgCount = imageCountX * imageCountY
    val imagesIndex = index / singleImgCount
    val imageIndex = index % singleImgCount

    // 使用传入的 cache 实例，而不是全局单例
    val spriteSheet = cache.getOrDecodeImage(
        imagesIndex,
        images[imagesIndex]!!
    ).asImageBitmap()

    val cellWidth = spriteSheet.width / imageCountX
    val cellHeight = spriteSheet.height / imageCountY

    // 计算该帧在大图中的具体坐标
    val left = (imageIndex % imageCountX) * cellWidth
    val top = (imageIndex / imageCountX) * cellHeight

    return SpriteFrame(
        spriteSheet = spriteSheet,
        srcRect = IntRect(left, top, left + cellWidth, top + cellHeight)
    )
}

private fun findClosestValueIndex(array: List<UShort>, target: UShort): Int {
    var left = 0
    var right = array.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        if (array[mid] < target) {
            left = mid + 1
        } else {
            right = mid
        }
    }
    return left
}

class VideoShotImageCache {
    private val memoryCache = LruCache<Int, Bitmap>(3) // 缓存3张大图
    private val activeTasks = ConcurrentHashMap<Int, Deferred<Bitmap>>()

    companion object {
        val bitmapOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
            
            // 内存管理
            inTempStorage = ByteArray(16 * 1024) // 16KB临时缓冲区，减少内存分配
            inSampleSize = 1 // 采样率，1表示原始大小
            
            // 其他性能优化
            inJustDecodeBounds = false // 实际解码像素数据
            inPremultiplied = false // 不进行预乘处理，节省计算
            
        }
    }

    suspend fun getOrDecodeImage(imagesIndex: Int, imageData: ByteArray): Bitmap = coroutineScope {
        memoryCache.get(imagesIndex)?.let { return@coroutineScope it }

        val task = activeTasks.getOrPut(imagesIndex) {
            async(Dispatchers.IO) {
                val decoded = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, bitmapOptions)
                memoryCache.put(imagesIndex, decoded)
                decoded
            }
        }
        try {
            return@coroutineScope task.await()
        } finally {
            activeTasks.remove(imagesIndex)
        }
    }

    fun clear() {
        // 取消所有正在进行的解码任务引用
        activeTasks.clear()
        
        // 显式回收所有 Bitmap（对于 Android 8.0 以下很重要）
        memoryCache.snapshot().values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        
        // 清空缓存
        memoryCache.evictAll()
    }
}

// 包装了大图（精灵图）和小图对应的矩形区域
data class SpriteFrame(
    val spriteSheet: ImageBitmap,
    val srcRect: IntRect
)