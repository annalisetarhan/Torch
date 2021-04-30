package com.annalisetarhan.torch.encryption

import java.nio.ByteBuffer

class NumberConversions {
    companion object {
        fun longToByteArray(long: Long): ByteArray {
            val bytes = ByteArray(Long.SIZE_BYTES)
            ByteBuffer.wrap(bytes).putLong(long)
            return bytes
        }

        fun intToByteArray(int: Int): ByteArray {
            val bytes = ByteArray(Int.SIZE_BYTES)
            ByteBuffer.wrap(bytes).putInt(int)
            return bytes
        }

        fun longFromByteArray(bytes: ByteArray): Long = ByteBuffer.wrap(bytes).long
        fun intFromByteArray(bytes: ByteArray): Int = ByteBuffer.wrap(bytes).int
    }
}