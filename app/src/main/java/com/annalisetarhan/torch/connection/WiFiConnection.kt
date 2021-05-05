package com.annalisetarhan.torch.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.widget.Toast
import com.annalisetarhan.torch.database.Repository
import com.annalisetarhan.torch.encryption.Constants.Companion.MAX_MESSAGE_SIZE
import com.annalisetarhan.torch.encryption.Time.Companion.currentTime
import java.net.ServerSocket


class WiFiConnection(val context: Context, private val repo: Repository) {
    val manager: WifiAwareManager? = context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager?
    var wifiSession: WifiAwareSession? = null
    var publishSession: PublishDiscoverySession? = null
    var subscribeSession: SubscribeDiscoverySession? = null

    /* Used to receive messages on a background thread */
    val handlerThread: HandlerThread = HandlerThread("HandlerThread")
    var handler: Handler

    /* Used to purge dead messages from database */
    private lateinit var runnable: Runnable

    /* Used for one-on-one connections */
    private val serverSocket = ServerSocket(0)      // Port = 0 means system chooses port
    private val port = serverSocket.localPort

    /* Map of each peer handle to last connection time.
    Used by publishers to send out new messages, used
    by subscribers to choose a peer to connect with. */
    private val peers = hashMapOf<PeerHandle, Long>()

    init {
        handlerThread.start()
        handler = object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                repo.receiveMessage(msg.obj as NetworkMessage)
            }
        }

        /* Create and register a broadcast receiver to track changes in WiFi Aware availability */
        val filter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                wifiSession = null
                if (manager?.isAvailable == true) {
                    getSession()
                } else {
                    Toast.makeText(context, "WiFi Aware not available. Messages may not send or be received.", Toast.LENGTH_LONG).show()
                }
            }
        }
        context.registerReceiver(receiver, filter)

        /* More WiFi Aware setup */
        startPublishing()
        getSession()
        startSubscribing()

        /* Once per minute, purge dead messages from database and compare databases with a peer */
        runnable = Runnable {
            connectToSomeone()
            repo.purgeDatabase(currentTime())
            handler.postDelayed(runnable, 60000)
        }
        handler.post(runnable)
    }

    /*
        MESSAGE EXCHANGE
     */

    /* Send newly created message (as a publisher) */
    fun sendMessage(message: NetworkMessage) {
        for (peer in peers.keys) {
            publishSession?.sendMessage(
                    peer,
                    message.networkId!!,
                    message.ttd + message.messageInfo + message.payload
            )
        }
    }

    /* Choose least recently contacted peer and connect with them (as a subscriber) */
    private fun connectToSomeone() {
        val leastRecentlyContactedPeer: PeerHandle = peers.minByOrNull { it.value }?.key ?: return
        peers[leastRecentlyContactedPeer] = currentTime()
        connectToPeer(leastRecentlyContactedPeer)
    }

    /* Start a connection (as subscriber) */
    private fun connectToPeer(peer: PeerHandle) {
        val allMessages: List<NetworkMessage> = repo.getAllMessages()
        if (allMessages.isEmpty()) return
        val bytes = messageListToBytes(allMessages)

        /* Request a WiFi Aware network */
        requestSubscriberNetwork(peer, bytes)

        /* Send message to peer to request that they start a connection as well */
        subscribeSession?.sendMessage(peer, 0, byteArrayOf(0.toByte()))

        /* Actual message exchange code is in the onAvailable callbacks below */
    }

    private fun requestSubscriberNetwork(peer: PeerHandle, messages: ByteArray) {
        val networkSpecifier = subscribeSession?.let {
            WifiAwareNetworkSpecifier.Builder(it, peer).build()
        }
        val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .setNetworkSpecifier(networkSpecifier)
                .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            var peerAwareInfo: WifiAwareNetworkInfo? = null

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                peerAwareInfo = networkCapabilities.transportInfo as WifiAwareNetworkInfo
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val peerIpv6 = peerAwareInfo?.peerIpv6Addr
                val peerPort = peerAwareInfo?.port
                val socket = peerPort?.let { network.socketFactory.createSocket(peerIpv6, it) }

                socket?.getOutputStream()?.write(messages)
                socket?.close()
            }
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, callback)
    }

    private fun requestPublisherNetwork(peer: PeerHandle) {
        val networkSpecifier = publishSession?.let {
            WifiAwareNetworkSpecifier.Builder(it, peer)
                    .setPort(port)
                    .build()
        }
        val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                .setNetworkSpecifier(networkSpecifier)
                .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                /* Read messages from socket */
                val client = serverSocket.accept()
                val stream = client.getInputStream()
                val bytes = ByteArray(MAX_MESSAGE_SIZE)
                while (stream.read(bytes) > 0) {
                    acceptMessage(bytesToMessage(bytes))
                }
                client.close()
            }
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, callback)
    }

    /*
        WIFI AWARE INITIALIZATION
     */

    /* Start up WiFi Aware and join or form cluster */
    var attachFailedFlag = false
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

        val callback = object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                super.onPublishStarted(session)
                publishSession = session
            }

            /* Publishers receive invitations to connect */
            override fun onMessageReceived(peerHandle: PeerHandle?, message: ByteArray?) {
                super.onMessageReceived(peerHandle, message)

                /* If this is the first time seeing this peer, add to peers */
                if (peerHandle != null && !peers.containsKey(peerHandle)) {
                    peers[peerHandle] = 0   // Zero because connection has never been established
                }
                if (peerHandle != null) {
                    requestPublisherNetwork(peerHandle)
                }
            }

        }
        wifiSession?.publish(config, callback, handler)
    }

    private fun startSubscribing() {
        val config: SubscribeConfig = SubscribeConfig.Builder()
                .setServiceName("Torch")
                .build()
        val callback = object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                super.onSubscribeStarted(session)
                subscribeSession = session
            }

            override fun onServiceDiscovered(peerHandle: PeerHandle?, serviceSpecificInfo: ByteArray?, matchFilter: MutableList<ByteArray>?) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter)
                if (peerHandle != null) {
                    connectToPeer(peerHandle)
                }
            }

            /* Subscribers receive NetworkMessages */
            override fun onMessageReceived(peerHandle: PeerHandle?, message: ByteArray?) {
                super.onMessageReceived(peerHandle, message)
                if (message == null) return
                acceptMessage(bytesToMessage(message))
            }
        }
        wifiSession?.subscribe(config, callback, handler)
    }

    private fun acceptMessage(networkMessage: NetworkMessage) {
        val handlerMessage = Message.obtain()
        handlerMessage.obj = networkMessage
        handler.sendMessage(handlerMessage)
    }

    fun killConnection() {
        publishSession?.close()
        subscribeSession?.close()
        wifiSession?.close()
        handlerThread.quitSafely()
    }
}