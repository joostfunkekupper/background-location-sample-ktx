package ai.a2i2.locationtracking.data

import ai.a2i2.locationtracking.data.db.LocationDatabase
import ai.a2i2.locationtracking.data.db.LocationEntity
import ai.a2i2.locationtracking.utils.BackgroundLocationManager
import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import java.util.*
import java.util.concurrent.ExecutorService

class LocationRepository private constructor(
    private val locationDatabase: LocationDatabase,
    private val locationManager: BackgroundLocationManager,
    private val executor: ExecutorService
) {

    // Database related fields/methods:
    private val locationDao = locationDatabase.locationDao()

    /**
     * Returns all recorded locations from database.
     */
    fun getLocations(): LiveData<List<LocationEntity>> = locationDao.getLocations()

    // Not being used now but could in future versions.
    /**
     * Returns specific location in database.
     */
    fun getLocation(id: UUID): LiveData<LocationEntity> = locationDao.getLocation(id)

    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
    fun updateLocation(locationEntity: LocationEntity) {
        executor.execute {
            locationDao.updateLocation(locationEntity)
        }
    }

    /**
     * Adds location to the database.
     */
    fun addLocation(locationEntity: LocationEntity) {
        executor.execute {
            locationDao.addLocation(locationEntity)
        }
    }

    /**
     * Adds list of locations to the database.
     */
    fun addLocations(myLocationEntities: List<LocationEntity>) {
        executor.execute {
            locationDao.addLocations(myLocationEntities)
        }
    }

    // Location related fields/methods:
    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean> = locationManager.receivingLocationUpdates

    /**
     * Subscribes to location updates.
     */
    @MainThread
    fun startLocationUpdates() = locationManager.startLocationUpdates()

    /**
     * Un-subscribes from location updates.
     */
    @MainThread
    fun stopLocationUpdates() = locationManager.stopLocationUpdates()

    @MainThread
    fun requestLocationServices(activity: Activity, resolutionForResult: ActivityResultLauncher<IntentSenderRequest>) = locationManager.requestLocationServices(activity, resolutionForResult)

    companion object {
        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(
                    LocationDatabase.getInstance(context),
                    BackgroundLocationManager.getInstance(context),
                    executor)
                    .also { INSTANCE = it }
            }
        }
    }
}