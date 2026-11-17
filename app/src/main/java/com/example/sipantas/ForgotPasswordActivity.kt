package com.example.sipantas

import android.app.ProgressDialog
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

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btn_back_to_login).setOnClickListener {
            finish()
        }

        val etIdentifier = findViewById<EditText>(R.id.et_identifier)
        val btnSendRecovery = findViewById<MaterialButton>(R.id.btn_send_recovery)

        btnSendRecovery.setOnClickListener {
            val identifier = etIdentifier.text.toString().trim()
            if (identifier.isEmpty()) {
                Toast.makeText(this, "Harap masukkan nomor ponsel atau email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progress = ProgressDialog(this).apply {
                setMessage("Mengirim petunjuk pemulihan...")
                setCancelable(false)
                show()
            }

            val apiService = ApiClient.getService(this)
            apiService.forgotPassword(identifier).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Any>>>,
                    response: Response<ApiResponse<Map<String, Any>>>
                ) {
                    progress.dismiss()
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val msg = body.message
                        AlertDialog.Builder(this@ForgotPasswordActivity)
                            .setTitle("Petunjuk Pemulihan Terkirim")
                            .setMessage(msg)
                            .setPositiveButton("Masuk Sekarang") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        val errMsg = body?.message ?: "Gagal memproses pemulihan kata sandi."
                        AlertDialog.Builder(this@ForgotPasswordActivity)
                            .setTitle("Pemulihan Gagal")
                            .setMessage(errMsg)
                            .setPositiveButton("Coba Lagi", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    progress.dismiss()
                    AlertDialog.Builder(this@ForgotPasswordActivity)
                        .setTitle("Koneksi Server Terputus")
                        .setMessage("Tidak dapat menghubungi server SIPANTAS.\n\nDetail: ${t.localizedMessage}")
                        .setPositiveButton("Tutup", null)
                        .show()
                }
            })
        }
    }
}
