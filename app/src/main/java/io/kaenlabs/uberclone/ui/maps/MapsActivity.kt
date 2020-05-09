package io.kaenlabs.uberclone.ui.maps

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

import io.kaenlabs.uberclone.R
import io.kaenlabs.uberclone.data.network.NetworkService
import io.kaenlabs.uberclone.utils.AnimationUtils
import io.kaenlabs.uberclone.utils.MapUtils
import io.kaenlabs.uberclone.utils.PermissionUtils
import io.kaenlabs.uberclone.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsActivity"
        private const val LOCATION_PERMISSION_CODE = 999
        private const val PICKUP_REQUEST_CODE = 998
        private const val DROP_REQUEST_CODE = 997
    }

    private lateinit var mMap: GoogleMap
    private lateinit var mapsPresenter: MapsPresenter
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback

    private var currentLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null

    private var greyPolyLine: Polyline? = null
    private var blackPolyLine: Polyline? = null

    private val nearByCabMarkerList = arrayListOf<Marker>()
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapsPresenter = MapsPresenter(NetworkService())
        mapsPresenter.onAttach(this)
        setupClickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun setupClickListeners() {
        pickUpTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(PICKUP_REQUEST_CODE)
        }

        dropTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(DROP_REQUEST_CODE)
        }

        requestCabButton.setOnClickListener {
            statusTextView.visibility = View.VISIBLE
            statusTextView.text = getString(R.string.requesting_your_cab)
            requestCabButton.isEnabled = false
            pickUpTextView.isEnabled = false
            dropTextView.isEnabled = false
            mapsPresenter.requestACab(pickupLatLng!!, dropLatLng!!)
        }
    }

    private fun checkAndShowRequestButton() {
        if (pickupLatLng != null && dropLatLng != null) {
            requestCabButton.visibility = View.VISIBLE
            requestCabButton.isEnabled = true
        }
    }

    private fun launchLocationAutoCompleteActivity(requestCode: Int) {
        val fields = listOf<Place.Field>(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent =
            Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
        startActivityForResult(intent, requestCode)
    }

    private fun enableMyLocationOnMap() {
        mMap.setPadding(0, ViewUtils.dpToPx(48f), 0, 0)
        mMap.isMyLocationEnabled = true
    }

    private fun moveCamera(latLng: LatLng?) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng?) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun setUpLocationListener() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        // for getting location updates in every 2 seconds
        val locationRequest = LocationRequest().apply {
            interval = 2000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (currentLatLng == null) {
                    for (location in locationResult.locations) {
                        currentLatLng = LatLng(
                            location.latitude,
                            location.longitude
                        )
                        setCurrentLocationAsPickup()
                        Log.d(TAG, "lat ${location.latitude} long ${location.longitude}")
                        enableMyLocationOnMap()
                        moveCamera(currentLatLng)
                        animateCamera(currentLatLng)
                        mapsPresenter.requestNearByCabs(currentLatLng!!)
                        break
                    }
                }
            }
        }

        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private fun setCurrentLocationAsPickup() {
        pickupLatLng = currentLatLng
        pickUpTextView.text = getString(R.string.current_location)
    }

    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isPermissionFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        setUpLocationListener()
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                }
            }
            else -> {
                PermissionUtils.requestAccessFineLocationGranted(this, LOCATION_PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setUpLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_not_granted_info),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKUP_REQUEST_CODE || requestCode == DROP_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    when (requestCode) {
                        PICKUP_REQUEST_CODE -> {
                            pickUpTextView.text = place.name
                            pickupLatLng = place.latLng
                            checkAndShowRequestButton()
                        }
                        DROP_REQUEST_CODE -> {
                            dropTextView.text = place.name
                            dropLatLng = place.latLng
                            checkAndShowRequestButton()
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status: Status = Autocomplete.getStatusFromIntent(data!!)
                    Log.e(TAG, "error ${status.statusMessage}")
                }
                AutocompleteActivity.RESULT_CANCELED -> {
                    val status: Status = Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG, "User cancelled ${status.statusMessage}")
                }
            }

        }
    }

    override fun onDestroy() {
        mapsPresenter.onDetach()
        super.onDestroy()
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitMap(this))
        return mMap.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }

    private fun addOriginDestinationMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(MapUtils.getOriginDestinationBitMap())
        return mMap.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }

    override fun showNearByCabs(latLngList: List<LatLng>) {
        nearByCabMarkerList.clear()
        for (latlng in latLngList) {
            val nearByCarMarker = addCarMarkerAndGet(latlng)
            nearByCabMarkerList.add(nearByCarMarker)
        }
    }

    override fun informCabBooked() {
        Log.d(TAG, "onInformed")
        nearByCabMarkerList.forEach {
            it.remove()
        }
        nearByCabMarkerList.clear()
        requestCabButton.visibility = View.GONE
        statusTextView.text = getString(R.string.cab_is_booked)
    }

    override fun showPath(latLngList: List<LatLng>) {
        Log.d(TAG, "show Path ${latLngList.size}")
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

        val greyPolyLineOptions = PolylineOptions().apply {
            // to show the preview of the path
            color(Color.GRAY)
            width(5f)
            addAll(latLngList)
        }
        greyPolyLine = mMap.addPolyline(greyPolyLineOptions)

        val blackPolyLineOptions = PolylineOptions().apply {
            // to show the preview of the path
            color(Color.BLACK)
            width(5f)
        }
        blackPolyLine = mMap.addPolyline(blackPolyLineOptions)

        originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
        originMarker?.setAnchor(0.5f, 0.5f)

        destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        destinationMarker?.setAnchor(0.5f, 0.5f)

        val polylineAnimator = AnimationUtils.polylineAnimator()
        polylineAnimator.addUpdateListener {
            val percentage = it.animatedValue as Int
            val index = ((greyPolyLine?.points!!.size) * (percentage / 100.0f)).toInt()
            blackPolyLine?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polylineAnimator.start()

    }
}
