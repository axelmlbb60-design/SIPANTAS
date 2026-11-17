package com.example.sipantas.notification

import android.content.Intent
import android.util.Log
import com.example.sipantas.LetterDetailActivity
import com.example.sipantas.QueueActivity
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SipantasFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SIPANTAS_FCM", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Notifikasi SIPANTAS"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Pemberitahuan dari kelurahan"
        val type = remoteMessage.data["type"] ?: "info"
        val dataIdStr = remoteMessage.data["data_id"]
        val dataId = dataIdStr?.toIntOrNull() ?: 0

        val targetIntent = when (type) {
            "pengajuan_baru" -> {
                Intent(this, LetterDetailActivity::class.java).apply {
                    putExtra("EXTRA_PENGAJUAN_ID", dataId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            "antrean_h_min_1" -> {
                Intent(this, QueueActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            else -> null
        }

        NotificationHelper.showSystemNotification(
            context = applicationContext,
            title = title,
            message = body,
            intent = targetIntent
        )
    }

    private fun sendRegistrationToServer(token: String) {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            val apiService = ApiClient.getService(applicationContext)
            apiService.updateFcmToken(userId, token).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                    Log.d("SIPANTAS_FCM", "Token updated on MySQL successfully")
                }
                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    Log.e("SIPANTAS_FCM", "Failed to update token on MySQL", t)
                }
            })
        }
    }
}
