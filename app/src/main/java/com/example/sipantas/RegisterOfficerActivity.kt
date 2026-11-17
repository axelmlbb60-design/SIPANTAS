package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterOfficerActivity : AppCompatActivity() {

    private var targetRole: String = "rtrw"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_officer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_officer_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        targetRole = intent.getStringExtra("EXTRA_ROLE") ?: "rtrw"

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btn_back_to_role).setOnClickListener {
            finish()
        }

        val tvRoleBadge = findViewById<TextView>(R.id.tv_officer_role_badge)
        val tvLabelExtra1 = findViewById<TextView>(R.id.tv_label_extra1)
        val etExtra1 = findViewById<EditText>(R.id.et_officer_extra1)
        val etInviteCode = findViewById<EditText>(R.id.et_invite_code)
        val etNik = findViewById<EditText>(R.id.et_officer_nik)
        val etName = findViewById<EditText>(R.id.et_officer_name)
        val etPhone = findViewById<EditText>(R.id.et_officer_phone)
        val etEmail = findViewById<EditText>(R.id.et_officer_email)
        val etPassword = findViewById<EditText>(R.id.et_officer_password)
        val btnActivate = findViewById<MaterialButton>(R.id.btn_activate_officer)

        when (targetRole) {
            "rtrw" -> {
                tvRoleBadge.text = "Aktivasi Akun RT / RW"
                tvLabelExtra1.text = "Nomor SK Pengangkatan RT / RW"
                etExtra1.hint = "Contoh: SK.04/LURAH/2024"
                etInviteCode.setText("INV-RTRW-2024")
                btnActivate.text = "Aktivasi akun RT / RW"
            }
            "kelurahan", "kecamatan" -> {
                tvRoleBadge.text = "Aktivasi Petugas Kelurahan"
                tvLabelExtra1.text = "NIP (Nomor Induk Pegawai)"
                etExtra1.hint = "18 digit NIP resmi kedinasan"
                etInviteCode.setText("INV-KEL-2024")
                btnActivate.text = "Aktivasi akun petugas kelurahan"
            }
            "admin" -> {
                tvRoleBadge.text = "Aktivasi Admin Sistem"
                tvLabelExtra1.text = "Master Token Pengelola"
                etExtra1.hint = "Token master otorisasi sistem"
                etInviteCode.setText("INV-ADMIN-2024")
                btnActivate.text = "Aktivasi akun admin sistem"
            }
        }

        btnActivate.setOnClickListener {
            val code = etInviteCode.text.toString().trim()
            val nik = etNik.text.toString().trim()
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val extra1 = etExtra1.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (code.isEmpty() || nik.isEmpty() || name.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Kode undangan, NIK, nama lengkap, dan kata sandi wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nik.length != 16) {
                Toast.makeText(this, "Nomor NIK harus tepat 16 digit angka!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progress = ProgressDialog(this).apply {
                setMessage("Mengaktivasi akun penugasan resmi...")
                setCancelable(false)
                show()
            }

            val apiService = ApiClient.getService(this)
            val nip = if (targetRole == "kelurahan" || targetRole == "kecamatan") extra1 else null
            val sk = if (targetRole == "rtrw") extra1 else null

            apiService.registerOfficer(
                role = targetRole,
                kode = code,
                nik = nik,
                nama = name,
                noHp = phone,
                email = email,
                pass = password,
                nip = nip,
                sk = sk
            ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Any>>>,
                    response: Response<ApiResponse<Map<String, Any>>>
                ) {
                    progress.dismiss()
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val assignedNik = body.data?.get("nik")?.toString() ?: ""
                        AlertDialog.Builder(this@RegisterOfficerActivity)
                            .setTitle("Aktivasi Akun Berhasil!")
                            .setMessage("${body.message}\n\nCatat NIK Anda untuk masuk:\n$assignedNik")
                            .setPositiveButton("Masuk Sekarang") { _, _ ->
                                val intent = Intent(this@RegisterOfficerActivity, LoginActivity::class.java).apply {
                                    putExtra("EXTRA_SELECTED_ROLE", when (targetRole) {
                                        "rtrw" -> "RT / RW"
                                        "kelurahan", "kecamatan" -> "Petugas Kelurahan"
                                        "admin" -> "Admin Sistem"
                                        else -> "Warga"
                                    })
                                }
                                startActivity(intent)
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        val errMsg = body?.message ?: "Gagal mengaktivasi akun petugas."
                        AlertDialog.Builder(this@RegisterOfficerActivity)
                            .setTitle("Aktivasi Ditolak")
                            .setMessage(errMsg)
                            .setPositiveButton("Periksa Kembali", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    progress.dismiss()
                    AlertDialog.Builder(this@RegisterOfficerActivity)
                        .setTitle("Koneksi Terputus")
                        .setMessage("Tidak dapat terhubung ke server SIPANTAS.\n\nDetail: ${t.localizedMessage}")
                        .setPositiveButton("Tutup", null)
                        .show()
                }
            })
        }
    }
}
