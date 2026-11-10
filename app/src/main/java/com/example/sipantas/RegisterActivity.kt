package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private var currentStep = 1

    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    private lateinit var step4Container: LinearLayout

    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvStepIndicator: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var tvLoginLink: TextView

    private lateinit var etFullName: TextInputEditText
    private lateinit var etNik: TextInputEditText
    private lateinit var etPhone: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        step1Container = findViewById(R.id.step_1_container)
        step2Container = findViewById(R.id.step_2_container)
        step3Container = findViewById(R.id.step_3_container)
        step4Container = findViewById(R.id.step_4_container)

        tvHeaderTitle = findViewById(R.id.tv_header_title)
        tvStepIndicator = findViewById(R.id.tv_step_indicator)
        btnAction = findViewById(R.id.btn_action)
        tvLoginLink = findViewById(R.id.tv_login_link)

        etFullName = findViewById(R.id.et_full_name)
        etNik = findViewById(R.id.et_nik)
        etPhone = findViewById(R.id.et_phone)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            handleBackPress()
        }

        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }

        findViewById<View>(R.id.btn_upload_ktp).setOnClickListener {
            findViewById<TextView>(R.id.tv_upload_ktp_status).text = "✓ Berkas KTP berhasil dipilih (ktp_warga.jpg)"
            Toast.makeText(this, "Foto KTP dipilih", Toast.LENGTH_SHORT).show()
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnAction.setOnClickListener {
            handleNextStep()
        }
    }

    private fun handleNextStep() {
        when (currentStep) {
            1 -> {
                val name = etFullName.text?.toString()?.trim()
                if (name.isNullOrBlank()) {
                    etFullName.error = "Nama lengkap wajib diisi"
                    return
                }
                currentStep = 2
                updateStepUI()
            }
            2 -> {
                currentStep = 3
                updateStepUI()
            }
            3 -> {
                currentStep = 4
                updateStepUI()
            }
            4 -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("EXTRA_USER_NAME", etFullName.text?.toString()?.ifBlank { "Budi Santoso" } ?: "Budi Santoso")
                intent.putExtra("EXTRA_ROLE", "Warga")
                startActivity(intent)
                finishAffinity()
            }
        }
    }

    private fun handleBackPress() {
        if (currentStep > 1 && currentStep < 4) {
            currentStep--
            updateStepUI()
        } else {
            finish()
        }
    }

    private fun updateStepUI() {
        step1Container.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        step2Container.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        step3Container.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        step4Container.visibility = if (currentStep == 4) View.VISIBLE else View.GONE

        when (currentStep) {
            1 -> {
                tvHeaderTitle.text = "Registrasi warga"
                tvStepIndicator.visibility = View.VISIBLE
                tvStepIndicator.text = "Langkah 1/3"
                btnAction.text = "Lanjut ke alamat"
                tvLoginLink.visibility = View.VISIBLE
            }
            2 -> {
                tvHeaderTitle.text = "Alamat & identitas"
                tvStepIndicator.visibility = View.VISIBLE
                tvStepIndicator.text = "Langkah 2/3"
                btnAction.text = "Lanjut verifikasi nomor"
                tvLoginLink.visibility = View.GONE
            }
            3 -> {
                tvHeaderTitle.text = "Keamanan akun"
                tvStepIndicator.visibility = View.VISIBLE
                tvStepIndicator.text = "Langkah 3/3"
                val phone = etPhone.text?.toString()?.ifBlank { "0812••••7890" } ?: "0812••••7890"
                findViewById<TextView>(R.id.tv_otp_phone_desc).text = "Masukkan 6 digit kode yang dikirim ke $phone"
                btnAction.text = "Verifikasi & kirim pendaftaran"
                tvLoginLink.visibility = View.GONE
            }
            4 -> {
                tvHeaderTitle.text = "Status pendaftaran"
                tvStepIndicator.visibility = View.GONE
                btnAction.text = "Masuk ke Beranda"
                tvLoginLink.visibility = View.GONE
            }
        }
    }
}
