package ai.a2i2.locationtracking

import ai.a2i2.locationtracking.utils.createNotificationChannel

class BaseApplication : AuditApplication() {

    override fun onCreate() {
        super.onCreate()

        // Register a notification channel
        applicationContext.createNotificationChannel()
    }
}