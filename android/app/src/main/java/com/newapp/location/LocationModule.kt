package com.newapp.location

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.SparseArray
import com.facebook.react.bridge.*
import com.newapp.service.LocationService
import com.newapp.util.GetLocation


class LocationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val mCallbacks: SparseArray<Callback> = SparseArray()
    private var mRequestCode = 0
    private val appContext = reactApplicationContext.baseContext
    private lateinit var locationManager: LocationManager
    private var getLocation: GetLocation? = null

    init {
        try {
            locationManager =
                reactContext.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            appContext.startService(Intent(appContext,LocationService::class.java))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun stopLocationTracker(promise: Promise) {
        getLocation?.stop()
        promise.resolve(true)
    }

    @ReactMethod
    private fun startLocationTracker(options: ReadableMap?, promise: Promise) {
        if (getLocation != null) {
            getLocation?.cancel()
        }
        getLocation = GetLocation(locationManager)
        getLocation?.startLocationTracker(options, promise)
    }
    @ReactMethod
    private fun getLocation(promise: Promise) {
        if (getLocation != null) {
            getLocation?.getLocation(promise)
        } else {
            getLocation = GetLocation(locationManager)
            getLocation?.getLocation(promise)
        }
    }

    companion object {
        const val NAME = "LocationModule"
    }

}