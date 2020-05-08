package io.kaenlabs.uberclone.ui.maps

import android.util.Log
import io.kaenlabs.uberclone.data.network.NetworkService
import io.kaenlabs.uberclone.simulator.WebSocket
import io.kaenlabs.uberclone.simulator.WebSocketListener

class MapsPresenter(private val networkService: NetworkService) :WebSocketListener {

    companion object{
        private const val TAG = "MapsPresenter"
    }

    var view:MapsView? = null
    lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView){
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun onDetach(){
        webSocket.disconnect()
        view =  null
    }

    override fun onConnect() {
        Log.d(TAG,"onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG,"onMessage : $data")
    }

    override fun onDisconnect() {
        Log.d(TAG,"onDisConnect")
    }

    override fun onError(error: String) {
        Log.e(TAG,"onError : $error")
    }

}