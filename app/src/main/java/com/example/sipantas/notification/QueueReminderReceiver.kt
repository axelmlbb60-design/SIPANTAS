package com.example.sipantas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sipantas.QueueActivity

class QueueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ticketNo = intent.getStringExtra("EXTRA_TICKET_NO") ?: "A-001"
        val session = intent.getStringExtra("EXTRA_SESSION") ?: "Pagi"
        val date = intent.getStringExtra("EXTRA_DATE") ?: "Besok"
        val queueId = intent.getIntExtra("EXTRA_QUEUE_ID", 1001)

        val title = "🔔 Pengingat Antrean Layanan Besok (H-1)"
        val message = "Besok ($date) adalah jadwal kunjungan Anda di Loket Kelurahan pada sesi $session. Nomor Tiket: $ticketNo. Mohon hadir 10 menit lebih awal dan bawa dokumen asli!"

        val targetIntent = Intent(context, QueueActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        NotificationHelper.showSystemNotification(
            context = context,
            title = title,
            message = message,
            notificationId = queueId,
            intent = targetIntent
        )
    }
}
