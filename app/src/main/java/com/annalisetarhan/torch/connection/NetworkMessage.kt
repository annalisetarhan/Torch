package com.annalisetarhan.torch.connection

data class NetworkMessage (
    val messageId: ByteArray,  // SHA(encMessage) 32 bytes
    val ttd: Long,             // 8 bytes
    val encMessage: ByteArray  // iv - 12 bytes, timeSent - 8 bytes, public key - 20? bytes
    // 255 - (32+8+12+8+20) = 175, but we don't know if we'll actually have the full 255. So 160?
)