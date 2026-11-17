package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.PengajuanItem
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var activePengajuan: PengajuanItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupUserHeader()
        setupAccountBanner()
        setupNavigation()

        // Inisialisasi Channel Notifikasi Android
        com.example.sipantas.notification.NotificationHelper.createNotificationChannel(this)
    }

    override fun onResume() {
        super.onResume()
        loadSubmissions()
    }

    private fun setupUserHeader() {
        val userName = intent.getStringExtra("EXTRA_USER_NAME") ?: sessionManager.getNama()
        val userRole = intent.getStringExtra("EXTRA_ROLE") ?: sessionManager.getRole()

        findViewById<TextView>(R.id.tv_greeting).text = "Halo, $userName"

        val rt = sessionManager.getRt().ifBlank { "03" }
        val rw = sessionManager.getRw().ifBlank { "02" }
        val kel = sessionManager.getKelurahan().ifBlank { "Kelurahan Daya" }

        val displayRole = when (userRole.lowercase()) {
            "rtrw", "rt / rw" -> "Ketua RT / RW"
            "kecamatan", "petugas kelurahan" -> "Petugas Kelurahan"
            "admin", "admin sistem" -> "Admin Sistem"
            else -> "Warga"
        }

        findViewById<TextView>(R.id.tv_sub_badge).text = "$displayRole · RT $rt / RW $rw · $kel"

        findViewById<ImageView>(R.id.btn_header_profile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("EXTRA_USER_NAME", userName)
                putExtra("EXTRA_ROLE", displayRole)
            }
            startActivity(intent)
        }
    }

    private fun setupAccountBanner() {
        val bannerStatus = findViewById<LinearLayout>(R.id.banner_account_status)
        val tvBannerIcon = findViewById<TextView>(R.id.tv_banner_icon)
        val tvBannerTitle = findViewById<TextView>(R.id.tv_banner_title)
        val tvBannerDesc = findViewById<TextView>(R.id.tv_banner_desc)

        val statusAkun = sessionManager.getStatusAkun()

        when (statusAkun) {
            "menunggu_verifikasi" -> {
                bannerStatus.visibility = View.VISIBLE
                tvBannerIcon.text = "⏳"
                tvBannerTitle.text = "Status Akun: Menunggu Verifikasi RT"
                tvBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.status_pending))
                tvBannerDesc.text = "Data pendaftaran & KTP Anda sedang dicocokkan oleh Ketua RT setempat."
            }
            "proses_pindah" -> {
                bannerStatus.visibility = View.VISIBLE
                tvBannerIcon.text = "📦"
                tvBannerTitle.text = "Status Akun: Proses Pindah Domisili"
                tvBannerTitle.setTextColor(ContextCompat.getColor(this, R.color.status_info))
                tvBannerDesc.text = "Permohonan pindah telah diajukan. Menunggu konfirmasi penerimaan oleh RT baru."
            }
            else -> {
                bannerStatus.visibility = View.GONE
            }
        }
    }

    private fun setupNavigation() {
        // Hero button -> ServiceList
        findViewById<MaterialButton>(R.id.btn_hero_apply).setOnClickListener {
            startActivity(Intent(this, ServiceListActivity::class.java))
        }

        // Antrean -> QueueActivity
        findViewById<View>(R.id.action_antrean).setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }

        // Lapor warga -> ReportActivity
        findViewById<View>(R.id.action_lapor).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        // Click on recent application card -> LetterDetailActivity
        findViewById<View>(R.id.card_active_letter).setOnClickListener {
            val intent = Intent(this, LetterDetailActivity::class.java)
            if (activePengajuan != null) {
                intent.putExtra("EXTRA_PENGAJUAN_ID", activePengajuan!!.id)
                intent.putExtra("EXTRA_SERVICE_NAME", activePengajuan!!.jenisSurat)
            } else {
                intent.putExtra("EXTRA_SERVICE_NAME", "Surat Keterangan Domisili")
            }
            startActivity(intent)
        }

        // Bottom Navigation Bar Clicks
        findViewById<View>(R.id.nav_beranda).setOnClickListener {
            // Already on Beranda
        }

        findViewById<View>(R.id.nav_pengajuan).setOnClickListener {
            startActivity(Intent(this, ServiceListActivity::class.java))
        }

        findViewById<View>(R.id.nav_kabar).setOnClickListener {
            Toast.makeText(this, "Kabar Wilayah: Layanan pembuatan dokumen digital aktif 24 jam.", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_bantuan).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSubmissions() {
        val role = sessionManager.getRole()
        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        val bannerRtrw = findViewById<LinearLayout>(R.id.banner_rtrw_badge)
        val tvRtrwCount = findViewById<TextView>(R.id.tv_rtrw_badge_count)
        val tvRtrwDesc = findViewById<TextView>(R.id.tv_rtrw_badge_desc)

        // For RT/RW: fetch pending submissions for badge
        if (role == "rtrw") {
            apiService.getPengajuanList(role = "rtrw", status = "menunggu_rt").enqueue(object : Callback<ApiResponse<List<PengajuanItem>>> {
                override fun onResponse(call: Call<ApiResponse<List<PengajuanItem>>>, response: Response<ApiResponse<List<PengajuanItem>>>) {
                    val list = response.body()?.data ?: emptyList()
                    val count = list.size
                    if (count > 0) {
                        bannerRtrw.visibility = View.VISIBLE
                        tvRtrwCount.text = count.toString()
                        tvRtrwDesc.text = "Ada $count permohonan surat warga menunggu tanda tangan digital Anda."
                        
                        // Tampilkan System Notification di perangkat RT / RW
                        com.example.sipantas.notification.NotificationHelper.showSystemNotification(
                            context = this@MainActivity,
                            title = "📬 Permohonan Surat Baru Masuk!",
                            message = "Ada $count surat pengantar baru dari warga menunggu tanda tangan digital Anda.",
                            notificationId = 777,
                            intent = Intent(this@MainActivity, LetterDetailActivity::class.java).apply {
                                putExtra("EXTRA_PENGAJUAN_ID", list[0].id)
                                putExtra("EXTRA_SERVICE_NAME", list[0].jenisSurat)
                            }
                        )

                        bannerRtrw.setOnClickListener {
                            val intent = Intent(this@MainActivity, LetterDetailActivity::class.java).apply {
                                putExtra("EXTRA_PENGAJUAN_ID", list[0].id)
                                putExtra("EXTRA_SERVICE_NAME", list[0].jenisSurat)
                            }
                            startActivity(intent)
                        }
                    } else {
                        bannerRtrw.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<PengajuanItem>>>, t: Throwable) {
                    bannerRtrw.visibility = View.GONE
                }
            })
        } else {
            bannerRtrw.visibility = View.GONE
        }

        // Load recent submission for user
        val userFilter = if (role == "warga") userId else null
        apiService.getPengajuanList(userId = userFilter).enqueue(object : Callback<ApiResponse<List<PengajuanItem>>> {
            override fun onResponse(call: Call<ApiResponse<List<PengajuanItem>>>, response: Response<ApiResponse<List<PengajuanItem>>>) {
                val list = response.body()?.data
                val cardActive = findViewById<View>(R.id.card_active_letter)
                val tvEmpty = findViewById<View>(R.id.tv_empty_submissions)
                val tvActiveCount = findViewById<TextView>(R.id.tv_active_count)

                if (!list.isNullOrEmpty()) {
                    activePengajuan = list[0]
                    cardActive.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    tvActiveCount.text = "${list.size} pengajuan aktif"

                    findViewById<TextView>(R.id.tv_main_letter_title).text = activePengajuan!!.jenisSurat
                    findViewById<TextView>(R.id.tv_main_letter_sub).text = "${activePengajuan!!.nomorSurat} · Pemohon: ${activePengajuan!!.namaLengkap ?: sessionManager.getNama()}"

                    val badge = findViewById<TextView>(R.id.tv_main_letter_badge)
                    when (activePengajuan!!.status) {
                        "menunggu_rt" -> {
                            badge.text = "Menunggu RT"
                            badge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_pending))
                        }
                        "menunggu_kecamatan" -> {
                            badge.text = "Menunggu Kelurahan/Kecamatan"
                            badge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_info))
                        }
                        "selesai" -> {
                            badge.text = "Selesai (QR Terbit)"
                            badge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_success))
                        }
                        "ditolak_rt" -> {
                            badge.text = "Ditolak RT"
                            badge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_danger))
                        }
                        "ditolak_kecamatan" -> {
                            badge.text = "Ditolak Kecamatan"
                            badge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_danger))
                        }
                        else -> {
                            badge.text = activePengajuan!!.status
                        }
                    }

                    findViewById<TextView>(R.id.tv_main_letter_date).text = "Diajukan: ${activePengajuan!!.createdAt.take(10)}"
                } else {
                    cardActive.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvActiveCount.text = "0 pengajuan"
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PengajuanItem>>>, t: Throwable) {
                // Keep default preview card on failure
            }
        })
    }
}
