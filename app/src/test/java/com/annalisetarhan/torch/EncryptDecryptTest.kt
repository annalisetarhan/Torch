package com.annalisetarhan.torch

import com.annalisetarhan.torch.encryption.MessageFactory
import org.junit.Test
import kotlin.random.Random

class EncryptDecryptTest {
    private val factory = MessageFactory()

    @Test
    fun encryptDecrypt() {
        for (i in 0..1000) {
            encryptDecryptStandard()
            encryptDecryptPrivate()
        }
    }

    private fun encryptDecryptStandard() {
        val hashtag = getRandomHashtag()
        val rawMessage = getRandomStandardMessage()

        val dbMessage = factory.makeStandardDatabaseMessage(hashtag, rawMessage)
        val networkMessage = factory.makeNetworkMessage(dbMessage)
        val reconstructed = factory.reconstructStandardMessage(networkMessage)

        // This is a workaround because I'm not inserting hashtag into activeHashtags. Normally, reconstruct will do it automatically.
        factory.decryptStandardMessage(hashtag, reconstructed)

        assert(reconstructed.hashtag.equals(hashtag))
        assert(reconstructed.message.equals(rawMessage))
    }

    private fun encryptDecryptPrivate() {
        val rawMessage = getRandomPrivateMessage()

        val dbMessage = factory.makePrivateDatabaseMessage(factory.truncatedPublicKey(), rawMessage)
        val networkMessage = factory.makeNetworkMessage(dbMessage)
        val reconstructed = factory.reconstructPrivateMessage(networkMessage)

        assert(reconstructed.message.equals(rawMessage))
    }

    companion object {
        fun getHashtagLength(): Int = (1..20).random()
        fun getStandardMessageLength(): Int = (1..170).random()     // Limited by WiFi Aware
        fun getPrivateMessageLength(): Int = (1..101).random()      // Limited by RSA key size

        private val charPool : List<Char> = ('!'..'~') + (' ')

        fun getString(length: Int): String {
            return (1..length)
                    .map { _ -> Random.nextInt(0, charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
        }
        fun getRandomHashtag(): String {
            return getString(getHashtagLength())
        }
        fun getRandomStandardMessage(): String {
            return getString(getStandardMessageLength())
        }
        fun getRandomPrivateMessage(): String {
            return getString(getPrivateMessageLength())
        }
    }
}

