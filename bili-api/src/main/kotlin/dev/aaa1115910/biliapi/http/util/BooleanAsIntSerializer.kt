package dev.aaa1115910.biliapi.http.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 用于处理API返回Int(0/1)但代码期望Boolean的情况
 */
object BooleanAsIntSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        // 尝试读取Int值，0为false，非0为true
        return try {
            val value = decoder.decodeInt()
            value != 0
        } catch (e: Exception) {
            // 如果读取Int失败，尝试直接读取Boolean
            decoder.decodeBoolean()
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeInt(if (value) 1 else 0)
    }
}
