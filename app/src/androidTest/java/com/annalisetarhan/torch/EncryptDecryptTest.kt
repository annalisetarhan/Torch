package com.annalisetarhan.torch

import com.annalisetarhan.torch.encryption.MessageFactory
import kotlin.random.Random

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.connection.bytesToMessageList
import com.annalisetarhan.torch.connection.messageListToBytes
import com.annalisetarhan.torch.database.DatabaseMessage

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptDecryptTest {
    private val factory = MessageFactory(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun encryptDecrypt() {
        for (i in 0..1000) {
            encryptDecryptStandard()
            encryptDecryptPrivate()
        }
    }

    @Test
    fun toBytesAndBack() {
        // Make 101 message lists
        for (i in 0..100) {
            val dbMessageList = mutableListOf<DatabaseMessage>()
            val networkMsgList = mutableListOf<NetworkMessage>()
            val hashtags = Array(101) {""}
            val messages = Array(101) {""}

            // With 101 messages each
            for (j in 0..100) {

                // Generate and save hashtags and messages
                val hashtag = getRandomHashtag()
                val messageText = getRandomStandardMessage()
                hashtags[j] = hashtag
                messages[j] = messageText

                // Construct database and network messages
                val dbMessage = factory.makeStandardDatabaseMessage(hashtag,messageText)
                dbMessageList.add(dbMessage)
                networkMsgList.add(factory.makeNetworkMessage(dbMessage))
            }

            // Convert network message list to bytearray
            val bytes = messageListToBytes(networkMsgList)

            // And convert them back
            val newNetworkMessages = bytesToMessageList(bytes)

            // See if the new network messages have the same content as the originals
            for (j in 0..100) {
                val reconstructed = factory.reconstructStandardMessage(newNetworkMessages[j])
                factory.decryptStandardMessage(hashtags[j], reconstructed)
                assert(reconstructed.hashtag.equals(hashtags[j]))
                assert(reconstructed.message.equals(messages[j]))
            }
        }
    }

    private fun encryptDecryptStandard() {
        val hashtag = getRandomHashtag()
        val rawMessage = getRandomStandardMessage()

        val dbMessage = factory.makeStandardDatabaseMessage(hashtag, rawMessage)
        val networkMessage = factory.makeNetworkMessage(dbMessage)
        val reconstructed = factory.reconstructStandardMessage(networkMessage)

        // This is a workaround because I'm not inserting hashtag into activeHashtags.
        // Normally, reconstruct will do it automatically.
        factory.decryptStandardMessage(hashtag, reconstructed)

        assert(reconstructed.hashtag.equals(hashtag))
        assert(reconstructed.message.equals(rawMessage))
    }

    private fun encryptDecryptPrivate() {
        val rawMessage = getRandomPrivateMessage()

        val dbMessage = factory.makePrivateDatabaseMessage(factory.keys.truncatedPublicKey(), rawMessage)
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

