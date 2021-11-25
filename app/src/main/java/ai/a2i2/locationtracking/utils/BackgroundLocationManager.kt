/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 1. Check if Location Services are available before requesting Location Updates.
 * 2. Store result in Shared Preferences.
 * 3. Include PendingIntent.FLAG_MUTABLE for PendingIntent as per API 31 requirements.
 * 4. Method to request user to enable Location Services through system dialog.
 */
package ai.a2i2.locationtracking.utils

import ai.a2i2.locationtracking.R
import ai.a2i2.locationtracking.receiver.LocationUpdatesBroadcastReceiver
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import me.ibrahimsn.library.LiveSharedPreferences
import java.util.concurrent.TimeUnit

class BackgroundLocationManager(private val context: Context) {

    private val _receivingLocationUpdates: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>(false)

    private var preferences: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.pref_key), Context.MODE_PRIVATE)
    private val liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(preferences)

    /**
     * Status of location updates, i.e., whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean>
        get() = _receivingLocationUpdates

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // All location requests are considered hints, and you may receive locations that are more/less
    // accurate, and faster/slower than requested.
    // https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        //
        // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
        // receive updates less frequently than this interval when the app is no longer in the
        // foreground.
        interval = TimeUnit.SECONDS.toMillis(60)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.SECONDS.toMillis(10)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.MINUTES.toMillis(2)

        // The priority of the request is a strong hint to the LocationClient for which location
        // sources to use. For example, PRIORITY_HIGH_ACCURACY is more likely to use GPS, and
        // PRIORITY_BALANCED_POWER_ACCURACY is more likely to use WIFI & Cell tower positioning,
        // but it also depends on many other factors (such as which sources are available) and is
        // implementation dependent.
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    @SuppressLint("MissingPermission")
    @Throws(SecurityException::class)
    @MainThread
    fun startLocationUpdates() {
        if (!PermissionUtils.isLocationPermissionGranted(context)) return

        // Don't rely on the FusedLocationProviderClient.locationAvailability as this takes multiple
        // variables into account to determine if location is available, rather than the actual
        // Location Service (via device settings) from the LocationManager has been disabled.
        // https://issuetracker.google.com/issues/198176818#comment70
        if (!context.isLocationEnabled()) {
            stopLocationUpdates()
            setLocationServicesEnabledPref(false)
            Log.w(TAG, "Location Services are disabled!")
        } else {
            try {
                _receivingLocationUpdates.value = true
                setLocationServicesEnabledPref(true)

                // If the PendingIntent is the same as the last request (which it always is), this
                // request will replace any requestLocationUpdates() called before.
                // If FusedLocationProviderClient.locationAvailability is false we may not immediately
                // get a location update, nothing we can do to change that.
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationUpdatePendingIntent
                )
            } catch (permissionRevoked: SecurityException) {
                _receivingLocationUpdates.value = false

                // Exception only occurs if the user revokes the FINE location permission before
                // requestLocationUpdates() is finished executing (very rare).
                Log.w(TAG, "Location permissions revoked; details: $permissionRevoked")
                throw permissionRevoked
            }
        }
    }

    @MainThread
    fun stopLocationUpdates() {
        Log.d(TAG, "Stop location updates")
        _receivingLocationUpdates.value = false
        fusedLocationClient.removeLocationUpdates(locationUpdatePendingIntent)
    }

    @MainThread
    fun requestLocationServices(activity: Activity, resolutionForResult: ActivityResultLauncher<IntentSenderRequest>) {
        val settingsClient = LocationServices.getSettingsClient(activity)
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                if (it.locationSettingsStates?.isLocationUsable == true) {
                    // Immediately start the location updates if the location services is in fact available
                    startLocationUpdates()
                }
            }
            .addOnFailureListener {
                val statusCode = (it as ApiException).statusCode
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // Request the user to enable location services through a system dialog provided
                    // provided through the ResolvableApiException
                    val resolvable = it as ResolvableApiException
                    val intentSenderRequest = IntentSenderRequest.Builder(resolvable.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)
                }
            }
    }

    private fun setLocationServicesEnabledPref(value: Boolean) {
        with(liveSharedPreferences.preferences.edit()) {
            putBoolean("location_services_enabled", value)
            apply()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BackgroundLocationManager? = null

        fun getInstance(context: Context): BackgroundLocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackgroundLocationManager(context).also { INSTANCE = it }
            }
        }

        private val TAG = BackgroundLocationManager::class.simpleName
    }
}