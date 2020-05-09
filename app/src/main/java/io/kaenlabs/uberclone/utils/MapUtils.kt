package io.kaenlabs.uberclone.utils

import android.content.Context
import android.graphics.*
import com.google.android.gms.maps.model.LatLng
import io.kaenlabs.uberclone.R
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.round

object MapUtils {

    fun getCarBitMap(context: Context): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return Bitmap.createScaledBitmap(bitmap, 50, 100, false)
    }

    fun getOriginDestinationBitMap(): Bitmap {
        val height = 20
        val width = 20
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    fun getRotation(startLatLng: LatLng, endLatLng: LatLng): Float {
        val latDiff = abs(endLatLng.latitude - startLatLng.latitude)
        val lngDiff = abs(endLatLng.longitude - startLatLng.longitude)
        var rotation = -1f
        when {
            // First Quadrant
            startLatLng.latitude < endLatLng.latitude && startLatLng.longitude < endLatLng.longitude -> {
                rotation = Math.toDegrees(atan(lngDiff / latDiff)).toFloat()
            }
            // Second Quadrant
            startLatLng.latitude >= endLatLng.latitude && startLatLng.longitude < endLatLng.longitude -> {
                rotation = ((90 - Math.toDegrees(atan(lngDiff / latDiff))) + 90).toFloat()
            }
            // Third Quadrant
            startLatLng.latitude >= endLatLng.latitude && startLatLng.longitude >= endLatLng.longitude -> {
                rotation = (180 + Math.toDegrees(atan(lngDiff / latDiff))).toFloat()
            }
            // Fourth Quadrant
            startLatLng.latitude >= endLatLng.latitude && startLatLng.longitude < endLatLng.longitude -> {
                rotation = ((90 - Math.toDegrees(atan(lngDiff / latDiff))) + 270).toFloat()
            }
        }
        return rotation
    }

}