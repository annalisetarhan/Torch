package com.annalisetarhan.torch

data class DomainMessage(
        val messageId: ByteArray,
        val messageInfo: ByteArray,
        val hashtag: String,
        val timeSent: Long,
        val senderPublicKeyTrunc: ByteArray, // TODO: should be color and icon instead
        val message: String
)