package com.annalisetarhan.torch

import android.net.wifi.aware.PeerHandle
import java.security.KeyPair
import java.security.KeyPairGenerator

class Repository {
    val peers = hashMapOf<PeerHandle, Int>()    // <PeerHandle, LastConnectTime> (NextConnectTime?)
    val hashkeys = hashMapOf<String, Int>()     // <HashKey, NumMsgs> DELETE? why is this here?
    val activeHashtags = arrayOfNulls<String>(5)

    fun sendMessage(rawMessage: String) {

    }

}