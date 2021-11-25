package ai.a2i2.locationtracking.utils

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

object PermissionUtils {
    const val PERMISSION_REQUEST_CODE = 34;

    private fun checkPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Open the apps settings so that the user can navigate to the permissions section. We cannot
     * directly open the location permissions screen, so this is the next best thing.
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    /**
     * Are we required to show a rationale to the user as to why we are asking for this runtime
     * permission? If true, then we should show a message to the user explaining why we require
     * this permission. These type of permissions should really be opt-in only, and not required.
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Verify if the android.permission.ACCESS_BACKGROUND_LOCATION for API 29+ and android.permission.ACCESS_FINE_LOCATION
     * permissions are set in the manifest and/or are granted by the user.
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return context.hasFineLocationPermission && context.hasBackgroundLocationPermission
    }

    fun isForegroundLocationPermissionGranted(context: Context): Boolean {
        return context.hasFineLocationPermission || context.hasCoarseLocationPermission
    }

    fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
        return context.hasBackgroundLocationPermission
    }

    private val Context.hasFineLocationPermission: Boolean
        get() = checkPermission(this, permission.ACCESS_FINE_LOCATION)

    private val Context.hasCoarseLocationPermission: Boolean
        get() = checkPermission(this, permission.ACCESS_COARSE_LOCATION)

    private val Context.hasBackgroundLocationPermission: Boolean
        @RequiresApi(Build.VERSION_CODES.Q)
        get() = checkPermission(this, permission.ACCESS_BACKGROUND_LOCATION)
}