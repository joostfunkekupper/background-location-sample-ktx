package ai.a2i2.locationtracking

import android.app.AppOpsManager
import android.app.Application
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.os.Build
import android.util.Log

private const val TAG = "Location Tracking"

/**
 * This class will perform data access auditing by logging any access to private data from users.
 * In this example that will be location updates. But we included any third-party libraries, we
 * would be informed of any access if we provide the library with an Attribution Context, i.e.
 * applicationContext.createAttributionContext("Your tag")
 * See https://developer.android.com/guide/topics/data/audit-access#log-access
 */
open class AuditApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appOpsCallback: AppOpsManager.OnOpNotedCallback =
                object : AppOpsManager.OnOpNotedCallback() {
                    private fun logPrivateDataAccess(
                        opCode: String,
                        attributionTag: String?,
                        trace: String
                    ) {
                        // Note: If the return value for the attribution tag is null, this means
                        // that the current Context object is associated with the main part of
                        // the app.
                        Log.i(
                            TAG, "Private data accessed.\n" +
                                    "Operation: $opCode\n" +
                                    "Attribution Tag: $attributionTag" +
                                    "\nStack Trace:\n$trace"
                        )
                    }

                    override fun onNoted(syncNotedAppOp: SyncNotedAppOp) {
                        logPrivateDataAccess(
                            syncNotedAppOp.op,
                            syncNotedAppOp.attributionTag,
                            Throwable().stackTrace.toString()
                        )
                    }

                    override fun onSelfNoted(syncNotedAppOp: SyncNotedAppOp) {
                        logPrivateDataAccess(
                            syncNotedAppOp.op,
                            syncNotedAppOp.attributionTag,
                            Throwable().stackTrace.toString()
                        )
                    }

                    override fun onAsyncNoted(asyncNotedAppOp: AsyncNotedAppOp) {
                        logPrivateDataAccess(
                            asyncNotedAppOp.op,
                            asyncNotedAppOp.attributionTag,
                            asyncNotedAppOp.message
                        )
                    }
                }

            val appOpsManager = getSystemService(AppOpsManager::class.java) as AppOpsManager
            appOpsManager.setOnOpNotedCallback(mainExecutor, appOpsCallback)
        }
    }
}