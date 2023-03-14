package com.newapp.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.SparseArray
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener


class PermissionsModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), PermissionListener {
    private val mCallbacks: SparseArray<Callback> = SparseArray()
    private var mRequestCode = 0

    private val permissionLocation: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                android.Manifest.permission.FOREGROUND_SERVICE
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }


    override fun getName(): String {
        return MODULE_NAME
    }

    @ReactMethod
    fun checkNotifications(promise: Promise) {
        val enabled = NotificationManagerCompat
            .from(reactApplicationContext).areNotificationsEnabled()
        val output = Arguments.createMap()
        val settings = Arguments.createMap()
        output.putString("status", if (enabled) GRANTED else BLOCKED)
        output.putMap("settings", settings)
        promise.resolve(output)
    }

    @ReactMethod
    fun openSettings(promise: Promise) {
        try {
            val intent = Intent()
            val packageName = reactContext.packageName
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.fromParts("package", packageName, null)
            reactContext.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject(ERROR_INVALID_ACTIVITY, e)
        }
    }

    @ReactMethod
    fun checkPermissions(promise: Promise) {
        var checkedPermissionsDenied = 0

        val output: WritableMap = WritableNativeMap()
        for (permission in permissionLocation) {
            Log.d("Permissions", "${reactContext.checkSelfPermission(permission)}")

            if (reactContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                checkedPermissionsDenied++
            }
        }

        if (checkedPermissionsDenied >= 1) {
            output.putBoolean(GRANTED, false)
            output.putBoolean(CAN_ASK_AGAIN, true)

        } else {
            output.putBoolean(GRANTED, true)
            output.putBoolean(CAN_ASK_AGAIN, true)

        }
        promise.resolve(output)
    }

    @ReactMethod
    fun requestPermissions(promise: Promise) {
        val output: WritableMap = WritableNativeMap()
        val permissionsToCheck = ArrayList<String>()
        var checkedPermissionsGranted = 0
        var checkedPermissionsDenied = 0
        var checkedPermissionsBlocked = 0

        for (permission in permissionLocation) {
            if (reactContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                checkedPermissionsGranted++
            } else {
                permissionsToCheck.add(permission)
            }
        }
        if (permissionLocation.size == checkedPermissionsGranted) {
            output.putBoolean(GRANTED, true)
            output.putBoolean(CAN_ASK_AGAIN, true)
            promise.resolve(output)
            return
        }
        try {
            val activity = permissionAwareActivity
            mCallbacks.put(
                mRequestCode,
                Callback { args ->
                    val results = args[0] as IntArray
                    val awareActivity = args[1] as PermissionAwareActivity
                    for (j in permissionsToCheck.indices) {
                        val permission = permissionsToCheck[j]
                        if (results.isNotEmpty() && results[j] == PackageManager.PERMISSION_GRANTED) {
                            //output.putString(permission, GRANTED)
                            checkedPermissionsGranted++
                        } else {
                            if (awareActivity.shouldShowRequestPermissionRationale(permission)) {
                                // output.putString(permission, DENIED)
                                checkedPermissionsDenied++
                            } else {
                                // output.putString(permission, BLOCKED)
                                checkedPermissionsBlocked++
                            }
                        }
                    }
                    if (checkedPermissionsBlocked >= 1) {
                        output.putBoolean(GRANTED, false)
                        output.putBoolean(CAN_ASK_AGAIN, false)

                    } else if (checkedPermissionsDenied >= 1) {
                        output.putBoolean(GRANTED, false)
                        output.putBoolean(CAN_ASK_AGAIN, true)

                    } else {
                        output.putBoolean(GRANTED, true)
                        output.putBoolean(CAN_ASK_AGAIN, true)

                    }

                    promise.resolve(output)
                })
            activity.requestPermissions(permissionsToCheck.toTypedArray(), mRequestCode, this)
            mRequestCode++
        } catch (e: IllegalStateException) {
            promise.reject(ERROR_INVALID_ACTIVITY, e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        mCallbacks[requestCode].invoke(grantResults, permissionAwareActivity)
        mCallbacks.remove(requestCode)
        return mCallbacks.size() == 0
    }

    private val permissionAwareActivity: PermissionAwareActivity
        get() {
            val activity = currentActivity
            if (activity == null) {
                throw IllegalStateException(
                    "Tried to use permissions API while not attached to an " + "Activity."
                )
            } else if (activity !is PermissionAwareActivity) {
                throw IllegalStateException(
                    "Tried to use permissions API but the host Activity doesn't"
                            + " implement PermissionAwareActivity."
                )
            }
            return activity
        }

    companion object {
        const val ERROR_INVALID_ACTIVITY = "E_INVALID_ACTIVITY"
        const val MODULE_NAME = "PermissionsModule"
        const val GRANTED = "granted"
        const val DENIED = "denied"
        const val UNAVAILABLE = "unavailable"
        const val BLOCKED = "blocked"
        const val CAN_ASK_AGAIN = "canAskAgain"
    }
}