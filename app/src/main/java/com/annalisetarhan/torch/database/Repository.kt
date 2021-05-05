package com.annalisetarhan.torch.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.annalisetarhan.torch.connection.NetworkMessage
import com.annalisetarhan.torch.connection.WiFiConnection
import com.annalisetarhan.torch.encryption.MessageFactory
import com.annalisetarhan.torch.ui.DomainMessage

class Repository(val context: Context) {

    private val messageFactory = MessageFactory(context)
    private val messageDao = AppDatabase.getInstance(context).messageDao
    private var network = WiFiConnection(context, this)

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
        messageDao.insertInternal(databaseMessage)
        val networkMessage = messageFactory.makeNetworkMessage(databaseMessage)
        network.sendMessage(networkMessage)
    }

    fun receiveMessage(networkMessage: NetworkMessage) {
        val dbMessage = messageFactory.reconstructStandardMessage(networkMessage)
        messageDao.insertExternal(dbMessage)
    }

    suspend fun addHashtag(hashtag: String): LiveData<List<DomainMessage>> {
        messageFactory.addHashtag(hashtag)

        val mysteriousMessages = messageDao.getAllEncrypted()
        for (message in mysteriousMessages) {
            if (messageFactory.decryptStandardMessage(hashtag, message)) {
                messageDao.addDecryptedInfo(message)
            }
        }

        /* Transforms each of the database messages with the hashtag into a
         * domain message and returns a livedata to watch for new ones */
        return Transformations.map(messageDao.getHashtagMessages(hashtag)) { list ->
            val newList = mutableListOf<DomainMessage>()
            for (databaseMessage in list) {
                newList.add(messageFactory.makeDomainMessage(databaseMessage))
            }
            newList
        }
    }

    fun getAllMessages(): List<NetworkMessage> {
        val dbMessages: List<DatabaseMessage> = messageDao.getAllMessages()
        return dbMessages.map { messageFactory.makeNetworkMessage(it) }
    }

    fun purgeDatabase(ttd: Long) {
        messageDao.purge(ttd)
    }

    fun killConnection() {
        network.killConnection()
    }
}