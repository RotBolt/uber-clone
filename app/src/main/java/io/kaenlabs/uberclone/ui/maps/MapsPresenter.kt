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

    fun requestACab(pickupLatLng: LatLng, dropLatLng: LatLng) {
        val jsonObject = JSONObject().apply {
            with(Constants) {
                put(TYPE, REQUEST_CAB)
                put(PICKUP_LAT, pickupLatLng.latitude)
                put(PICKUP_LNG, pickupLatLng.longitude)
                put(DROP_LAT, dropLatLng.latitude)
                put(DROP_LNG, dropLatLng.longitude)
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
        when (jsonObject.get(Constants.TYPE)) {
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearByCabs(jsonObject)
            }
            Constants.CAB_BOOKED -> {
                view?.informCabBooked()
            }
            Constants.PICKUP_PATH, Constants.TRIP_PATH -> {
                handleOnMessageRequestACab(jsonObject)
            }
            Constants.LOCATION -> {
                val lat = jsonObject.getDouble(Constants.LAT)
                val lng = jsonObject.getDouble(Constants.LNG)
                view?.updateCabLocation(LatLng(lat, lng))

            }
            Constants.CAB_IS_ARRIVING -> {
                view?.informCabIsArriving()
            }
            Constants.CAB_ARRIVED -> {
                view?.informCabArrived()
            }
            Constants.TRIP_START -> {
                view?.informTripStart()
            }
            Constants.TRIP_END -> {
                view?.informTripEnd()
            }
        }
    }

    private fun handleOnMessageRequestACab(jsonObject: JSONObject) {
        val jsonArray = jsonObject.getJSONArray(Constants.PATH)
        val pickUpPath = arrayListOf<LatLng>()
        for (i in 0 until jsonArray.length()) {
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            pickUpPath.add(LatLng(lat, lng))
        }
        view?.showPath(pickUpPath)
    }

    private fun handleOnMessageNearByCabs(jsonObject: JSONObject) {
        val nearByCabLocations = arrayListOf<LatLng>()
        val jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS)
        for (i in 0 until jsonArray.length()) {
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            nearByCabLocations.add(LatLng(lat, lng))
        }
        view?.showNearByCabs(nearByCabLocations)
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisConnect")
    }

    override fun onError(error: String) {
        Log.e(TAG, "onError : $error")
        val jsonObject = JSONObject(error)
        when (jsonObject.getString(Constants.TYPE)) {
            Constants.ROUTES_NOT_AVAILABLE -> {
                view?.showRoutesNotAvailableError()
            }
            Constants.DIRECTION_API_FAILED -> {
                view?.showDirectionAPiFailedError(
                    "Direction Api failed : ${jsonObject.getString(
                        Constants.ERROR
                    )}"
                )
            }
        }
    }

}