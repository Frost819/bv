package dev.aaa1115910.biliapi.http.util

import io.ktor.utils.io.core.use
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

fun ByteArray.zlibCompress(): ByteArray {
    val output = ByteArray(this.size * 4)
    val compressor = Deflater().apply {
        setInput(this@zlibCompress)
        finish()
    }
    val compressedDataLength: Int = compressor.deflate(output)
    return output.copyOfRange(0, compressedDataLength)
}

fun ByteArray.zlibDecompress(): ByteArray {
    val inflater = Inflater()
    try {
        inflater.setInput(this)
        // 预估解压后大小（通常是压缩的3-5倍），减少扩容次数
        val estimatedSize = this.size * 4
        val outputStream = ByteArrayOutputStream(estimatedSize.coerceAtLeast(1024))
        return outputStream.use {
            // 增大缓冲区，减少循环次数
            val buffer = ByteArray(8192)
            var count: Int
            while (inflater.inflate(buffer).also { count = it } > 0) {
                outputStream.write(buffer, 0, count)
            }
            outputStream.toByteArray()
        }
    } finally {
        inflater.end()
    }
}