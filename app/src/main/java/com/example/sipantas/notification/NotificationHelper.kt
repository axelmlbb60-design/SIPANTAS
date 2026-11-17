package com.example.sipantas.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sipantas.MainActivity
import com.example.sipantas.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "sipantas_notif_channel"
    private const val CHANNEL_NAME = "Notifikasi SIPANTAS"
    private const val CHANNEL_DESC = "Pemberitahuan pengajuan surat masuk dan pengingat antrean"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
        intent: Intent? = null
    ) {
        createNotificationChannel(context)

        val targetIntent = intent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Menjadwalkan pengingat jam antrean H-1 langsung di perangkat warga
     */
    fun scheduleQueueReminder(
        context: Context,
        queueId: Int,
        targetDateStr: String, // format yyyy-MM-dd
        sessionStr: String,
        ticketNo: String
    ) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = sdf.parse(targetDateStr) ?: return

            // Hitung waktu H-1 pukul 08:00 pagi
            val calendar = Calendar.getInstance().apply {
                time = targetDate
                add(Calendar.DAY_OF_YEAR, -1) // H-1
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // Jika waktu H-1 sudah terlewat, set untuk 10 detik dari sekarang agar langsung teruji
            var triggerMillis = calendar.timeInMillis
            if (triggerMillis <= System.currentTimeMillis()) {
                triggerMillis = System.currentTimeMillis() + 10_000 // 10 detik simulasi
            }

            val alarmIntent = Intent(context, QueueReminderReceiver::class.java).apply {
                putExtra("EXTRA_TICKET_NO", ticketNo)
                putExtra("EXTRA_DATE", targetDateStr)
                putExtra("EXTRA_SESSION", sessionStr)
                putExtra("EXTRA_QUEUE_ID", queueId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                queueId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
