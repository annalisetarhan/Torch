package com.annalisetarhan.torch

import android.net.wifi.aware.PeerHandle

class Repository {
    val peers = hashMapOf<PeerHandle, Int>()    // <PeerHandle, LastConnectTime> (NextConnectTime?)
    val hashkeys = hashMapOf<String, Int>()     // <HashKey, NumMsgs>
}