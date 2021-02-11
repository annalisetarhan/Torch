package com.annalisetarhan.torch.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.aware.*
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.annalisetarhan.torch.Repository
import java.security.KeyPair
import java.security.KeyPairGenerator

class WiFiConnection(val context: Context, val handler: Handler) {
    val repository = Repository(context) // TODO: dependency injection

    val manager: WifiAwareManager? = context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
    var wifiSession: WifiAwareSession? = null
    var publishSession: PublishDiscoverySession? = null

    val peers = hashMapOf<PeerHandle, Int>()    // <PeerHandle, LastConnectTime>

    var attachFailedFlag = false

    init {
        /* Create and register a broadcast receiver to track changes in WiFi Aware availability */
        val filter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        val myReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                wifiSession = null
                if (manager?.isAvailable == true) {
                    getSession()
                } else {
                    Toast.makeText(context, "WiFi Aware not available. Messages may not send or be received.", Toast.LENGTH_LONG).show()
                }
            }
        }
        context.registerReceiver(myReceiver, filter)

        getSession()        // Assuming the receiver doesn't get some sort of auto-current-state response, which would trigger a getSession()
        startPublishing()
        startSubscribing()
    }

    /* Start up WiFi Aware and join or form cluster */
    private fun getSession() {
        manager?.attach(object : AttachCallback() {
            /* Tries twice to attach, then announces failure. */
            override fun onAttachFailed() {
                super.onAttachFailed()
                if (attachFailedFlag) {
                    Toast.makeText(context, "WiFi Aware not available. Messages may not send or be received.", Toast.LENGTH_LONG).show()
                    attachFailedFlag = false
                } else {
                    attachFailedFlag = true
                    getSession()
                }
            }

            override fun onAttached(awareSession: WifiAwareSession?) {
                super.onAttached(awareSession)
                attachFailedFlag = false
                wifiSession = awareSession

            }
        }, handler)
    }

    private fun startPublishing() {
        val config: PublishConfig = PublishConfig.Builder()
                .setServiceName("Torch")
                .build()
        wifiSession?.publish(config, object : DiscoverySessionCallback()
        {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                super.onPublishStarted(session)
                publishSession = session
            }

            // This will be an announcement that a new peer has discovered this publish
            override fun onMessageReceived(peerHandle: PeerHandle?, message: ByteArray?) {
                super.onMessageReceived(peerHandle, message)
                if (peerHandle != null) {
                    peers[peerHandle] = 0
                }
            }
        }, handler)
    }

    private fun startSubscribing() {}

    fun connectToPeer(peer: PeerHandle, msgList: String) {}

    fun sendMessage(peer: PeerHandle, message: NetworkMessage) {
        publishSession?.sendMessage(peer, message.messageId.toString().toInt(), )
    }

    fun endSession() {
        wifiSession?.close()
    }
}

