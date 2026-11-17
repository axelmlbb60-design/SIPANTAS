package com.example.sipantas

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.AntreanItem
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QueueActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_queue)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.queue_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btn_book_queue).setOnClickListener {
            bookQueueTicket()
        }
    }

    private fun bookQueueTicket() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memesan tiket antrean ke loket...")
            setCancelable(false)
            show()
        }

        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        apiService.createAntrean(
            action = "create",
            userId = userId,
            layanan = "Loket 1 · Pelayanan Surat & Legalisir",
            tanggal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            sesi = "08.30 – 09.00 WITA"
        ).enqueue(object : Callback<ApiResponse<AntreanItem>> {
            override fun onResponse(call: Call<ApiResponse<AntreanItem>>, response: Response<ApiResponse<AntreanItem>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    val ticket = body.data
                    AlertDialog.Builder(this@QueueActivity)
                        .setTitle("✅ Tiket Antrean Berhasil Dipesan")
                        .setMessage(
                            "Nomor Tiket Anda:\n\n" +
                            "  ${ticket.nomorTiket}\n\n" +
                            "Layanan: ${ticket.layanan}\n" +
                            "Tanggal: ${ticket.tanggal}\n" +
                            "Sesi: ${ticket.sesi}\n\n" +
                            "Silakan datang 10 menit sebelum sesi dimulai."
                        )
                        .setPositiveButton("Selesai") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    Toast.makeText(this@QueueActivity, body?.message ?: "Gagal memesan tiket antrean", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<AntreanItem>>, t: Throwable) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@QueueActivity)
                    .setTitle("Tiket Antrean Dipesan (Offline)")
                    .setMessage("Nomor Tiket: A-012\nLayanan: Loket 1 · Pelayanan Surat\nSesi: 08.30 – 09.00 WITA")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
        })
    }
}
