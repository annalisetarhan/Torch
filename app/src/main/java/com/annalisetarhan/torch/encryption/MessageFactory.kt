package com.annalisetarhan.torch.encryption

import android.util.Log
import com.annalisetarhan.torch.BuildConfig
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_IV_BYTES
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_TAG_BITS
import com.annalisetarhan.torch.encryption.Constants.Companion.RSA_KEY_BITS
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_AN_HOUR
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_A_DAY
import com.annalisetarhan.torch.encryption.Constants.Companion.TRUNC_KEY_BYTES
import com.annalisetarhan.torch.encryption.Constants.Companion.TTD_BYTES
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.util.*
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageFactory {
    private var ttd = SECONDS_IN_A_DAY
    private lateinit var keyPair: KeyPair

    val activeHashtags = arrayListOf<String>()

    // Does this actually need to be 256? It's only for making hashkeys from hashtags
    // and creating message digests of already encrypted messages. It would save 16 bytes in msgs...
    private val sha = MessageDigest.getInstance("SHA-256")
    init {
        // TODO: check shared prefs for stored keypair, ttd setting
        generateKeyPair()
    }

    fun addHashtag(hashtag: String) {
        activeHashtags.add(hashtag)
    }

    fun removeHashtag(hashtag: String) {
        activeHashtags.remove(hashtag)
    }

    fun makeDatabaseMessage(hashtag: String, rawMessage: String): DatabaseMessage {
        val timeSent: Long = currentTimeInSecs()
        val encMessage: ByteArray = encrypt(hashtag, timeSent, rawMessage)

        return DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = hash(encMessage.toString(Charsets.UTF_8)),
                ttd = getMessageTtd(timeSent),
                encMessage = encMessage,
                hashtag = hashtag,
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
                encMessage = dbMessage.encMessage
        )
    }

    fun reconstructDatabaseMessage(networkMessage: NetworkMessage): DatabaseMessage {
        val encMessage = networkMessage.encMessage
        val msgWithoutIvOrTtd = encMessage.copyOfRange(GCM_IV_BYTES + TTD_BYTES, encMessage.size)
        val dbMessage = DatabaseMessage(
                uuid = UUID.randomUUID().toString(),
                messageId = hash(msgWithoutIvOrTtd.toString(Charsets.UTF_8)),
                ttd = longFromByteArray(networkMessage.ttd),
                encMessage = networkMessage.encMessage
        )
        for (hashtag in activeHashtags) {
            if (decrypt(hashtag, dbMessage)) {
                break
            }
        }
        return dbMessage
    }

    /*
        ENCRYPTION / DECRYPTION
     */

    // Returns: rawIV + encrypted(hashtag + timeSent + senderPkTrunc + message)
    private fun encrypt(hashtag: String, timeSent: Long, rawMessage: String): ByteArray {
        val hashkey = hash(hashtag)
        val key = SecretKeySpec(hashkey, "AES")
        val publicKey: ByteArray = truncatedPublicKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(
                hashtag.toByteArray(Charsets.UTF_8) +
                        longToByteArray(timeSent) +
                        publicKey +
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

    // Returns true if the hashtag was the right one, false otherwise. Updates dbMessage fields. MUST update database afterwards!!
    fun decrypt(hashtag: String, dbMessage: DatabaseMessage): Boolean {
        val hashkey = hash(hashtag)
        val iv = dbMessage.encMessage.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = dbMessage.encMessage.copyOfRange(GCM_IV_BYTES, dbMessage.encMessage.size)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)
        val key = SecretKeySpec(hashkey, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(DECRYPT_MODE, key, gcmSpec)

        /* There's debate about whether you should ever try/catch in Kotlin,
            but the alternatives seem iffy. I'm doing it, smell or not. */
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            Log.w("DECRYPT", "Bad Authentication Tag in received message")
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
            dbMessage.senderPublicKeyTrunc = senderPublicKey
            dbMessage.message = message.toString(Charsets.UTF_8)

            true
        } else {
            false
        }
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

    private fun currentTimeInSecs(): Long = System.currentTimeMillis()/1000

    fun updateTtdSetting(hours: Int) {
        ttd = hours * SECONDS_IN_AN_HOUR
    }

    // Keys
    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(RSA_KEY_BITS)
        keyPair = keyPairGenerator.genKeyPair()
    }

    private fun truncatedPublicKey(): ByteArray = keyPair.public.encoded.copyOfRange(0, TRUNC_KEY_BYTES)
}

class Constants {
    companion object {
        const val SECONDS_IN_A_DAY = 86400
        const val SECONDS_IN_AN_HOUR = 3600
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val RSA_KEY_BITS = 1024
        const val TRUNC_KEY_BYTES = 10
        const val TTD_BYTES = 8
    }
}