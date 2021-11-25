package ai.a2i2.locationtracking.utils

import ai.a2i2.locationtracking.R
import ai.a2i2.locationtracking.ui.main.MainActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

/**
 * Show a notification which will open the app when tapped.
 */
fun Context.showLocationUnavailableNotification(title: String, text: String): Int {
    // Open MainActivity when the notification is tapped
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val id = this.getString(R.string.channel_id)
    val notification = NotificationCompat.Builder(this, id)
        .setSmallIcon(R.drawable.ic_not_listed_location)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true) // automatically remove notification when tapped
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOnlyAlertOnce(true)
        .build()

    // Note: This ID should be unique for each notification and tracked so that it can be updated or
    // dismissed later. Storing it SharedPreferences would allow you to do so throughout your app.
    val notificationId = Random.nextInt()
    with(NotificationManagerCompat.from(this)) {
        notify(notificationId, notification)
    }
    return notificationId
}

/**
 * Create a notification channel for notifications that highlight any location based
 * permission or setting changes to the user. This should be called when the app
 * is first created.
 */
fun Context.createNotificationChannel() {
    val id = this.getString(R.string.channel_id)
    val name = this.getString(R.string.channel_name)
    val description = this.getString(R.string.channel_description)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannelCompat.Builder(id, importance)
        .setName(name)
        .setDescription(description)
        .build()
    NotificationManagerCompat.from(this)
        .createNotificationChannel(channel)
}

/**
 * Remove all app notifications.
 */
fun Context.cancelAllNotifications() {
    NotificationManagerCompat.from(this).cancelAll()
}