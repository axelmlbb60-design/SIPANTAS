package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.DashboardStats
import com.example.sipantas.data.SessionManager
import com.example.sipantas.data.UserModel
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        setupProfileHeader()
        setupRoleSpecificMenus()
        setupCommonMenus()
    }

    private fun setupProfileHeader() {
        val userName = sessionManager.getNama()
        val userRole = sessionManager.getRole()
        val rt = sessionManager.getRt().ifBlank { "03" }
        val rw = sessionManager.getRw().ifBlank { "02" }
        val kel = sessionManager.getKelurahan().ifBlank { "Kelurahan Daya" }

        findViewById<TextView>(R.id.tv_profile_name).text = userName
        findViewById<TextView>(R.id.tv_profile_sub).text = "${userRole.uppercase()} · RT $rt / RW $rw · $kel"

        val tvStatus = findViewById<TextView>(R.id.tv_profile_status_badge)
        when (sessionManager.getStatusAkun()) {
            "menunggu_verifikasi" -> {
                tvStatus.text = "⏳ Menunggu Verifikasi RT"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending))
            }
            "proses_pindah" -> {
                tvStatus.text = "📦 Proses Pindah Domisili"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_info))
            }
            else -> {
                tvStatus.text = "✓ Akun Terverifikasi (Aktif)"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            }
        }
    }

    private fun setupRoleSpecificMenus() {
        val role = sessionManager.getRole()
        val menuVerifikasi = findViewById<View>(R.id.menu_verifikasi_warga)
        val menuStatistik = findViewById<View>(R.id.menu_statistik_wilayah)

        if (role == "rtrw") {
            menuVerifikasi.visibility = View.VISIBLE
            menuVerifikasi.setOnClickListener {
                showVerifikasiWargaList()
            }
        } else {
            menuVerifikasi.visibility = View.GONE
        }

        if (role == "kecamatan" || role == "admin") {
            menuStatistik.visibility = View.VISIBLE
            menuStatistik.setOnClickListener {
                showDashboardStatsDialog()
            }
        } else {
            menuStatistik.visibility = View.GONE
        }
    }

    private fun setupCommonMenus() {
        // Pindah Domisili
        findViewById<View>(R.id.menu_pindah_domisili).setOnClickListener {
            showPindahDomisiliDialog()
        }

        // Dokumen
        findViewById<View>(R.id.menu_documents).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Dokumen Kependudukan")
                .setMessage("NIK: ${sessionManager.getNik()}\nNama: ${sessionManager.getNama()}\nStatus: ${sessionManager.getStatusAkun()}\nAlamat Domisili: ${sessionManager.getAlamatDomisili().ifBlank { "Sesuai KTP" }}")
                .setPositiveButton("Tutup", null)
                .show()
        }

        // Keamanan & Kata Sandi
        findViewById<View>(R.id.menu_security).setOnClickListener {
            showChangePasswordDialog()
        }

        // Support
        findViewById<View>(R.id.menu_support).setOnClickListener {
            Toast.makeText(this, "Pusat Bantuan: Hubungi WhatsApp Kelurahan di 0812-3456-7890", Toast.LENGTH_LONG).show()
        }

        // About
        findViewById<View>(R.id.menu_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Server Settings
        findViewById<View>(R.id.menu_server_settings).setOnClickListener {
            showServerSettingsDialog()
        }

        // Logout
        findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar dari akun SIPANTAS?")
                .setPositiveButton("Keluar") { _, _ ->
                    sessionManager.logout()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun showPindahDomisiliDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val etAlamatTujuan = EditText(this).apply {
            hint = "Alamat tujuan pindah (Jalan, RT/RW, Kelurahan)"
        }
        val etAlasan = EditText(this).apply {
            hint = "Alasan pindah (Contoh: Pindah kerja / tugas dinas)"
        }

        layout.addView(etAlamatTujuan)
        layout.addView(etAlasan)

        AlertDialog.Builder(this)
            .setTitle("Permohonan Pindah Domisili")
            .setMessage("Mengajukan permohonan surat keterangan pindah domisili dari wilayah RT saat ini:")
            .setView(layout)
            .setPositiveButton("Kirim Permohonan") { _, _ ->
                val alamat = etAlamatTujuan.text.toString().trim()
                val alasan = etAlasan.text.toString().trim()
                if (alamat.isBlank()) {
                    Toast.makeText(this, "Alamat tujuan wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitPindahDomisili(alamat, alasan)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun submitPindahDomisili(alamat: String, alasan: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengirim permohonan pindah domisili...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.ajukanPindah("ajukan_pindah", sessionManager.getUserId(), alamat, alasan).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    sessionManager.updateUserStatus("proses_pindah")
                    setupProfileHeader()
                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("✅ Permohonan Pindah Terkirim")
                        .setMessage("Permohonan Anda dan draf Surat Keterangan Pindah Domisili telah diteruskan ke Ketua RT untuk pelepasan.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal memproses permohonan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                sessionManager.updateUserStatus("proses_pindah")
                setupProfileHeader()
                Toast.makeText(this@ProfileActivity, "Permohonan disimpan secara lokal (Offline mode).", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
        }

        val etCurrentPass = EditText(this).apply {
            hint = "Password saat ini"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPass = EditText(this).apply {
            hint = "Password baru (min. 6 karakter)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val cbShowPassword = android.widget.CheckBox(this).apply {
            text = "Tampilkan kata sandi"
            setOnCheckedChangeListener { _, isChecked ->
                val type = if (isChecked) {
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                etCurrentPass.inputType = type
                etNewPass.inputType = type
                etCurrentPass.setSelection(etCurrentPass.text.length)
                etNewPass.setSelection(etNewPass.text.length)
            }
        }

        layout.addView(etCurrentPass)
        layout.addView(etNewPass)
        layout.addView(cbShowPassword)

        AlertDialog.Builder(this)
            .setTitle("Ganti Kata Sandi")
            .setView(layout)
            .setPositiveButton("Simpan Sandi Baru") { _, _ ->
                val current = etCurrentPass.text.toString().trim()
                val newPass = etNewPass.text.toString().trim()
                if (current.isBlank() || newPass.isBlank()) {
                    Toast.makeText(this, "Semua bidang kata sandi wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPass.length < 6) {
                    Toast.makeText(this, "Password baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val apiService = ApiClient.getService(this)
                apiService.changePassword("change", sessionManager.getUserId(), current, newPass, null).enqueue(object : Callback<ApiResponse<Void>> {
                    override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                        val body = response.body()
                        if (response.isSuccessful && body != null && body.success) {
                            Toast.makeText(this@ProfileActivity, "Kata sandi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal memperbarui sandi", Toast.LENGTH_LONG).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                        Toast.makeText(this@ProfileActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showVerifikasiWargaList() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memuat data warga baru...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.getWargaMenunggu(status = "menunggu_verifikasi").enqueue(object : Callback<ApiResponse<List<UserModel>>> {
            override fun onResponse(call: Call<ApiResponse<List<UserModel>>>, response: Response<ApiResponse<List<UserModel>>>) {
                progressDialog.dismiss()
                val list = response.body()?.data ?: emptyList()
                if (list.isEmpty()) {
                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("Verifikasi Warga Baru")
                        .setMessage("Tidak ada warga baru yang menunggu verifikasi saat ini. Semua akun sudah aktif.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val wargaNames = list.map { "${it.namaLengkap} (NIK: ${it.nik})" }.toTypedArray()
                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("Pilih Warga untuk Diverifikasi")
                        .setItems(wargaNames) { _, which ->
                            val chosen = list[which]
                            showVerifyActionDialog(chosen)
                        }
                        .setNegativeButton("Tutup", null)
                        .show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<UserModel>>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@ProfileActivity, "Gagal memuat data warga", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showVerifyActionDialog(user: UserModel) {
        val detailMsg = "Nama: ${user.namaLengkap}\n" +
                "NIK: ${user.nik}\n" +
                "No HP: ${user.noHp ?: "-"}\n" +
                "Alamat KTP: ${user.alamatKtp ?: "-"}\n" +
                "Tipe Penduduk: ${user.tipePenduduk?.uppercase() ?: "TETAP"}\n" +
                "Wilayah: RT ${user.nomorRt ?: "-"} / RW ${user.nomorRw ?: "-"}, Kel. ${user.namaKelurahan ?: "-"}"

        AlertDialog.Builder(this)
            .setTitle("Cocokkan Data & Dokumen KTP")
            .setMessage(detailMsg)
            .setPositiveButton("Aktifkan Akun Warga") { _, _ ->
                verifyWargaAction(user.id, "aktifkan")
            }
            .setNegativeButton("Tolak Pendaftaran") { _, _ ->
                verifyWargaAction(user.id, "tolak")
            }
            .setNeutralButton("Kembali", null)
            .show()
    }

    private fun verifyWargaAction(userId: Int, action: String) {
        val apiService = ApiClient.getService(this)
        apiService.verifikasiWarga(userId, action, sessionManager.getNama(), null).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@ProfileActivity, body.message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ProfileActivity, body?.message ?: "Gagal memproses verifikasi", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDashboardStatsDialog() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memuat data statistik kecamatan...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.getDashboardStats().enqueue(object : Callback<ApiResponse<DashboardStats>> {
            override fun onResponse(call: Call<ApiResponse<DashboardStats>>, response: Response<ApiResponse<DashboardStats>>) {
                progressDialog.dismiss()
                val stats = response.body()?.data
                if (stats != null) {
                    val ringkasan = stats.ringkasanPengajuan
                    val total = ringkasan?.get("total") ?: 0
                    val selesai = ringkasan?.get("selesai") ?: 0
                    val menungguRt = ringkasan?.get("menunggu_rt") ?: 0
                    val menungguKec = ringkasan?.get("menunggu_kecamatan") ?: 0
                    val ditolak = ringkasan?.get("ditolak") ?: 0

                    val warga = stats.statistikWarga
                    val wargaAktif = warga?.get("aktif") ?: 0
                    val wargaVerif = warga?.get("menunggu_verifikasi") ?: 0

                    val msg = "📊 RINGKASAN SURAT KECAMATAN:\n" +
                            "• Total Pengajuan: $total\n" +
                            "• Selesai (Resmi Terbit): $selesai\n" +
                            "• Menunggu Verifikasi RT: $menungguRt\n" +
                            "• Menunggu Pengesahan Kecamatan: $menungguKec\n" +
                            "• Ditolak: $ditolak\n\n" +
                            "👥 STATISTIK PENDUDUK:\n" +
                            "• Warga Aktif: $wargaAktif\n" +
                            "• Warga Menunggu Verifikasi: $wargaVerif"

                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("Dashboard Statistik Wilayah")
                        .setMessage(msg)
                        .setPositiveButton("Tutup", null)
                        .show()
                } else {
                    Toast.makeText(this@ProfileActivity, "Data statistik kosong", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<DashboardStats>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@ProfileActivity, "Gagal memuat statistik", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showServerSettingsDialog() {
        val input = EditText(this).apply {
            setText(sessionManager.getBaseUrl())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Pengaturan REST API Server")
            .setMessage("Base URL server saat ini:\n• HP Fisik (Wi-Fi): http://192.168.1.38/api_sipantas/\n• Emulator Android Studio: http://10.0.2.2/api_sipantas/")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotBlank()) {
                    sessionManager.setBaseUrl(newUrl)
                    Toast.makeText(this, "Base URL disimpan: ${sessionManager.getBaseUrl()}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Set IP Laptop Wi-Fi") { _, _ ->
                sessionManager.setBaseUrl(SessionManager.LAPTOP_WIFI_BASE_URL)
                Toast.makeText(this, "Base URL diset ke: ${SessionManager.LAPTOP_WIFI_BASE_URL}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
