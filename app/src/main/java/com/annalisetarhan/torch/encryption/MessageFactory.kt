package com.annalisetarhan.torch.encryption

import android.util.Log
import com.annalisetarhan.torch.BuildConfig
import com.annalisetarhan.torch.DomainMessage
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_IV_LENGTH
import com.annalisetarhan.torch.encryption.Constants.Companion.GCM_TAG_LENGTH
import com.annalisetarhan.torch.encryption.Constants.Companion.RSA_KEY_BITS
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_AN_HOUR
import com.annalisetarhan.torch.encryption.Constants.Companion.SECONDS_IN_A_DAY
import com.annalisetarhan.torch.encryption.Constants.Companion.UNIX_TIMESTAMP_BYTES
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessageFactory {
    private var ttd = SECONDS_IN_A_DAY
    private lateinit var keyPair: KeyPair
    private val sha = MessageDigest.getInstance("SHA")

    init {
        // TODO: check shared prefs for stored keypair, ttd setting
        generateKeyPair()
    }

    fun makeNetworkMessage(dbMessage: DatabaseMessage): NetworkMessage {
        // TODO
    }

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(RSA_KEY_BITS)
        keyPair = keyPairGenerator.genKeyPair()
    }


    /*
        Takes hashkey, hashtag, and message string, returns byte array with
        raw IV and encrypted public key + message
     */
    private fun encrypt(hashkey: ByteArray, hashtag: String, rawMessage: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(hashkey, "AES")
        val publicKey: ByteArray = keyPair.public.encoded

        cipher.init(ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(publicKey + rawMessage.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        // Sanity check
        val gcmSpec = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java)
        if (BuildConfig.DEBUG && gcmSpec.iv.size != GCM_IV_LENGTH) {
            error("Assertion failed - gcmSpec's IV size doesn't match GCM_IV_LENGTH")
        }
        if (BuildConfig.DEBUG && gcmSpec.tLen != GCM_TAG_LENGTH) {
            error("Assertion failed - gcmSpec's tag length doesn't match GCM_TAG_LENGTH")
        }
        if (BuildConfig.DEBUG && iv.size != GCM_IV_LENGTH) {
            error("Assertion failed - IV size doesn't match GCM_IV_LENGTH")
        }

        return iv + ciphertext
    }

    private fun decrypt(hashtag: String, encryptedMessage: ByteArray, messageId: ByteArray): DomainMessage? {
        val hashkey = hash(hashtag)
        val iv = encryptedMessage.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedMessage.copyOfRange(GCM_IV_LENGTH, encryptedMessage.size)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
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
        return if (plaintext?.copyOfRange(0,hashtag.length)?.equals(hashtag) == true) {
            val timeSentStart = hashtag.length
            val senderPublicKeyStart = timeSentStart + UNIX_TIMESTAMP_BYTES
            val messageStart = senderPublicKeyStart + (RSA_KEY_BITS / 8)
            val timeSent = plaintext.copyOfRange(timeSentStart, senderPublicKeyStart)
            val senderPublicKey = plaintext.copyOfRange(senderPublicKeyStart, messageStart)
            val message = plaintext.copyOfRange(messageStart, plaintext.size)
            DomainMessage(
                    messageId,
                    hashtag,
                    timeSent.toString(Charsets.UTF_8).toLong(),
                    senderPublicKey,
                    message.toString()
            )
        } else {
            null
        }
    }

    // Get hashkey from hashtag or create message digest to use as shared messageId
    private fun hash(message: String): ByteArray {
        return sha.digest(message.toByteArray(Charsets.UTF_8))
    }

    private fun getMessageTtd(): Long {
        return System.currentTimeMillis() + ttd
    }

    fun updateTtdSetting(hours: Int) {
        ttd = hours * SECONDS_IN_AN_HOUR
    }
}

class Constants {
    companion object {
        const val SECONDS_IN_A_DAY = 86400
        const val SECONDS_IN_AN_HOUR = 3600
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
        const val UNIX_TIMESTAMP_BYTES = 4
        const val RSA_KEY_BITS = 1024
    }
}