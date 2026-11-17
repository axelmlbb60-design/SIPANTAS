package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.SessionManager
import com.example.sipantas.data.UserModel
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var currentRole: String = "Warga"
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentRole = intent.getStringExtra("EXTRA_SELECTED_ROLE") ?: "Warga"
        val tvRoleChip = findViewById<TextView>(R.id.tv_role_chip)
        tvRoleChip.text = "Masuk sebagai $currentRole  ·  Ganti peran"

        tvRoleChip.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        val navigateBack = {
            if (isTaskRoot) {
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            navigateBack()
        }

        onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }

        findViewById<TextView>(R.id.tv_register_link).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        findViewById<TextView>(R.id.tv_forgot_password).setOnLongClickListener {
            showServerIpDialog()
            true
        }

        val etIdentifier = findViewById<TextInputEditText>(R.id.et_identifier)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)

        // Form login kosong secara default
        etIdentifier.setText("")
        etPassword.setText("")

        findViewById<MaterialButton>(R.id.btn_login).setOnClickListener {
            val identifier = etIdentifier.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""

            if (identifier.isBlank()) {
                etIdentifier.error = "NIK / Nomor Pegawai wajib diisi"
                return@setOnClickListener
            }
            if (password.isBlank()) {
                etPassword.error = "Kata sandi wajib diisi"
                return@setOnClickListener
            }

            performLogin(identifier, password)
        }
    }

    private fun performLogin(nik: String, pass: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menghubungkan ke server SIPANTAS...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.login(nik, pass).enqueue(object : Callback<ApiResponse<UserModel>> {
            override fun onResponse(
                call: Call<ApiResponse<UserModel>>,
                response: Response<ApiResponse<UserModel>>
            ) {
                progressDialog.dismiss()
                val body = response.body()

                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    val user = body.data
                    sessionManager.saveUserSession(user)

                    Toast.makeText(this@LoginActivity, "Selamat datang, ${user.namaLengkap}!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("EXTRA_USER_NAME", user.namaLengkap)
                        putExtra("EXTRA_ROLE", user.role.replaceFirstChar { it.uppercase() })
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errMsg = body?.message ?: "Gagal masuk. Periksa kembali NIK dan password Anda."
                    showErrorDialog("Gagal Masuk", errMsg)
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserModel>>, t: Throwable) {
                progressDialog.dismiss()
                showOfflineOptionsDialog(t.localizedMessage ?: "Tidak dapat menghubungi server.")
            }
        })
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showOfflineOptionsDialog(errorDetail: String) {
        AlertDialog.Builder(this)
            .setTitle("Koneksi Server Terputus")
            .setMessage("Aplikasi tidak dapat terhubung ke REST API di:\n${sessionManager.getBaseUrl()}\n\nDetail: $errorDetail\n\nPastikan Apache & MySQL di XAMPP aktif. Anda juga dapat mengubah IP Server jika menguji di HP fisik.")
            .setPositiveButton("Ubah URL Server") { _, _ ->
                showServerIpDialog()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showServerIpDialog() {
        val input = EditText(this).apply {
            setText(sessionManager.getBaseUrl())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Pengaturan REST API Server")
            .setMessage("Pilih atau ketik Base URL server SIPANTAS:\n• HP Fisik (Wi-Fi): http://192.168.1.38/api_sipantas/\n• Emulator Android Studio: http://10.0.2.2/api_sipantas/")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotBlank()) {
                    sessionManager.setBaseUrl(newUrl)
                    Toast.makeText(this, "Base URL tersimpan: ${sessionManager.getBaseUrl()}", Toast.LENGTH_SHORT).show()
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
