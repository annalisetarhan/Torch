package com.annalisetarhan.torch.encryption

import android.util.Log
import androidx.databinding.library.BuildConfig
import com.annalisetarhan.torch.DomainMessage
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.Constants.Companion.CIPHER_TRANSFORMATION_STANDARD
import com.annalisetarhan.torch.encryption.Constants.Companion.CIPHER_TRANSFORMATION_PRIVATE
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_IV_BYTES
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_TAG_BITS
import com.annalisetarhan.torch.encryption.Constants.Companion.KEY_PAIR_ALGORITHM
import com.annalisetarhan.torch.encryption.Constants.Companion.MESSAGE_DIGEST_ALGORITHM
import com.annalisetarhan.torch.encryption.Constants.Companion.RSA_KEY_BITS
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_AN_HOUR
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_A_DAY
import com.annalisetarhan.torch.encryption.Constants.Companion.SECRET_KEY_ALGORITHM
import com.annalisetarhan.torch.encryption.Constants.Companion.TRUNC_KEY_BYTES
import com.annalisetarhan.torch.encryption.Constants.Companion.TIMESTAMP_BYTES
import java.lang.Exception
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageFactory {
    private var ttd = SECONDS_IN_A_DAY
    private lateinit var keyPair: KeyPair

    val activeHashtags = arrayListOf<String>()
    val fullPublicKeys: MutableMap<ByteArray, RSAPublicKey> = mutableMapOf()

    // TESTING ONLY
    val tempFullPublicKeys: MutableMap<Long, PublicKey> = mutableMapOf()

    // Does this actually need to be 256? It's only for making hashkeys from hashtags
    // and creating message digests of already encrypted messages. It would save 16 bytes in msgs...
    private val sha = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
    init {
        // TODO: check shared prefs for stored keypair, ttd setting
        generateKeyPair()
        // FOR TESTING ONLY
        tempFullPublicKeys[truncatedPublicKey()] = keyPair.public
    }

    fun addHashtag(hashtag: String) {
        activeHashtags.add(hashtag)
    }

    fun removeHashtag(hashtag: String) {
        activeHashtags.remove(hashtag)
    }

    fun makeStandardDatabaseMessage(hashtag: String, rawMessage: String): DatabaseMessage {
        val timeSent: Long = currentTimeInSecs()
        val encMessage: ByteArray = encryptStandardMessage(hashtag, timeSent, rawMessage)

        return DatabaseMessage(
            uuid = UUID.randomUUID().toString(),
            messageId = hash(encMessage.toString(Charsets.UTF_8)),
            ttd = getMessageTtd(timeSent),
            messageInfo = 1,
            encMessage = encMessage,
            hashtag = hashtag,
            timeSent = timeSent,
            senderPublicKeyTrunc = truncatedPublicKey(),
            message = rawMessage
        )
    }

    fun makePrivateDatabaseMessage(receiverPublicKeyTrunc: Long, rawMessage: String): DatabaseMessage {
        val timeSent: Long = currentTimeInSecs()
        val encMessage: ByteArray = encryptPrivateMessage(receiverPublicKeyTrunc, timeSent, rawMessage)

        return DatabaseMessage(
            uuid = UUID.randomUUID().toString(),
            messageId = hash(encMessage.toString(Charsets.UTF_8)),
            ttd = getMessageTtd(timeSent),
            messageInfo = 2,
            encMessage = longToByteArray(receiverPublicKeyTrunc) + encMessage,
            receiverPublicKeyTrunc = receiverPublicKeyTrunc,
            timeSent = timeSent,
            senderPublicKeyTrunc = truncatedPublicKey(),
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
                message.senderPublicKeyTrunc == truncatedPublicKey(),
                message.message!!
        )
    }

    fun reconstructStandardMessage(networkMessage: NetworkMessage): DatabaseMessage {
        val encMessage = networkMessage.payload
        val dbMessage = DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = hash(encMessage.toString(Charsets.UTF_8)),     // Can't just use networkMessage's id because it was truncated
                ttd = longFromByteArray(networkMessage.ttd),
                messageInfo = 1,
                encMessage = encMessage
        )
        for (hashtag in activeHashtags) {
            if (decryptStandardMessage(hashtag, dbMessage)) break
        }
        return dbMessage
    }

    fun reconstructPrivateMessage(networkMessage: NetworkMessage): DatabaseMessage {
        val encMessage = networkMessage.payload
        val receiverPublicKeyTrunc = longFromByteArray(encMessage.copyOfRange(0, TRUNC_KEY_BYTES))
        val dbMessage = DatabaseMessage(
            uuid = UUID.randomUUID().toString(),
            messageId = hash(encMessage.toString(Charsets.UTF_8)), // Can't just use networkMessage's id because it was truncated
            ttd = longFromByteArray(networkMessage.ttd),
            messageInfo = 2,
            encMessage = encMessage,
            receiverPublicKeyTrunc = receiverPublicKeyTrunc
        )
        if (receiverPublicKeyTrunc == truncatedPublicKey()) {
            decryptPrivateMessage(dbMessage)
        }
        return dbMessage
    }

    /*
        ENCRYPTION / DECRYPTION
     */

    // Returns: rawIV + encrypted(hashtag + timeSent + senderPkTrunc + message)
    private fun encryptStandardMessage(hashtag: String, timeSent: Long, rawMessage: String): ByteArray {
        val hashkey = hash(hashtag)
        val key = SecretKeySpec(hashkey, SECRET_KEY_ALGORITHM)
        val publicKey: Long = truncatedPublicKey()

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_STANDARD)
        cipher.init(ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(
                hashtag.toByteArray(Charsets.UTF_8) +
                        longToByteArray(timeSent) +
                        longToByteArray(publicKey) +
                        rawMessage.toByteArray(Charsets.UTF_8)
        )
        val iv = cipher.iv

        // Sanity check
        val gcmSpec = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java)
        if (BuildConfig.DEBUG && gcmSpec.iv.size != GCM_IV_BYTES) {
            error("Assertion failed - gcmSpec's IV size doesn't match GCM_IV_LENGTH")
        }
        if (BuildConfig.DEBUG && gcmSpec.tLen != GCM_TAG_BITS) {
            error("Assertion failed - gcmSpec's tag length doesn't match GCM_TAG_LENGTH_BITS")
        }
        if (BuildConfig.DEBUG && iv.size != GCM_IV_BYTES) {
            error("Assertion failed - IV size doesn't match GCM_IV_LENGTH")
        }

        return iv + ciphertext
    }

    // Maximum message length seems to be 101 bytes
    private fun encryptPrivateMessage(receiverPublicKeyTrunc: Long, timeSent: Long, rawMessage: String): ByteArray {
        val receiverPublicKeyFull = tempFullPublicKeys[receiverPublicKeyTrunc]
        val senderPublicKeyTrunc = truncatedPublicKey()

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_PRIVATE)
        cipher.init(ENCRYPT_MODE, receiverPublicKeyFull)

        return cipher.doFinal(
            longToByteArray(timeSent) +
                    longToByteArray(senderPublicKeyTrunc) +
                    rawMessage.toByteArray(Charsets.UTF_8)
        )
    }

    // Returns true if the hashtag was the right one, false otherwise. Updates dbMessage fields. MUST update database afterwards!!
    fun decryptStandardMessage(hashtag: String, dbMessage: DatabaseMessage): Boolean {
        val hashkey = hash(hashtag)
        val iv = dbMessage.encMessage.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = dbMessage.encMessage.copyOfRange(GCM_IV_BYTES, dbMessage.encMessage.size)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        val key = SecretKeySpec(hashkey, SECRET_KEY_ALGORITHM)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_STANDARD)
        cipher.init(DECRYPT_MODE, key, gcmSpec)

        /* There's debate about whether you should ever try/catch in Kotlin,
            but the alternatives seem iffy. I'm doing it, smell or not. */
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.w("DECRYPT", "Error decrypting standard message"  + e.stackTrace)
            e.message?.let { Log.w("DECRYPT", it) }
            null
        }
        return if (plaintext?.copyOfRange(0,hashtag.length)?.toString(Charsets.UTF_8) == hashtag) {
            val timeSentStart = hashtag.length
            val senderPublicKeyStart = timeSentStart + Long.SIZE_BYTES
            val messageStart = senderPublicKeyStart + TRUNC_KEY_BYTES

            val timeSent = plaintext.copyOfRange(timeSentStart, senderPublicKeyStart)
            val senderPublicKey = plaintext.copyOfRange(senderPublicKeyStart, messageStart)
            val message = plaintext.copyOfRange(messageStart, plaintext.size)

            dbMessage.hashtag = hashtag
            dbMessage.timeSent = longFromByteArray(timeSent)
            dbMessage.senderPublicKeyTrunc = longFromByteArray(senderPublicKey)
            dbMessage.message = message.toString(Charsets.UTF_8)

            true
        } else {
            false
        }
    }

    fun decryptPrivateMessage(dbMessage: DatabaseMessage) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION_PRIVATE)
        val privateKey = keyPair.private
        cipher.init(DECRYPT_MODE, privateKey)

        val payload = dbMessage.encMessage
        val encMessage = payload.copyOfRange(TRUNC_KEY_BYTES, payload.size)

        val plaintext = try {
            cipher.doFinal(encMessage)
        } catch (e: Exception) {
            Log.w("DECRYPT", "Error decrypting private message" + e.stackTrace)
            e.message?.let { Log.w("DECRYPT", it) }
            return
        }
        val publicKeyStart = TIMESTAMP_BYTES
        val messageStart = TIMESTAMP_BYTES + TRUNC_KEY_BYTES

        val timeSent = plaintext.copyOfRange(0, publicKeyStart)
        val senderPublicKeyTrunc = plaintext.copyOfRange(publicKeyStart, messageStart)
        val message = plaintext.copyOfRange(messageStart, plaintext.size)

        dbMessage.timeSent = longFromByteArray(timeSent)
        dbMessage.senderPublicKeyTrunc = longFromByteArray(senderPublicKeyTrunc)
        dbMessage.message = message.toString(Charsets.UTF_8)
    }

    // Get hashkey from hashtag or create message digest to use as shared messageId
    private fun hash(message: String): ByteArray {
        return sha.digest(message.toByteArray(Charsets.UTF_8))
    }

    /*
        UTILITY FUNCTIONS
     */

    // Number <-> ByteArray
    private fun longToByteArray(long: Long): ByteArray {
        val bytes = ByteArray(Long.SIZE_BYTES)
        ByteBuffer.wrap(bytes).putLong(long)
        return bytes
    }

    private fun intToByteArray(int: Int): ByteArray {
        val bytes = ByteArray(Int.SIZE_BYTES)
        ByteBuffer.wrap(bytes).putInt(int)
        return bytes
    }

    private fun longFromByteArray(bytes: ByteArray): Long =  ByteBuffer.wrap(bytes).long
    private fun intFromByteArray(bytes: ByteArray): Int = ByteBuffer.wrap(bytes).int

    // Time
    private fun getMessageTtd(timeSent: Long): Long = timeSent + ttd

    fun currentTimeInSecs(): Long = System.currentTimeMillis()/1000

    fun updateTtdSetting(hours: Int) {
        ttd = hours * SECONDS_IN_AN_HOUR
    }

    // Keys
    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM)
        keyPairGenerator.initialize(RSA_KEY_BITS)
        keyPair = keyPairGenerator.generateKeyPair()
    }

    fun truncatedPublicKey(): Long {
        val publicKey = keyPair.public as RSAPublicKey
        val truncated = publicKey.modulus % Long.MAX_VALUE.toBigInteger()
        return truncated.toLong()
    }
}

class Constants {
    companion object {
        const val SECONDS_IN_A_DAY = 86400
        const val SECONDS_IN_AN_HOUR = 3600

        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val RSA_KEY_BITS = 1024
        const val TRUNC_KEY_BYTES = Long.SIZE_BYTES
        const val TIMESTAMP_BYTES = 8
        const val MESSAGE_INFO_BYTES = 1

        const val CIPHER_TRANSFORMATION_STANDARD = "AES/GCM/NoPadding"
        const val CIPHER_TRANSFORMATION_PRIVATE = "RSA/ECB/PKCS1Padding"
        const val KEY_PAIR_ALGORITHM = "RSA"
        const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"
        const val SECRET_KEY_ALGORITHM = "AES"
    }
}