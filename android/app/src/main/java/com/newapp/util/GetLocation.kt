package com.newapp.util

import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap


class GetLocation(private val locationManager: LocationManager) {

    private var listenerStartTrack: LocationListener? = null
    private var listenerGetLocation: LocationListener? = null
    private var startTrackPromise: Promise? = null

    fun getLocation(promise: Promise) {
        try {
            if (!isLocationEnabled) {
                promise.reject("UNAVAILABLE", "Location not available")
                return
            }
            val enableHighAccuracy = true
            val criteria = Criteria()
            criteria.accuracy =
                if (enableHighAccuracy) Criteria.ACCURACY_FINE else Criteria.ACCURACY_COARSE
            listenerGetLocation = LocationListener { location ->
                val resultLocation = WritableNativeMap()
                resultLocation.putString("provider", location.provider)
                resultLocation.putDouble("latitude", location.latitude)
                resultLocation.putDouble("longitude", location.longitude)
                resultLocation.putDouble("accuracy", location.accuracy.toDouble())
                resultLocation.putDouble("altitude", location.altitude)
                resultLocation.putDouble("speed", location.speed.toDouble())
                resultLocation.putDouble("bearing", location.bearing.toDouble())
                resultLocation.putDouble("time", location.time.toDouble())

                promise.resolve(resultLocation)

                listenerGetLocation?.let { locationManager.removeUpdates(it) }

            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                listenerGetLocation!!
            )

            /*locationManager.requestLocationUpdates(
                0L,
                0f,
                criteria,
                listenerGetLocation!!,
                Looper.myLooper()
            )*/


        } catch (ex: SecurityException) {
            ex.printStackTrace()
            stop()
            promise.reject("UNAUTHORIZED", "Location permission denied", ex)
        } catch (ex: Exception) {
            ex.printStackTrace()
            stop()
            promise.reject("UNAVAILABLE", "Location not available", ex)
        }
    }

    fun startLocationTracker(options: ReadableMap?, promise: Promise) {
        val hasKeyMinTimeMs = options?.hasKey("minTimeMs") ?: false
        val hasKeyMinDistanceMs = options?.hasKey("minDistanceM") ?: false

        val minTimeMsLong = options?.getDouble("minTimeMs")?.toLong() ?: (1000 * 60 * 1).toLong()
        val minDistanceMFloat = options?.getDouble("minDistanceM")?.toFloat() ?: (5).toFloat()

        val minTimeMs = if (hasKeyMinTimeMs) {
            minTimeMsLong
        } else {
            (1000 * 60 * 1).toLong()
        }

        val minDistanceM = if (hasKeyMinDistanceMs) {
            minDistanceMFloat
        } else {
            (5).toFloat()
        }

        Log.d("minTimeMs", "$minTimeMs")
        Log.d("minDistanceM", "$minDistanceM")

        this.startTrackPromise = promise
        try {
            if (!isLocationEnabled) {
                startTrackPromise?.reject("UNAVAILABLE", "Location not available")
                return
            }
            val enableHighAccuracy = true
            val criteria = Criteria()
            criteria.accuracy =
                if (enableHighAccuracy) Criteria.ACCURACY_FINE else Criteria.ACCURACY_COARSE
            listenerStartTrack = LocationListener { location ->
                val resultLocation = WritableNativeMap()
                resultLocation.putString("provider", location.provider)
                resultLocation.putDouble("latitude", location.latitude)
                resultLocation.putDouble("longitude", location.longitude)
                resultLocation.putDouble("accuracy", location.accuracy.toDouble())
                resultLocation.putDouble("altitude", location.altitude)
                resultLocation.putDouble("speed", location.speed.toDouble())
                resultLocation.putDouble("bearing", location.bearing.toDouble())
                resultLocation.putDouble("time", location.time.toDouble())

                Log.d(
                    "startLocationTracker",
                    "Lat : ${location.latitude} Lon : ${location.longitude}"
                )

                startTrackPromise?.resolve(resultLocation)

            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceM,
                listenerStartTrack!!
            )

            /* locationManager.requestLocationUpdates(
                 minTimeMs,
                 minDistanceM,
                 criteria,
                 listenerStartTrack!!,
                 Looper.myLooper()
             )*/

        } catch (ex: SecurityException) {
            ex.printStackTrace()
            stop()
            promise.reject("UNAUTHORIZED", "Location permission denied", ex)
        } catch (ex: Exception) {
            ex.printStackTrace()
            stop()
            promise.reject("UNAVAILABLE", "Location not available", ex)
        }
    }

    @Synchronized
    fun cancel() {
        if (startTrackPromise == null) {
            return
        }
        try {
            startTrackPromise?.reject("CANCELLED", "Location cancelled by another request")
            stop()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun stop() {
        if (listenerStartTrack != null) {
            locationManager.removeUpdates(listenerStartTrack!!)
        }

        startTrackPromise = null
        listenerStartTrack = null
    }

    private val isLocationEnabled: Boolean
        get() {
            try {
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return false
        }
}