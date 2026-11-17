package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.NotificationItem
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: NotificationAdapter
    private lateinit var rvNotifications: RecyclerView
    private lateinit var containerEmpty: View

    private lateinit var filterAll: TextView
    private lateinit var filterSurat: TextView
    private lateinit var filterAntrean: TextView
    private lateinit var filterLaporan: TextView

    private var currentFilter: String = "semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notification_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        com.example.sipantas.ui.BottomNavHelper.setupBottomNav(this, com.example.sipantas.ui.BottomNavHelper.TAB_KABAR)

        rvNotifications = findViewById(R.id.rv_notifications)
        containerEmpty = findViewById(R.id.container_empty_notif)

        filterAll = findViewById(R.id.filter_all)
        filterSurat = findViewById(R.id.filter_surat)
        filterAntrean = findViewById(R.id.filter_antrean)
        filterLaporan = findViewById(R.id.filter_laporan)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(emptyList()) { item ->
            handleNotificationClick(item)
        }
        rvNotifications.adapter = adapter

        filterAll.setOnClickListener { setFilter("semua") }
        filterSurat.setOnClickListener { setFilter("surat") }
        filterAntrean.setOnClickListener { setFilter("antrean") }
        filterLaporan.setOnClickListener { setFilter("laporan") }

        findViewById<MaterialButton>(R.id.btn_mark_all_read).setOnClickListener {
            markAllAsRead()
        }

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        com.example.sipantas.ui.BottomNavHelper.setupBottomNav(this, com.example.sipantas.ui.BottomNavHelper.TAB_KABAR)
        loadNotifications()
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_primary_button)
        val inactiveBg = ContextCompat.getDrawable(this, R.drawable.bg_role_card)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val blueColor = ContextCompat.getColor(this, R.color.brand_primary)

        val chips = listOf(
            Triple(filterAll, "semua", "Semua"),
            Triple(filterSurat, "surat", "Surat"),
            Triple(filterAntrean, "antrean", "Antrean"),
            Triple(filterLaporan, "laporan", "Laporan")
        )

        for ((chip, fKey, _) in chips) {
            if (fKey == currentFilter) {
                chip.background = activeBg
                chip.setTextColor(whiteColor)
            } else {
                chip.background = inactiveBg
                chip.setTextColor(blueColor)
            }
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        apiService.getNotifications(userId = userId, filter = currentFilter)
            .enqueue(object : Callback<ApiResponse<List<NotificationItem>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<NotificationItem>>>,
                    response: Response<ApiResponse<List<NotificationItem>>>
                ) {
                    val list = response.body()?.data ?: emptyList()
                    if (list.isEmpty()) {
                        containerEmpty.visibility = View.VISIBLE
                        rvNotifications.visibility = View.GONE
                    } else {
                        containerEmpty.visibility = View.GONE
                        rvNotifications.visibility = View.VISIBLE
                        adapter.updateData(list)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<NotificationItem>>>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "Gagal memuat notifikasi: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markAllAsRead() {
        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        apiService.getNotifications(userId = userId, action = "mark_all_read")
            .enqueue(object : Callback<ApiResponse<List<NotificationItem>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<NotificationItem>>>,
                    response: Response<ApiResponse<List<NotificationItem>>>
                ) {
                    Toast.makeText(this@NotificationActivity, "Semua notifikasi telah ditandai dibaca", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                }

                override fun onFailure(call: Call<ApiResponse<List<NotificationItem>>>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, "Gagal memperbarui: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleNotificationClick(item: NotificationItem) {
        val detailDialog = AlertDialog.Builder(this)
            .setTitle(item.judul)
            .setMessage("${item.pesan}\n\nWaktu: ${item.waktuLalu ?: "Terkini"}")
            .setPositiveButton("Tutup", null)

        when (item.tipe?.lowercase()) {
            "surat" -> {
                detailDialog.setNeutralButton("Buka Detail Surat") { _, _ ->
                    val intent = Intent(this, LetterDetailActivity::class.java).apply {
                        putExtra("EXTRA_PENGAJUAN_ID", item.dataId ?: 1)
                        putExtra("EXTRA_SERVICE_NAME", item.judul)
                    }
                    startActivity(intent)
                }
            }
            "antrean" -> {
                detailDialog.setNeutralButton("Lihat Antrean") { _, _ ->
                    val intent = Intent(this, QueueActivity::class.java)
                    startActivity(intent)
                }
            }
            "laporan" -> {
                detailDialog.setNeutralButton("Lihat Laporan") { _, _ ->
                    val intent = Intent(this, ReportActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        detailDialog.show()
    }
}
