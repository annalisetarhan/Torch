package com.annalisetarhan.torch.encryption

import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.TimeConstants.Companion.SECONDS_IN_AN_HOUR
import com.annalisetarhan.torch.encryption.TimeConstants.Companion.SECONDS_IN_A_DAY
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class MessageFactory {
    private var ttd = SECONDS_IN_A_DAY
    private lateinit var keyPair: KeyPair
    private val sha = MessageDigest.getInstance("SHA")
    private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")


    init {
        // TODO: check shared prefs for stored keypair, ttd
        generateKeyPair()
    }

    fun makeDatabaseMessage(hashtag: String, rawMessage: String): DatabaseMessage {
        val hashkey = hash(hashtag)
        val encryptedMessage = encrypt(hashkey, hashtag, rawMessage)
        return DatabaseMessage(
            uuid = UUID.randomUUID(),
            hashkey = hashkey,
            TTD = getMessageTtd(),

        )
    }

    fun makeNetworkMessage(dbMessage: DatabaseMessage): NetworkMessage {}

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(1024)
        keyPair = keyPairGenerator.genKeyPair()
    }

    fun updateTtdSetting(hours: Int) {
        ttd = hours * SECONDS_IN_AN_HOUR
    }

    private fun encrypt(hashkey: ByteArray, hashtag: String, rawMessage: String): ByteArray {

    }

    private fun hash(message: String): ByteArray {
        return sha.digest(message.toByteArray(Charsets.UTF_8))
    }

    private fun getMessageTtd(): Long {
        return System.currentTimeMillis() + ttd
    }
}

class TimeConstants {
    companion object {
        const val SECONDS_IN_A_DAY = 86400
        const val SECONDS_IN_AN_HOUR = 3600
    }
}