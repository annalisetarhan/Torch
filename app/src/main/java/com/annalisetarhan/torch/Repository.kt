package com.annalisetarhan.torch

import android.net.wifi.aware.PeerHandle
import com.annalisetarhan.torch.encryption.MessageFactory
import java.security.KeyPair
import java.security.KeyPairGenerator

class Repository {
    val messageFactory = MessageFactory()

    val peers = hashMapOf<PeerHandle, Int>()    // <PeerHandle, LastConnectTime> (NextConnectTime?)
    val hashkeys = hashMapOf<String, Int>()     // <HashKey, NumMsgs> DELETE? why is this here?
    val activeHashtags = arrayOfNulls<String>(5)

    // TODO: track users, mapping truncated pk to full pk.
    // TODO: when a user de/registers from/to a hashtag, change color code in previous messages
    // TODO?: if a user deregisters from the only shared hashtag, forget their full pk

    /* Enforce the invarient that the rawMessage is at most 160 characters */
    fun sendMessage(hashtag: String, rawMessage: String) {
        messageFactory.makeDatabaseMessage(hashtag, rawMessage)
    }

}