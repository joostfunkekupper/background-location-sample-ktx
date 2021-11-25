package ai.a2i2.locationtracking.viewmodels

import ai.a2i2.locationtracking.data.LocationRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Transformations
import java.util.concurrent.Executors

class LocationHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository.getInstance(
        application.applicationContext,
        Executors.newSingleThreadExecutor()
    )

    val locationsLiveData = Transformations.map(locationRepository.getLocations()) { locations ->
        val newLocations: ArrayList<String> = ArrayList()
        locations?.forEach { location ->
            newLocations.add(location.toString())
        }
        return@map newLocations
    }
}