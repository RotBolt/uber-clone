package io.kaenlabs.uberclone.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import io.kaenlabs.uberclone.data.network.NetworkService
import io.kaenlabs.uberclone.simulator.WebSocket
import io.kaenlabs.uberclone.simulator.WebSocketListener
import io.kaenlabs.uberclone.utils.Constants
import org.json.JSONObject

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {

    companion object {
        private const val TAG = "MapsPresenter"
    }

    var view: MapsView? = null
    lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView) {
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun requestNearByCabs(latLng: LatLng) {
        val jsonObject = JSONObject().apply {
            with(Constants) {
                put(TYPE, NEAR_BY_CABS)
                put(LAT, latLng.latitude)
                put(LNG, latLng.longitude)
            }
        }
        webSocket.sendMessage(jsonObject.toString())
    }


    fun onDetach() {
        webSocket.disconnect()
        view = null
    }

    override fun onConnect() {
        Log.d(TAG, "onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage : $data")
        val jsonObject = JSONObject(data)
        when(jsonObject.get(Constants.TYPE)){
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearByCabs(jsonObject)
            }
        }
    }

    private fun handleOnMessageNearByCabs(jsonObject: JSONObject){
        val nearByCabLocations = arrayListOf<LatLng>()
        val jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS)
        for (i in 0 until jsonArray.length()){
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            nearByCabLocations.add(LatLng(lat,lng))
        }
        view?.showNearByCabs(nearByCabLocations)
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisConnect")
    }

    override fun onError(error: String) {
        Log.e(TAG, "onError : $error")
    }

}