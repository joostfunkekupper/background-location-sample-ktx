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
 * 1. Capture background location request response.
 * 2. Add requestLocationServices() method.
 */
package ai.a2i2.locationtracking.viewmodels

import ai.a2i2.locationtracking.data.LocationRepository
import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import java.util.concurrent.Executors

class LocationUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // https://developer.android.com/guide/topics/data/audit-access#create-attribution-tags
        LocationRepository.getInstance(
            application.createAttributionContext("Background location manager"),
            Executors.newSingleThreadExecutor()
        )
    } else {
        LocationRepository.getInstance(
            application.applicationContext,
            Executors.newSingleThreadExecutor()
        )
    }

    val receivingLocationUpdates: LiveData<Boolean> = locationRepository.receivingLocationUpdates

    val locationListLiveData = locationRepository.getLocations()

    // Used to determine if we can request the background location permission directly, or if we should
    // open the app settings screen if we've already attempted that previously.
    var hasRequestedBackgroundLocation = false

    fun startLocationUpdates() = locationRepository.startLocationUpdates()

    fun stopLocationUpdates() = locationRepository.stopLocationUpdates()

    fun requestLocationServices(activity: Activity, resolutionForResult: ActivityResultLauncher<IntentSenderRequest>) = locationRepository.requestLocationServices(activity, resolutionForResult)
}