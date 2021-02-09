package com.annalisetarhan.torch

data class DomainMessage(
        val messageId: ByteArray,
        val hashtag: String,
        val timeSent: Long,
        val senderPublicKey: ByteArray, // TODO: should be color and icon instead
        val message: String
)