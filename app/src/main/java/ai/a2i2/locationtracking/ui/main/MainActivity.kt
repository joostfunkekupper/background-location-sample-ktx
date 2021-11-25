package ai.a2i2.locationtracking.ui.main

import ai.a2i2.locationtracking.R
import ai.a2i2.locationtracking.databinding.ActivityMainBinding
import ai.a2i2.locationtracking.ui.history.LocationHistoryActivity
import ai.a2i2.locationtracking.utils.PermissionUtils
import ai.a2i2.locationtracking.utils.cancelAllNotifications
import ai.a2i2.locationtracking.viewmodels.LocationUpdateViewModel
import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationSettingsStates
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import me.ibrahimsn.library.LiveSharedPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * A SharedPreference wrapper providing a LiveData of a preference allowing us to observer any
     * changes, plus have them persisted. This is useful for when changes are made from within a
     * service or receiver.
     */
    private lateinit var liveSharedPreferences: LiveSharedPreferences

    /**
     * A ViewModel which is Application context aware.
     */
    private val locationUpdateViewModel by lazy {
        ViewModelProvider(this).get(LocationUpdateViewModel::class.java)
    }

    /**
     * Register a callback for an Activity Result. The result being whether the Location Services
     * has been enabled. If the user denied the change we ask the user again to enable the service.
     * https://developer.android.com/training/basics/intents/result#register
     */
    private val resolutionForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        val states = activityResult.data?.let { LocationSettingsStates.fromIntent(it) }
        when (activityResult.resultCode) {
            RESULT_OK ->
                if (states?.isLocationUsable == true) {
                    with(liveSharedPreferences.preferences.edit()) {
                        putBoolean("location_services_enabled", true)
                        apply()
                    }
                    locationUpdateViewModel.startLocationUpdates()
                    Log.i(TAG, "Location services have been enabled")
                }
            RESULT_CANCELED ->
                showLocationServicesWarning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val preferences = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)
        liveSharedPreferences = LiveSharedPreferences(preferences)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        checkLocationPermission()
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        val view = super.onCreateView(parent, name, context, attrs)

        locationUpdateViewModel.locationListLiveData.observe(
            this,
            { locations ->
                locations?.let {
                    if (locations.isEmpty()) {
                        binding.location.text = getString(R.string.location_unknown)
                    } else {
                        binding.location.text =
                            getString(R.string.location_known, locations.first().toString())
                    }
                }
            }
        )

        return view
    }

    /**
     * Inflate the menu, but also add an observer that will update the toggle updates menu item
     * with the appropriate text to start or stop location updates.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        locationUpdateViewModel.receivingLocationUpdates.observe(
            this,
            { receivingLocation ->
                val menuItem = menu?.findItem(R.id.toggle_updates)
                if (receivingLocation == true) {
                    menuItem?.setTitle(R.string.stop_location_updates)
                    // Receiving location updates, clear any notifications we have
                    applicationContext.cancelAllNotifications()
                } else {
                    menuItem?.setTitle(R.string.start_location_updates)
                }
            }
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.location_history -> {
                startActivity(Intent(this, LocationHistoryActivity::class.java))
                true
            }
            R.id.toggle_updates -> {
                toggleLocationUpdates()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleLocationUpdates() {
        val receivingUpdates = locationUpdateViewModel.receivingLocationUpdates.value
        if (receivingUpdates == true) {
            locationUpdateViewModel.stopLocationUpdates()
        } else {
            locationUpdateViewModel.startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()

        liveSharedPreferences.getBoolean("location_services_enabled", true)
            .observe(this) { available ->
                if (!available) {
                    showLocationServicesWarning()
                } else {
                    // Location services are enabled so we can dismiss this warning if it's showing.
                    // This might occur because the BroadcastReceiver has received an Intent indicating
                    // that the Location Services was disabled, but by the time we return to this
                    // screen the user has enabled it again. The observer receives multiple boolean
                    // events so we need to react accordingly.
                    dismissWarning()
                }
            }
    }

    /**
     * Verify it the user has provided us with all the necessary permissions to track their location,
     * even when the application is closed.
     */
    private fun checkLocationPermission() {
        when {
            !PermissionUtils.isForegroundLocationPermissionGranted(this) -> {
                PermissionUtils.requestPermission(
                    this,
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
                )
            }
            !PermissionUtils.isBackgroundLocationPermissionGranted(this) -> {
                if (PermissionUtils.shouldShowRationale(this, ACCESS_BACKGROUND_LOCATION)
                    || locationUpdateViewModel.hasRequestedBackgroundLocation
                ) {
                    showBackgroundLocationRationale()
                } else {
                    // This will never get reached as a rationale should always be shown for this permission
                    requestBackgroundLocationPermission()
                }
            }
            else -> {
                locationUpdateViewModel.startLocationUpdates()
                locationPermissionGranted()
            }
        }
    }

    private fun requestBackgroundLocationPermission() {
        // Open the location permissions for the app where the user can select "Allow all the time".
        // If we have asked previously but the user has denied it, that request will fail. So instead
        // open the app settings where the user can navigate manually to the location permissions
        // and modify it.
        if (!locationUpdateViewModel.hasRequestedBackgroundLocation) {
            PermissionUtils.requestPermission(this, arrayOf(ACCESS_BACKGROUND_LOCATION))
            locationUpdateViewModel.hasRequestedBackgroundLocation = true
        } else {
            PermissionUtils.openAppSettings(this)
        }
    }

    /**
     * Result from requesting runtime permissions can be checked here.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PermissionUtils.PERMISSION_REQUEST_CODE) {
            // We received some permission result from someone/something else.
            return
        }

        if (grantResults.isEmpty()) {
            // The permission request was cancelled
            return
        }

        if (permissions.any {
                it.contentEquals(ACCESS_FINE_LOCATION)
                        || it.contentEquals(ACCESS_COARSE_LOCATION)
            } && grantResults.all { it == PackageManager.PERMISSION_DENIED }) {
            locationPermissionsDenied()
        } else if (permissions.any { it.contentEquals(ACCESS_BACKGROUND_LOCATION) }
            && grantResults.all { it == PackageManager.PERMISSION_DENIED }) {
            showBackgroundLocationRationale()
        } else {
            // Check if any other permissions need to be requested
            checkLocationPermission()
        }
    }

    private var dialog: AlertDialog? = null

    /**
     * Ask the user to allow location updates "all the time" so that we can receive location updates
     * while the app is closed.
     */
    private fun showBackgroundLocationRationale() {
        if (dialog == null) {
            dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.location_rationale_title)
                .setMessage(R.string.location_rationale)
                .setPositiveButton(getString(R.string.got_it)) { _, _ -> requestBackgroundLocationPermission() }
                .setNegativeButton(getString(R.string.no_thanks)) { _, _ ->
                    locationPermissionsDenied()
                }
                .create()
        }

        if (dialog?.isShowing == false) {
            dialog?.show()
        }
    }

    private var locationServicesWarning: Snackbar? = null

    /**
     * Ask the user to turn on Location Services so that we can receive location updates.
     */
    private fun showLocationServicesWarning() {
        if (locationServicesWarning == null) {
            locationServicesWarning = Snackbar.make(
                binding.root,
                R.string.enable_location_services_text,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    locationUpdateViewModel.requestLocationServices(this, resolutionForResult)
                }
        }

        if (locationServicesWarning?.isShown == false) {
            locationServicesWarning?.show()
        }
    }

    /**
     * Allows us to explicitly dismiss this warning.
     */
    private fun dismissWarning() {
        locationServicesWarning?.dismiss()
    }

    /**
     * Ask the user to allow Location permissions so that we can receive location updates.
     */
    private fun showLocationPermissionsWarning() {
        Snackbar.make(
            binding.root,
            R.string.error_permissions_denied,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                PermissionUtils.openAppSettings(this)
            }
            .show()
    }

    private fun locationPermissionsDenied() {
        binding.mapImage.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_access_denied
            )
        )
        showLocationPermissionsWarning()
    }

    private fun locationPermissionGranted() {
        binding.mapImage.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_map_dark
            )
        )
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}