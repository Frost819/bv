package dev.aaa1115910.biliapi.http.util

import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun ByteArray.brotliDecompress(): ByteArray {
    val inputStream = BrotliInputStream(ByteArrayInputStream(this))
    // 预估解压后大小，减少扩容次数
    val estimatedSize = this.size * 4
    val outputStream = ByteArrayOutputStream(estimatedSize.coerceAtLeast(4096))
    return outputStream.use {
        // 增大缓冲区，减少循环次数
        val buffer = ByteArray(8192)
        var count: Int
        try {
            while (inputStream.read(buffer).also { count = it } != -1) {
                outputStream.write(buffer, 0, count)
            }
        } finally {
            inputStream.close()
        }
        outputStream.toByteArray()
    }
}
