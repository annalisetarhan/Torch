package com.annalisetarhan.torch

import android.content.Context
import android.net.wifi.aware.PeerHandle
import android.os.Handler
import android.os.Looper
import androidx.room.Room
import androidx.room.RoomDatabase
import com.annalisetarhan.torch.connection.WiFiConnection
import com.annalisetarhan.torch.database.AppDatabase
import com.annalisetarhan.torch.encryption.MessageFactory
import java.security.KeyPair
import java.security.KeyPairGenerator

class Repository(context: Context) {
    val messageFactory = MessageFactory()
    val network = WiFiConnection(context, Handler(Looper.getMainLooper()))
    val messageDao = AppDatabase.getInstance(context).messageDao

    val activeHashtags = arrayOfNulls<String>(5)

    // TODO: track users, mapping truncated pk to full pk. when a user de/registers from/to a hashtag, change color code in previous messages if a user deregisters from the only shared hashtag, forget their full pk

    /* Enforce the invariant that the rawMessage is at most 160 characters */
    fun sendMessage(hashtag: String, rawMessage: String) {
        val databaseMessage = messageFactory.makeDatabaseMessage(hashtag, rawMessage)
        messageDao.insert(databaseMessage)

        val networkMessage = messageFactory.makeNetworkMessage(databaseMessage)
        network.sendMessage(networkMessage)
    }

}