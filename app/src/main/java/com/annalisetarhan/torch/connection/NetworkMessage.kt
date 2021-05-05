package com.annalisetarhan.torch.connection

import com.annalisetarhan.torch.encryption.Constants

data class NetworkMessage(
        val networkId: Int?,    // Truncated to fit WiFi Aware constraints
        val ttd: ByteArray,
        val messageInfo: Byte,
        val payload: ByteArray
)

/*
    NetworkMessage <--> ByteArray
 */

fun messageToBytes(message: NetworkMessage): ByteArray {
        val byteArray = ByteArray(Constants.MAX_MESSAGE_SIZE)
        var index = 0
        for (i in message.ttd.indices) {
                byteArray[index++] = message.ttd[i]
        }
        byteArray[index++] = message.messageInfo
        for (i in message.payload.indices) {
                byteArray[index++] = message.payload[i]
        }
        while (index < Constants.MAX_MESSAGE_SIZE) {
                byteArray[index++] = 0
        }
        return byteArray
}

fun messageListToBytes(messageList: List<NetworkMessage>): ByteArray {
        var byteArray = byteArrayOf()
        for (message in messageList) {
                byteArray += messageToBytes(message)
        }
        return byteArray
}

fun bytesToMessageList(byteArray: ByteArray): List<NetworkMessage> {
        val list = mutableListOf<NetworkMessage>()
        var index = 0
        while (index < byteArray.size) {
                list.add(bytesToMessage(byteArray.sliceArray(IntRange(index, index+ Constants.MAX_MESSAGE_SIZE))))
                index += Constants.MAX_MESSAGE_SIZE
        }
        return list
}

fun bytesToMessage(message: ByteArray): NetworkMessage {
        return NetworkMessage(
                networkId = null,
                ttd = message.sliceArray(0..7),
                messageInfo = message[8],
                payload = message.sliceArray(9 until message.size)
        )
}