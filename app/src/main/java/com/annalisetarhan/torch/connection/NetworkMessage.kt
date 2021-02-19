package com.annalisetarhan.torch.connection

data class NetworkMessage (
        val networkId: Int, // Truncated to fit WiFi Aware constraints
        val ttd: ByteArray,
        val encMessage: ByteArray
)