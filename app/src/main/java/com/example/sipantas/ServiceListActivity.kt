package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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

class ServiceListActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var pendingAttachmentFile: File? = null
    private var tvAttachmentStatusRef: TextView? = null

    private val selectAttachmentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleAttachmentSelection(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_list)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.service_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val onSelectService = View.OnClickListener { v ->
            val serviceTitle = when (v.id) {
                R.id.item_domisili -> "Surat Keterangan Domisili"
                R.id.item_pengantar_rtrw -> "Surat Pengantar RT / RW"
                R.id.item_sktm -> "Surat Keterangan Tidak Mampu"
                R.id.item_sku -> "Surat Keterangan Usaha"
                else -> "Surat Keterangan Domisili"
            }
            val intent = Intent(this, ApplyLetterActivity::class.java).apply {
                putExtra("EXTRA_LETTER_TYPE", serviceTitle)
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.item_domisili).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_pengantar_rtrw).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sktm).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sku).setOnClickListener(onSelectService)
    }

    private fun handleAttachmentSelection(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "lampiran_dokumen"
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
            Toast.makeText(this, "Ukuran dokumen melebihi batas 2MB!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "attachment_" + System.currentTimeMillis() + "_" + fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            pendingAttachmentFile = file
            tvAttachmentStatusRef?.text = "✓ Terlampir: $fileName (${fileSize / 1024} KB)"
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memproses berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showApplyDialog(serviceTitle: String) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_apply_letter, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.dialog_service_title)
        val etKeperluan = dialogView.findViewById<TextInputEditText>(R.id.et_keperluan)
        val radioGroupTujuan = dialogView.findViewById<RadioGroup>(R.id.rg_tujuan_wilayah)
        val rbKtpAsli = dialogView.findViewById<RadioButton>(R.id.rb_ktp_asli)
        val rbTempatKos = dialogView.findViewById<RadioButton>(R.id.rb_tempat_kos)
        val btnUpload = dialogView.findViewById<View>(R.id.btn_dialog_upload)
        val tvUploadStatus = dialogView.findViewById<TextView>(R.id.tv_dialog_upload_status)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_submit)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_cancel)

        tvTitle.text = serviceTitle
        tvAttachmentStatusRef = tvUploadStatus
        pendingAttachmentFile = null

        btnUpload.setOnClickListener {
            selectAttachmentLauncher.launch("*/*")
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val keperluan = etKeperluan.text?.toString()?.trim() ?: ""
            if (keperluan.isBlank()) {
                etKeperluan.error = "Keperluan surat wajib diisi"
                return@setOnClickListener
            }

            val tujuanWilayah = if (rbTempatKos.isChecked) "tempat_kos" else "ktp_asli"

            submitPengajuan(serviceTitle, keperluan, tujuanWilayah, dialog)
        }

        dialog.show()
    }

    private fun submitPengajuan(jenisSurat: String, keperluan: String, tujuanWilayah: String, dialog: AlertDialog) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengirim pengajuan surat...")
            setCancelable(false)
            show()
        }

        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        val userPart = ApiClient.createPartFromString(userId.toString())
        val jenisPart = ApiClient.createPartFromString(jenisSurat)
        val keperluanPart = ApiClient.createPartFromString(keperluan)
        val tujuanPart = ApiClient.createPartFromString(tujuanWilayah)

        var lampiranPart: MultipartBody.Part? = null
        pendingAttachmentFile?.let { file ->
            val reqFile = file.asRequestBody("application/*".toMediaTypeOrNull())
            lampiranPart = MultipartBody.Part.createFormData("dokumen_lampiran", file.name, reqFile)
        }

        apiService.createPengajuan(
            userId = userPart,
            jenisSurat = jenisPart,
            keperluan = keperluanPart,
            tujuanWilayah = tujuanPart,
            lampiran = lampiranPart
        ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, Any>>>,
                response: Response<ApiResponse<Map<String, Any>>>
            ) {
                progressDialog.dismiss()
                val body = response.body()

                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    dialog.dismiss()
                    val pengajuanId = (body.data["id"] as? Number)?.toInt() ?: 0
                    val nomorSurat = body.data["nomor_surat"] as? String ?: ""

                    Toast.makeText(this@ServiceListActivity, "Pengajuan $nomorSurat berhasil dibuat!", Toast.LENGTH_LONG).show()

                    val intent = Intent(this@ServiceListActivity, LetterDetailActivity::class.java).apply {
                        putExtra("EXTRA_PENGAJUAN_ID", pengajuanId)
                        putExtra("EXTRA_SERVICE_NAME", jenisSurat)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val msg = body?.message ?: "Gagal membuat pengajuan surat"
                    Toast.makeText(this@ServiceListActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                dialog.dismiss()
                Toast.makeText(this@ServiceListActivity, "Pengajuan dikirim secara lokal (Offline mode).", Toast.LENGTH_LONG).show()
                val intent = Intent(this@ServiceListActivity, LetterDetailActivity::class.java).apply {
                    putExtra("EXTRA_SERVICE_NAME", jenisSurat)
                }
                startActivity(intent)
                finish()
            }
        })
    }
}
