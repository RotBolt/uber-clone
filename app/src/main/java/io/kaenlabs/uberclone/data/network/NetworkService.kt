package io.kaenlabs.uberclone.data.network

import io.kaenlabs.uberclone.simulator.WebSocket
import io.kaenlabs.uberclone.simulator.WebSocketListener

class NetworkService {

    fun createWebSocket(webSocketListener: WebSocketListener) : WebSocket{
        return WebSocket(webSocketListener)
    }
}