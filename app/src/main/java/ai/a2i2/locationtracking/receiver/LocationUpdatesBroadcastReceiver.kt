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
 * Changes:
 * 1. Check for Location Services through the LocationManager.
 * 2. Store result in Shared Preferences.
 */
package ai.a2i2.locationtracking.receiver

import ai.a2i2.locationtracking.R
import ai.a2i2.locationtracking.data.LocationRepository
import ai.a2i2.locationtracking.data.db.LocationEntity
import ai.a2i2.locationtracking.utils.PermissionUtils
import ai.a2i2.locationtracking.utils.cancelAllNotifications
import ai.a2i2.locationtracking.utils.isLocationEnabled
import ai.a2i2.locationtracking.utils.showLocationUnavailableNotification
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import me.ibrahimsn.library.LiveSharedPreferences
import java.util.*
import java.util.concurrent.Executors

class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")

        val preferences =
            context.getSharedPreferences(context.getString(R.string.pref_key), Context.MODE_PRIVATE)
        val liveSharedPreferences = LiveSharedPreferences(preferences)

        if (intent.action == ACTION_PROCESS_UPDATES) {

            // LocationAvailability.isLocationAvailable returns true if the device location is
            // known and reasonably up to date within the hints requested by the active LocationRequests.
            // Failure to determine location may result from a number of causes including disabled
            // location settings or an inability to retrieve sensor data in the device's environment.
            LocationAvailability.extractLocationAvailability(intent)?.let {
                if (!PermissionUtils.isLocationPermissionGranted(context)) {
                    Log.w(TAG, "Background location permissions have been revoked!")
                    // Note: Clearing existing notifications to prevent duplication. In production
                    // you should never just dismiss all notifications.
                    context.cancelAllNotifications()
                    context.showLocationUnavailableNotification(
                        context.getString(R.string.location_background_permission_revoked),
                        context.getString(R.string.location_rationale)
                    )
                }

                if (!it.isLocationAvailable) {
                    Log.w(TAG, "Location is currently unavailable!")
                }

                if (!context.isLocationEnabled()) {
                    Log.w(TAG, "Location Services were disabled by the user!")
                    with(liveSharedPreferences.preferences.edit()) {
                        putBoolean("location_services_enabled", false)
                        apply()
                    }
                    // Note: Clearing existing notifications to prevent duplication. In production
                    // you should never just dismiss all notifications.
                    context.cancelAllNotifications()
                    context.showLocationUnavailableNotification(
                        context.getString(R.string.location_service_disabled),
                        context.getString(R.string.enable_location_services_text)
                    )
                }
            }

            LocationResult.extractResult(intent)?.let { locationResult ->
                val locations = locationResult.locations.map {
                    LocationEntity(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        foreground = isAppInForeground(context),
                        recordedAt = Date(it.time)
                    )
                }
                if (locations.isNotEmpty()) {
                    LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        .addLocations(locations)
                }
            }
        }
    }

    // Note: This function's implementation is only for debugging purposes. If you are going to do
    // this in a production app, you should instead track the state of all your activities in a
    // process via android.app.Application.ActivityLifecycleCallbacks's
    // unregisterActivityLifecycleCallbacks(). For more information, check out the link:
    // https://developer.android.com/reference/android/app/Application.html#unregisterActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        appProcesses.forEach { appProcess ->
            if (appProcess.importance ==
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == context.packageName
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private val TAG = LocationUpdatesBroadcastReceiver::class.simpleName

        const val ACTION_PROCESS_UPDATES =
            "ai.a2i2.locationtracking.action.PROCESS_UPDATES"
    }
}