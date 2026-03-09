package dev.aaa1115910.biliapi.util

object AvBvConverter {
    private const val XOR_CODE = 23442827791579L
    private const val MASK_CODE = 2251799813685247L
    private const val MAX_AID = 1L shl 51
    private const val BASE = 58L

    private const val DATA = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf"
    private val DATA_INDEX = IntArray(128) { -1 }.apply {
        DATA.forEachIndexed { index, c ->
            this[c.code] = index
        }
    }

    fun av2bv(aid: Long): String {
        val bytes = "BV1000000000".toCharArray()
        var bvIndex = bytes.size - 1
        var tmp = (MAX_AID or aid) xor XOR_CODE

        while (tmp > 0) {
            bytes[bvIndex] = DATA[(tmp % BASE).toInt()]
            tmp /= BASE
            bvIndex--
        }

        bytes.swap(3, 9)
        bytes.swap(4, 7)
        return String(bytes)
    }

    fun bv2av(bvid: String): Long {
        require(bvid.length == 12) { "bvid length must be 12" }

        val bvidArr = bvid.toCharArray()
        bvidArr.swap(3, 9)
        bvidArr.swap(4, 7)

        var tmp = 0L
        for (index in 3 until bvidArr.size) {
            val c = bvidArr[index]
            require(c.code < DATA_INDEX.size && DATA_INDEX[c.code] >= 0) {
                "invalid bvid char: $c"
            }
            tmp = tmp * BASE + DATA_INDEX[c.code]
        }

        return (tmp and MASK_CODE) xor XOR_CODE
    }

    private fun CharArray.swap(i: Int, j: Int) {
        this[i] = this[j].also { this[j] = this[i] }
    }
}
