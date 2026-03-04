package de.ingomc.nozio.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.ingomc.nozio.MainActivity
import de.ingomc.nozio.R
import de.ingomc.nozio.WidgetLaunchAction

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        showNotification(context)
    }

    companion object {
        const val CHANNEL_ID = "meal_tracking_reminder"
        private const val NOTIFICATION_ID = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Essens-Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Erinnerungen zum Tracken deiner Mahlzeiten"
            }
            manager?.createNotificationChannel(channel)
        }

        fun showNotification(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                MainActivity.createIntent(context, WidgetLaunchAction.NONE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Mahlzeit tracken")
                .setContentText("Zeit, dein Essen in Nozio zu erfassen.")
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
