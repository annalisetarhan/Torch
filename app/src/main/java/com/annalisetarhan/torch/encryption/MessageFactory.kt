package com.annalisetarhan.torch.encryption

import android.content.Context
import androidx.databinding.library.BuildConfig
import com.annalisetarhan.torch.ui.DomainMessage
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.Constants.Companion.TRUNC_KEY_BYTES
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.intFromByteArray
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.longFromByteArray
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.longToByteArray
import java.util.*

class MessageFactory(context: Context) {

    val keys = Keys(context)
    private val time = Time()
    private val encryption = Encryption(keys)

    private val activeHashtags = arrayListOf<String>()

    fun getPublicKey(): String = keys.publicKey().toString()
    fun getTruncatedPublicKey(): Long = keys.truncatedPublicKey()
    fun resetKeys() = keys.generateKeyPair()
    fun addHashtag(hashtag: String) = activeHashtags.add(hashtag)
    fun removeHashtag(hashtag: String) = activeHashtags.remove(hashtag)
    fun decryptStandardMessage(hashtag: String, dbMessage: DatabaseMessage): Boolean =
            encryption.decryptStandardMessage(hashtag, dbMessage)

    /*
        MAKE MESSAGES
     */

    fun makeStandardDatabaseMessage(hashtag: String, rawMessage: String): DatabaseMessage {
        val timeSent: Long = time.currentTimeInSecs()
        val encMessage: ByteArray = encryption.encryptStandardMessage(hashtag, timeSent, rawMessage)

        return DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = encryption.hash(encMessage.toString(Charsets.UTF_8)),
                ttd = time.getMessageTtd(timeSent),
                messageInfo = 1,
                encMessage = encMessage,
                hashtag = hashtag,
                timeSent = timeSent,
                senderPublicKeyTrunc = keys.truncatedPublicKey(),
                message = rawMessage
        )
    }

    fun makePrivateDatabaseMessage(receiverPublicKeyTrunc: Long, rawMessage: String): DatabaseMessage {
        val timeSent: Long = time.currentTimeInSecs()
        val encMessage: ByteArray = encryption.encryptPrivateMessage(receiverPublicKeyTrunc, timeSent, rawMessage)

        return DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = encryption.hash(encMessage.toString(Charsets.UTF_8)),
                ttd = time.getMessageTtd(timeSent),
                messageInfo = 2,
                encMessage = longToByteArray(receiverPublicKeyTrunc) + encMessage,
                receiverPublicKeyTrunc = receiverPublicKeyTrunc,
                timeSent = timeSent,
                senderPublicKeyTrunc = keys.truncatedPublicKey(),
                message = rawMessage
        )
    }

    fun makeNetworkMessage(dbMessage: DatabaseMessage): NetworkMessage {
        // TODO: split up long messages, reconstruct them on the other side
        val paddedTtd = longToByteArray(dbMessage.ttd)

        /* Size checks */
        if (BuildConfig.DEBUG && dbMessage.messageId.size != 32) {
            error("Assertion failed")
        }
        if (BuildConfig.DEBUG && (paddedTtd.size != Long.SIZE_BYTES || Long.SIZE_BYTES != 8)) {
            error("Assertion failed")
        }

        return NetworkMessage(
                networkId = intFromByteArray(dbMessage.messageId),  // Truncated
                ttd = paddedTtd,
                messageInfo = dbMessage.messageInfo.toByte(),
                payload = dbMessage.encMessage
        )
    }

    /* Liberal use of !! because this will only be called on messages whose hashtag is known,
     * so all other fields will be known as well */
    fun makeDomainMessage(message: DatabaseMessage): DomainMessage {
        return DomainMessage(
                message.messageId,
                message.messageInfo,
                message.hashtag!!,
                message.timeSent!!,
                message.senderPublicKeyTrunc!!,
                message.senderPublicKeyTrunc == keys.truncatedPublicKey(),
                message.message!!
        )
    }

    fun reconstructStandardMessage(networkMessage: NetworkMessage): DatabaseMessage {
        val encMessage = networkMessage.payload
        val dbMessage = DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = encryption.hash(encMessage.toString(Charsets.UTF_8)),     // Can't just use networkMessage's id because it was truncated
                ttd = longFromByteArray(networkMessage.ttd),
                messageInfo = 1,
                encMessage = encMessage
        )
        for (hashtag in activeHashtags) {
            if (encryption.decryptStandardMessage(hashtag, dbMessage)) break
        }
        return dbMessage
    }

    fun reconstructPrivateMessage(networkMessage: NetworkMessage): DatabaseMessage {
        val encMessage = networkMessage.payload
        val receiverPublicKeyTrunc = longFromByteArray(encMessage.copyOfRange(0, TRUNC_KEY_BYTES))
        val dbMessage = DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = encryption.hash(encMessage.toString(Charsets.UTF_8)), // Can't just use networkMessage's id because it was truncated
                ttd = longFromByteArray(networkMessage.ttd),
                messageInfo = 2,
                encMessage = encMessage,
                receiverPublicKeyTrunc = receiverPublicKeyTrunc
        )
        if (receiverPublicKeyTrunc == keys.truncatedPublicKey()) {
            encryption.decryptPrivateMessage(dbMessage)
        }
        return dbMessage
    }
}