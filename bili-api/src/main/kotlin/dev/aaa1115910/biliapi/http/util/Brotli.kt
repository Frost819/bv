package dev.aaa1115910.biliapi.http.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.brotli.dec.BrotliInputStream

fun ByteArray.brotliDecompress(): ByteArray {
    val input = BrotliInputStream(ByteArrayInputStream(this))
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var read: Int
    while (true) {
        read = input.read(buffer)
        if (read <= 0) break
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

