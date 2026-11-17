package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.SessionManager
import com.example.sipantas.data.UserModel
import com.example.sipantas.data.WilayahItem
import com.example.sipantas.network.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

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
    private lateinit var etPassword: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etRw: TextInputEditText
    private lateinit var etRt: TextInputEditText
    private lateinit var tvUploadStatus: TextView

    private var selectedKtpUri: Uri? = null
    private var selectedKtpFile: File? = null

    // Wilayah selections
    private var selectedKecamatanId: Int = 1
    private var selectedKelurahanId: Int = 1
    private var selectedRwId: Int = 1
    private var selectedRtId: Int = 1

    private val selectKtpLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleKtpFileSelection(uri)
        }
    }

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
        etPassword = findViewById(R.id.et_register_password)
        etAddress = findViewById(R.id.et_address)
        etRw = findViewById(R.id.et_rw)
        etRt = findViewById(R.id.et_rt)
        tvUploadStatus = findViewById(R.id.tv_upload_ktp_status)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            handleBackPress()
        }

        onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }

        findViewById<View>(R.id.btn_upload_ktp).setOnClickListener {
            selectKtpLauncher.launch("*/*")
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnAction.setOnClickListener {
            handleNextStep()
        }

        // Form kosong secara default
        etFullName.setText("")
        etNik.setText("")
        etPhone.setText("")
        etPassword.setText("")
        etAddress.setText("")
        etRw.setText("")
        etRt.setText("")
    }

    private fun handleKtpFileSelection(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "berkas_ktp"
        var fileSize = 0L

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) fileName = it.getString(nameIndex)
                if (sizeIndex != -1) fileSize = it.getLong(sizeIndex)
            }
        }

        // Validate max 2MB as required
        val maxSizeBytes = 2 * 1024 * 1024
        if (fileSize > maxSizeBytes) {
            Toast.makeText(this, "Ukuran berkas melebihi batas maksimal 2MB!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "ktp_upload_" + System.currentTimeMillis() + "_" + fileName)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            selectedKtpFile = tempFile
            selectedKtpUri = uri
            tvUploadStatus.text = "✓ Berkas terpilih: $fileName (${fileSize / 1024} KB)"
            Toast.makeText(this, "Dokumen KTP siap diunggah", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNextStep() {
        when (currentStep) {
            1 -> {
                val name = etFullName.text?.toString()?.trim() ?: ""
                val nik = etNik.text?.toString()?.trim() ?: ""
                val phone = etPhone.text?.toString()?.trim() ?: ""
                val pass = etPassword.text?.toString()?.trim() ?: ""

                if (name.isBlank()) {
                    etFullName.error = "Nama lengkap wajib diisi"
                    return
                }
                // Validasi NIK 16 digit sesuai spesifikasi
                if (nik.length != 16 || !nik.all { it.isDigit() }) {
                    etNik.error = "NIK harus tepat 16 digit angka"
                    Toast.makeText(this, "Validasi Gagal: NIK wajib 16 digit!", Toast.LENGTH_SHORT).show()
                    return
                }
                if (phone.length < 10) {
                    etPhone.error = "Nomor ponsel minimal 10 digit"
                    return
                }
                if (pass.length < 6) {
                    etPassword.error = "Kata sandi minimal 6 karakter"
                    return
                }

                currentStep = 2
                updateStepUI()
            }
            2 -> {
                val rw = etRw.text?.toString()?.trim() ?: ""
                val rt = etRt.text?.toString()?.trim() ?: ""
                val address = etAddress.text?.toString()?.trim() ?: ""
                if (rw.isBlank()) {
                    etRw.error = "Nomor RW wajib diisi"
                    return
                }
                if (rt.isBlank()) {
                    etRt.error = "Nomor RT wajib diisi"
                    return
                }
                if (address.isBlank()) {
                    etAddress.error = "Alamat lengkap wajib diisi"
                    return
                }
                currentStep = 3
                updateStepUI()
            }
            3 -> {
                submitRegistration()
            }
            4 -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("EXTRA_USER_NAME", etFullName.text?.toString()?.trim() ?: "Budi Santoso")
                    putExtra("EXTRA_ROLE", "Warga")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun submitRegistration() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mendaftarkan akun ke sistem SIPANTAS...")
            setCancelable(false)
            show()
        }

        val name = etFullName.text?.toString()?.trim() ?: ""
        val nik = etNik.text?.toString()?.trim() ?: ""
        val phone = etPhone.text?.toString()?.trim() ?: ""
        val pass = etPassword.text?.toString()?.trim() ?: ""
        val address = etAddress.text?.toString()?.trim() ?: ""

        val apiService = ApiClient.getService(this)

        val nikPart = ApiClient.createPartFromString(nik)
        val namePart = ApiClient.createPartFromString(name)
        val passPart = ApiClient.createPartFromString(pass)
        val phonePart = ApiClient.createPartFromString(phone)
        val rolePart = ApiClient.createPartFromString("warga")
        val tipePart = ApiClient.createPartFromString("tetap")
        val alamatKtpPart = ApiClient.createPartFromString(address)
        val alamatDomisiliPart = ApiClient.createPartFromString(address)
        val kecPart = ApiClient.createPartFromString(selectedKecamatanId.toString())
        val kelPart = ApiClient.createPartFromString(selectedKelurahanId.toString())
        val rwPart = ApiClient.createPartFromString(selectedRwId.toString())
        val rtPart = ApiClient.createPartFromString(selectedRtId.toString())

        var fotoPart: MultipartBody.Part? = null
        selectedKtpFile?.let { file ->
            val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            fotoPart = MultipartBody.Part.createFormData("foto_ktp", file.name, reqFile)
        }

        apiService.register(
            nik = nikPart,
            nama = namePart,
            pass = passPart,
            email = null,
            noHp = phonePart,
            role = rolePart,
            tipePenduduk = tipePart,
            alamatKtp = alamatKtpPart,
            alamatDomisili = alamatDomisiliPart,
            kecId = kecPart,
            kelId = kelPart,
            rwId = rwPart,
            rtId = rtPart,
            fotoKtp = fotoPart
        ).enqueue(object : Callback<ApiResponse<UserModel>> {
            override fun onResponse(
                call: Call<ApiResponse<UserModel>>,
                response: Response<ApiResponse<UserModel>>
            ) {
                progressDialog.dismiss()
                val body = response.body()

                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    val user = body.data
                    SessionManager(this@RegisterActivity).saveUserSession(user)
                    currentStep = 4
                    updateStepUI()
                    Toast.makeText(this@RegisterActivity, "Pendaftaran berhasil dikirim!", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = body?.message ?: "Gagal registrasi. Pastikan NIK belum pernah terdaftar."
                    AlertDialog.Builder(this@RegisterActivity)
                        .setTitle("Registrasi Ditolak")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserModel>>, t: Throwable) {
                progressDialog.dismiss()
                // If offline / demo mode fallback
                Toast.makeText(this@RegisterActivity, "Server Offline: Melanjutkan ke halaman verifikasi lokal.", Toast.LENGTH_LONG).show()
                currentStep = 4
                updateStepUI()
            }
        })
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
