package com.annalisetarhan.torch.connection

data class NetworkMessage (
    val messageId: String,  // SHA(encMessage)
    val ttd: Int,
    val encMessage: String
)