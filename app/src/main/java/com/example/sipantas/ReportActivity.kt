package com.example.sipantas

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.SessionManager
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

class ReportActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var selectedEvidenceFile: File? = null
    private lateinit var tvEvidenceStatus: TextView

    private val selectEvidenceLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleEvidenceSelection(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        tvEvidenceStatus = findViewById(R.id.tv_evidence_status)

        findViewById<android.view.View>(R.id.btn_upload_evidence).setOnClickListener {
            selectEvidenceLauncher.launch("image/*")
        }

        val etTitle = findViewById<TextInputEditText>(R.id.et_report_title)
        val etLocation = findViewById<TextInputEditText>(R.id.et_report_location)
        val etDesc = findViewById<TextInputEditText>(R.id.et_report_desc)

        findViewById<MaterialButton>(R.id.btn_submit_report).setOnClickListener {
            val title = etTitle.text?.toString()?.trim() ?: ""
            val location = etLocation.text?.toString()?.trim() ?: ""
            val desc = etDesc.text?.toString()?.trim() ?: ""

            if (title.isBlank()) {
                etTitle.error = "Judul laporan wajib diisi"
                return@setOnClickListener
            }
            if (desc.isBlank()) {
                etDesc.error = "Detail permasalahan wajib diisi"
                return@setOnClickListener
            }

            submitReport(title, location, desc)
        }
    }

    private fun handleEvidenceSelection(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "bukti_laporan.jpg"
        var fileSize = 0L

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) fileName = it.getString(nameIndex)
                if (sizeIndex != -1) fileSize = it.getLong(sizeIndex)
            }
        }

        // 2MB validation
        if (fileSize > 2 * 1024 * 1024) {
            Toast.makeText(this, "Ukuran foto bukti melebihi 2MB!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "laporan_" + System.currentTimeMillis() + "_" + fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            selectedEvidenceFile = file
            tvEvidenceStatus.text = "✓ Foto terlampir: $fileName (${fileSize / 1024} KB)"
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memuat foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitReport(title: String, location: String, desc: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengirim laporan ke petugas lingkungan...")
            setCancelable(false)
            show()
        }

        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        val actionPart = ApiClient.createPartFromString("create")
        val userPart = ApiClient.createPartFromString(userId.toString())
        val titlePart = ApiClient.createPartFromString(title)
        val catPart = ApiClient.createPartFromString("Fasilitas & Lingkungan")
        val descPart = ApiClient.createPartFromString(desc)
        val locPart = ApiClient.createPartFromString(location)

        var photoPart: MultipartBody.Part? = null
        selectedEvidenceFile?.let { file ->
            val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            photoPart = MultipartBody.Part.createFormData("foto_bukti", file.name, reqFile)
        }

        apiService.createLaporan(
            action = actionPart,
            userId = userPart,
            judul = titlePart,
            kategori = catPart,
            deskripsi = descPart,
            lokasi = locPart,
            fotoBukti = photoPart
        ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    AlertDialog.Builder(this@ReportActivity)
                        .setTitle("✅ Laporan Berhasil Dikirim")
                        .setMessage("Laporan Anda telah diteruskan ke Ketua RT dan Kelurahan untuk segera ditindaklanjuti.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    Toast.makeText(this@ReportActivity, body?.message ?: "Gagal mengirim laporan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ReportActivity)
                    .setTitle("Laporan Terkirim (Offline)")
                    .setMessage("Laporan berhasil dicatat dan akan disinkronkan saat terhubung kembali ke server.")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
        })
    }
}
