package io.kaenlabs.uberclone.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {

    fun showNearByCabs(latLngList: List<LatLng>)

}