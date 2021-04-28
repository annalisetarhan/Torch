package com.annalisetarhan.torch

data class DomainMessage(
        val messageId: ByteArray,
        val messageInfo: Int,
        val hashtag: String,
        val timeSent: Long,
        val senderPublicKeyTrunc: Long, // TODO: should be color and icon instead
        val sentByMe: Boolean,
        val message: String
)