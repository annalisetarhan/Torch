package com.annalisetarhan.torch.database

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.connection.WiFiConnection
import com.annalisetarhan.torch.database.AppDatabase
import com.annalisetarhan.torch.database.DatabaseMessage
import com.annalisetarhan.torch.encryption.MessageFactory
import com.annalisetarhan.torch.ui.DomainMessage

class Repository(context: Context) {
    private val messageFactory = MessageFactory(context)
    private val network = WiFiConnection(context, Handler(Looper.getMainLooper()))
    val messageDao = AppDatabase.getInstance(context).messageDao

    // TODO: track users, mapping truncated pk to full pk. when a user de/registers from/to a hashtag,
    //  change color code in previous messages (to grey?) if a user deregisters from the only shared hashtag,
    //  forget their full pk

    init {
        // TODO: set up some sort of looper to scan database and discard dead messages

    }

    /* This should only be called with existing active hashtags. Previously decrypted data gets to stay. */
    fun removeHashtag(hashtag: String) = messageFactory.removeHashtag(hashtag)
    fun getPublicKey(): String = messageFactory.getTruncatedPublicKey().toString()
    fun resetKeys() = messageFactory.resetKeys()

    /* Enforce the invariant that the rawMessage is at most 160 characters */
    suspend fun sendStandardMessage(hashtag: String, message: String) {
        val databaseMessage = messageFactory.makeStandardDatabaseMessage(hashtag, message)
        sendMessage(databaseMessage)
    }

    suspend fun sendPrivateMessage(truncPk: Long, message: String) {
        val databaseMessage = messageFactory.makePrivateDatabaseMessage(truncPk, message)
        sendMessage(databaseMessage)
    }

    private suspend fun sendMessage(databaseMessage: DatabaseMessage) {
        messageDao.insert(databaseMessage)
        val networkMessage = messageFactory.makeNetworkMessage(databaseMessage)
        network.sendMessage(networkMessage)
    }

    suspend fun receiveMessage(networkMessage: NetworkMessage) {
        val dbMessage = messageFactory.reconstructStandardMessage(networkMessage)
        messageDao.insert(dbMessage)
    }

    suspend fun addHashtag(hashtag: String): LiveData<List<DomainMessage>> {
        messageFactory.addHashtag(hashtag)

        val mysteriousMessages = messageDao.getAllEncrypted()
        for (message in mysteriousMessages) {
            if (messageFactory.decryptStandardMessage(hashtag, message)) {
                messageDao.addDecryptedInfo(message)
            }
        }

        /* Gets a livedata for messages in the database with the hashtags
        * and transforms each of the database messages into a domain message */
        return Transformations.map(messageDao.getHashtagMessages(hashtag)) { list ->
            val newList = mutableListOf<DomainMessage>()
            for (databaseMessage in list) {
                newList.add(messageFactory.makeDomainMessage(databaseMessage))
            }
            newList
        }
    }

}