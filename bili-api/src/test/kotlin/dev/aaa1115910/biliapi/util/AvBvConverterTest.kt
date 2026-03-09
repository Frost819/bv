package dev.aaa1115910.biliapi.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AvBvConverterTest {
    @Test
    fun `known av and bv should convert each other`() {
        val aid = 170001L
        val bv = AvBvConverter.av2bv(aid)

        assertEquals("BV17x411w7KC", bv)
        assertEquals(aid, AvBvConverter.bv2av(bv))
    }

    @Test
    fun `multiple aid samples should keep round trip`() {
        val aids = listOf(1L, 170001L, 455017605L, 987654321L)

        aids.forEach { aid ->
            val bv = AvBvConverter.av2bv(aid)
            assertEquals(aid, AvBvConverter.bv2av(bv), "round-trip failed for aid=$aid")
        }
    }

    @Test
    fun `invalid bvid length should throw`() {
        assertFailsWith<IllegalArgumentException> {
            AvBvConverter.bv2av("BV17x411w7K")
        }
    }

    @Test
    fun `invalid bvid char should throw`() {
        assertFailsWith<IllegalArgumentException> {
            AvBvConverter.bv2av("BV17x411w7K#")
        }
    }
}
