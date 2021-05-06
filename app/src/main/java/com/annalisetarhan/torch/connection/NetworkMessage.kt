package com.annalisetarhan.torch.connection

import com.annalisetarhan.torch.encryption.Constants.Companion.MESSAGE_INFO_BYTES
import com.annalisetarhan.torch.encryption.Constants.Companion.TIMESTAMP_BYTES
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.intFromByteArray
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.intToByteArray

data class NetworkMessage(
        val networkId: Int?,
        val ttd: ByteArray,
        val messageInfo: Byte,
        val payload: ByteArray
)

/*
    NetworkMessage <--> ByteArray
 */

fun messageToBytes(message: NetworkMessage): ByteArray {
        /* Size of message, not including networkId */
        val messageSize: Int = TIMESTAMP_BYTES + MESSAGE_INFO_BYTES + message.payload.size

        /* ByteArray representation of message size, appended to beginning of message */
        val messageSizeBytes: ByteArray = intToByteArray(messageSize)
        val byteArray = ByteArray(Int.SIZE_BYTES + messageSize)

        var index = 0
        for (i in 0 until Int.SIZE_BYTES) {
                byteArray[index++] = messageSizeBytes[i]
        }
        for (i in 0 until TIMESTAMP_BYTES) {
                byteArray[index++] = message.ttd[i]
        }
        byteArray[index++] = message.messageInfo
        for (i in message.payload.indices) {
                byteArray[index++] = message.payload[i]
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
                /* Figure out how big the message is */
                val firstByteOfPayload = index + Int.SIZE_BYTES
                val messageSizeBytes = byteArray.sliceArray(index until firstByteOfPayload)
                val messageSize = intFromByteArray(messageSizeBytes)

                val firstByteOfNextMessage = firstByteOfPayload + messageSize
                val messageBytes = byteArray.sliceArray(firstByteOfPayload until firstByteOfNextMessage)
                list.add(bytesToMessage(messageBytes))
                index = firstByteOfNextMessage
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