package com.annalisetarhan.torch.encryption

import android.util.Log
import androidx.databinding.library.BuildConfig
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.longFromByteArray
import com.annalisetarhan.torch.encryption.NumberConversions.Companion.longToByteArray
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class Encryption(val keys: Keys) {
    private val sha = MessageDigest.getInstance(Constants.MESSAGE_DIGEST_ALGORITHM)
    private val fullPublicKeys: MutableMap<Long, RSAPublicKey> = mutableMapOf()


    /* TESTING ONLY   val tempFullPublicKeys = mutableMapOf<Long, RSAPublicKey>()*/
    /* TESTING ONLY    init { tempFullPublicKeys[keys.truncatedPublicKey()] = keys.publicKey() as RSAPublicKey }*/

    // Returns: rawIV + encrypted(hashtag + timeSent + senderPkTrunc + message)
    fun encryptStandardMessage(hashtag: String, timeSent: Long, rawMessage: String): ByteArray {
        val hashkey = hash(hashtag)
        val key = SecretKeySpec(hashkey, Constants.SECRET_KEY_ALGORITHM)
        val publicKey: Long = keys.truncatedPublicKey()

        val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION_STANDARD)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(
                hashtag.toByteArray(Charsets.UTF_8) +
                        longToByteArray(timeSent) +
                        longToByteArray(publicKey) +
                        rawMessage.toByteArray(Charsets.UTF_8)
        )
        val iv = cipher.iv

        // Sanity check
        val gcmSpec = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java)
        if (BuildConfig.DEBUG && gcmSpec.iv.size != Constants.GCM_IV_BYTES) {
            error("Assertion failed - gcmSpec's IV size doesn't match GCM_IV_LENGTH")
        }
        if (BuildConfig.DEBUG && gcmSpec.tLen != Constants.GCM_TAG_BITS) {
            error("Assertion failed - gcmSpec's tag length doesn't match GCM_TAG_LENGTH_BITS")
        }
        if (BuildConfig.DEBUG && iv.size != Constants.GCM_IV_BYTES) {
            error("Assertion failed - IV size doesn't match GCM_IV_LENGTH")
        }

        return iv + ciphertext
    }

    // Maximum message length seems to be 101 bytes
    fun encryptPrivateMessage(receiverPublicKeyTrunc: Long, timeSent: Long, rawMessage: String): ByteArray {
        /*TESTING ONLY     val receiverPublicKeyFull = tempFullPublicKeys[receiverPublicKeyTrunc] as RSAPublicKey*/
        val receiverPublicKeyFull: RSAPublicKey? = fullPublicKeys[receiverPublicKeyTrunc]
        val senderPublicKeyTrunc = keys.truncatedPublicKey()

        val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION_PRIVATE)
        cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKeyFull)

        return cipher.doFinal(
                longToByteArray(timeSent) +
                        longToByteArray(senderPublicKeyTrunc) +
                        rawMessage.toByteArray(Charsets.UTF_8)
        )
    }

    // Returns true if the hashtag was the right one, false otherwise. Updates dbMessage fields. MUST update database afterwards!!
    fun decryptStandardMessage(hashtag: String, dbMessage: DatabaseMessage): Boolean {
        val hashkey = hash(hashtag)
        val iv = dbMessage.encMessage.copyOfRange(0, Constants.GCM_IV_BYTES)
        val ciphertext = dbMessage.encMessage.copyOfRange(Constants.GCM_IV_BYTES, dbMessage.encMessage.size)
        val gcmSpec = GCMParameterSpec(Constants.GCM_TAG_BITS, iv)
        val key = SecretKeySpec(hashkey, Constants.SECRET_KEY_ALGORITHM)

        val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION_STANDARD)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        /* There's debate about whether you should ever try/catch in Kotlin,
            but the alternatives seem iffy. I'm doing it, smell or not. */
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.w("DECRYPT", "Error decrypting standard message" + e.stackTrace)
            e.message?.let { Log.w("DECRYPT", it) }
            null
        }
        return if (plaintext?.copyOfRange(0, hashtag.length)?.toString(Charsets.UTF_8) == hashtag) {
            val timeSentStart = hashtag.length
            val senderPublicKeyStart = timeSentStart + Long.SIZE_BYTES
            val messageStart = senderPublicKeyStart + Constants.TRUNC_KEY_BYTES

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
        val cipher = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION_PRIVATE)
        val privateKey = keys.privateKey()
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        val payload = dbMessage.encMessage
        val encMessage = payload.copyOfRange(Constants.TRUNC_KEY_BYTES, payload.size)

        val plaintext = try {
            cipher.doFinal(encMessage)
        } catch (e: Exception) {
            Log.w("DECRYPT", "Error decrypting private message" + e.stackTrace)
            e.message?.let { Log.w("DECRYPT", it) }
            return
        }
        val publicKeyStart = Constants.TIMESTAMP_BYTES
        val messageStart = Constants.TIMESTAMP_BYTES + Constants.TRUNC_KEY_BYTES

        val timeSent = plaintext.copyOfRange(0, publicKeyStart)
        val senderPublicKeyTrunc = plaintext.copyOfRange(publicKeyStart, messageStart)
        val message = plaintext.copyOfRange(messageStart, plaintext.size)

        dbMessage.timeSent = longFromByteArray(timeSent)
        dbMessage.senderPublicKeyTrunc = longFromByteArray(senderPublicKeyTrunc)
        dbMessage.message = message.toString(Charsets.UTF_8)
    }

    // Get hashkey from hashtag or create message digest to use as shared messageId
    fun hash(message: String): ByteArray {
        return sha.digest(message.toByteArray(Charsets.UTF_8))
    }

}