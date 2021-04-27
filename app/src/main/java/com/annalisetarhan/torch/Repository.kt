package com.annalisetarhan.torch

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.connection.WiFiConnection
import com.annalisetarhan.torch.database.AppDatabase
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.MessageFactory

class Repository(context: Context) {
    val messageFactory = MessageFactory()
    val network = WiFiConnection(context, Handler(Looper.getMainLooper()))
    val messageDao = AppDatabase.getInstance(context).messageDao

    var numActiveHashtags = 0

    // TODO: track users, mapping truncated pk to full pk. when a user de/registers from/to a hashtag,
    //  change color code in previous messages (to grey?) if a user deregisters from the only shared hashtag,
    //  forget their full pk

    init {
        // TODO: get active hashtags from sharedPrefs, feed them to messageFactory
        // TODO: set up some sort of looper to scan database and discard dead messages
    }

    /* Enforce the invariant that the rawMessage is at most 160 characters */
    fun sendStandardMessage(hashtag: String, message: String) {
        val databaseMessage = messageFactory.makeStandardDatabaseMessage(hashtag, message)
        sendMessage(databaseMessage)
    }

    fun sendPrivateMessage(truncPk: Long, message: String) {
        val databaseMessage = messageFactory.makePrivateDatabaseMessage(truncPk, message)
        sendMessage(databaseMessage)
    }

    private fun sendMessage(databaseMessage: DatabaseMessage) {
        messageDao.insert(databaseMessage)
        val networkMessage = messageFactory.makeNetworkMessage(databaseMessage)
        network.sendMessage(networkMessage)
    }

    fun receiveMessage(networkMessage: NetworkMessage) {
        val dbMessage = messageFactory.reconstructStandardMessage(networkMessage)
        messageDao.insert(dbMessage)
    }

    /* This should never be called if numActiveHashtags > 4 */
    fun addHashtag(hashtag: String) {
        messageFactory.addHashtag(hashtag)
        numActiveHashtags++

        val mysteriousMessages = messageDao.getAllEncrypted()
        for (message in mysteriousMessages) {
            if (messageFactory.decryptStandardMessage(hashtag, message)) {
                messageDao.addDecryptedInfo(message)
                // TODO: liveData this so the view populates automatically
            }
        }
    }

    /* This should only be called with existing active hashtags. Previously decrypted data gets to stay. */
    fun removeHashtag(hashtag: String) {
        messageFactory.removeHashtag(hashtag)
        numActiveHashtags--
    }
}