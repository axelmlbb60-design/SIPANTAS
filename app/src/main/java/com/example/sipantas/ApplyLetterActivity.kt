package com.example.sipantas

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ApplyLetterActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var letterType: String = "Surat Keterangan Domisili"
    private var currentStep: Int = 1

    private var attachmentFile: File? = null
    private var attachmentFileName: String = ""

    // Views
    private lateinit var layoutStep1: View
    private lateinit var layoutStep2: View
    private lateinit var layoutStep3: View

    private lateinit var stepInd1: TextView
    private lateinit var stepInd2: TextView
    private lateinit var stepInd3: TextView
    private lateinit var stepLabel1: TextView
    private lateinit var stepLabel2: TextView
    private lateinit var stepLabel3: TextView
    private lateinit var stepLine1: View
    private lateinit var stepLine2: View

    private lateinit var etPurpose: TextInputEditText
    private lateinit var rgDestination: RadioGroup
    private lateinit var rbKtpAsli: RadioButton

    private lateinit var containerFileSelected: View
    private lateinit var tvSelectedFilename: TextView
    private lateinit var tvSelectedFilesize: TextView

    private lateinit var tvReviewService: TextView
    private lateinit var tvReviewApplicant: TextView
    private lateinit var tvReviewDestination: TextView
    private lateinit var tvReviewPurpose: TextView
    private lateinit var tvReviewAttachment: TextView
    private lateinit var cbAgreement: CheckBox

    private lateinit var btnPrimaryAction: MaterialButton
    private lateinit var btnSecondaryAction: MaterialButton

    private val selectAttachmentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleSelectedAttachment(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_apply_letter)

        sessionManager = SessionManager(this)
        letterType = intent.getStringExtra("EXTRA_LETTER_TYPE") ?: "Surat Keterangan Domisili"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.apply_letter_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        updateStepUI(1)
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            handleBackNavigation()
        }

        findViewById<TextView>(R.id.tv_letter_type_title).text = letterType
        findViewById<TextView>(R.id.tv_applicant_name).text = "Nama: ${sessionManager.getNama()}"
        findViewById<TextView>(R.id.tv_applicant_nik).text = "NIK: ${sessionManager.getNik()}"
        findViewById<TextView>(R.id.tv_applicant_address).text = "Domisili: ${sessionManager.getAlamatDomisili().ifBlank { "RT " + sessionManager.getRt() + " / RW " + sessionManager.getRw() }}"

        layoutStep1 = findViewById(R.id.layout_step_1)
        layoutStep2 = findViewById(R.id.layout_step_2)
        layoutStep3 = findViewById(R.id.layout_step_3)

        stepInd1 = findViewById(R.id.step_indicator_1)
        stepInd2 = findViewById(R.id.step_indicator_2)
        stepInd3 = findViewById(R.id.step_indicator_3)
        stepLabel1 = findViewById(R.id.tv_step_1_label)
        stepLabel2 = findViewById(R.id.tv_step_2_label)
        stepLabel3 = findViewById(R.id.tv_step_3_label)
        stepLine1 = findViewById(R.id.step_line_1)
        stepLine2 = findViewById(R.id.step_line_2)

        etPurpose = findViewById(R.id.et_purpose)
        rgDestination = findViewById(R.id.rg_destination)
        rbKtpAsli = findViewById(R.id.rb_ktp_asli)

        containerFileSelected = findViewById(R.id.container_file_selected)
        tvSelectedFilename = findViewById(R.id.tv_selected_filename)
        tvSelectedFilesize = findViewById(R.id.tv_selected_filesize)

        tvReviewService = findViewById(R.id.tv_review_service)
        tvReviewApplicant = findViewById(R.id.tv_review_applicant)
        tvReviewDestination = findViewById(R.id.tv_review_destination)
        tvReviewPurpose = findViewById(R.id.tv_review_purpose)
        tvReviewAttachment = findViewById(R.id.tv_review_attachment)
        cbAgreement = findViewById(R.id.cb_agreement)

        btnPrimaryAction = findViewById(R.id.btn_primary_action)
        btnSecondaryAction = findViewById(R.id.btn_secondary_action)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_select_attachment).setOnClickListener {
            selectAttachmentLauncher.launch("*/*")
        }

        findViewById<View>(R.id.btn_change_attachment).setOnClickListener {
            selectAttachmentLauncher.launch("*/*")
        }

        btnPrimaryAction.setOnClickListener {
            when (currentStep) {
                1 -> {
                    val purpose = etPurpose.text.toString().trim()
                    if (purpose.isEmpty()) {
                        Toast.makeText(this, "Harap isi keperluan pengajuan surat!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    updateStepUI(2)
                }
                2 -> {
                    updateStepUI(3)
                }
                3 -> {
                    if (!cbAgreement.isChecked) {
                        Toast.makeText(this, "Harap centang persetujuan kebenaran data!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    submitApplication()
                }
            }
        }

        btnSecondaryAction.setOnClickListener {
            handleBackNavigation()
        }
    }

    private fun updateStepUI(step: Int) {
        currentStep = step

        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_primary_button)
        val inactiveBg = ContextCompat.getDrawable(this, R.drawable.bg_role_chip)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val blueColor = ContextCompat.getColor(this, R.color.brand_primary)
        val mutedColor = ContextCompat.getColor(this, R.color.text_secondary)
        val activeLine = ContextCompat.getColor(this, R.color.brand_primary)
        val inactiveLine = ContextCompat.getColor(this, R.color.card_border)

        when (step) {
            1 -> {
                layoutStep1.visibility = View.VISIBLE
                layoutStep2.visibility = View.GONE
                layoutStep3.visibility = View.GONE

                stepInd1.background = activeBg
                stepInd1.setTextColor(whiteColor)
                stepLabel1.setTextColor(blueColor)

                stepInd2.background = inactiveBg
                stepInd2.setTextColor(blueColor)
                stepLabel2.setTextColor(mutedColor)

                stepInd3.background = inactiveBg
                stepInd3.setTextColor(blueColor)
                stepLabel3.setTextColor(mutedColor)

                stepLine1.setBackgroundColor(inactiveLine)
                stepLine2.setBackgroundColor(inactiveLine)

                btnPrimaryAction.text = "Lanjut ke berkas persyaratan"
                btnSecondaryAction.visibility = View.GONE
            }
            2 -> {
                layoutStep1.visibility = View.GONE
                layoutStep2.visibility = View.VISIBLE
                layoutStep3.visibility = View.GONE

                stepInd1.background = activeBg
                stepInd1.setTextColor(whiteColor)
                stepLabel1.setTextColor(blueColor)

                stepInd2.background = activeBg
                stepInd2.setTextColor(whiteColor)
                stepLabel2.setTextColor(blueColor)

                stepInd3.background = inactiveBg
                stepInd3.setTextColor(blueColor)
                stepLabel3.setTextColor(mutedColor)

                stepLine1.setBackgroundColor(activeLine)
                stepLine2.setBackgroundColor(inactiveLine)

                btnPrimaryAction.text = "Lanjut ke tinjauan pengajuan"
                btnSecondaryAction.visibility = View.VISIBLE
                btnSecondaryAction.text = "Kembali ke data pemohon"
            }
            3 -> {
                layoutStep1.visibility = View.GONE
                layoutStep2.visibility = View.GONE
                layoutStep3.visibility = View.VISIBLE

                stepInd1.background = activeBg
                stepInd1.setTextColor(whiteColor)
                stepLabel1.setTextColor(blueColor)

                stepInd2.background = activeBg
                stepInd2.setTextColor(whiteColor)
                stepLabel2.setTextColor(blueColor)

                stepInd3.background = activeBg
                stepInd3.setTextColor(whiteColor)
                stepLabel3.setTextColor(blueColor)

                stepLine1.setBackgroundColor(activeLine)
                stepLine2.setBackgroundColor(activeLine)

                // Populate Summary
                val destinationText = if (rbKtpAsli.isChecked) "Alamat KTP Tetap" else "Tempat Tinggal Sementara"
                tvReviewService.text = "Layanan: $letterType"
                tvReviewApplicant.text = "Pemohon: ${sessionManager.getNama()} (NIK: ${sessionManager.getNik()})"
                tvReviewDestination.text = "Tujuan Wilayah: $destinationText"
                tvReviewPurpose.text = "Keperluan: ${etPurpose.text.toString().trim()}"
                tvReviewAttachment.text = if (attachmentFile != null) {
                    "Lampiran: $attachmentFileName (Siap)"
                } else {
                    "Lampiran: Menggunakan data dokumen profil terverifikasi"
                }

                btnPrimaryAction.text = "Kirim pengajuan surat"
                btnSecondaryAction.visibility = View.VISIBLE
                btnSecondaryAction.text = "Ubah data pengajuan"
            }
        }
    }

    private fun handleBackNavigation() {
        when (currentStep) {
            1 -> finish()
            2 -> updateStepUI(1)
            3 -> updateStepUI(2)
        }
    }

    private fun handleSelectedAttachment(uri: Uri) {
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

        if (fileSize > 2 * 1024 * 1024) {
            Toast.makeText(this, "Ukuran file melebihi 2MB! Pilih file lain.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "doc_" + System.currentTimeMillis() + "_" + fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            attachmentFile = file
            attachmentFileName = fileName

            containerFileSelected.visibility = View.VISIBLE
            tvSelectedFilename.text = fileName
            val sizeKb = fileSize / 1024
            tvSelectedFilesize.text = "✓ Berkas terverifikasi ($sizeKb KB)"
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitApplication() {
        val progress = ProgressDialog(this).apply {
            setMessage("Mengirim pengajuan surat ke Ketua RT...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        val userIdBody = sessionManager.getUserId().toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val jenisSuratBody = letterType.toRequestBody("text/plain".toMediaTypeOrNull())
        val keperluanBody = etPurpose.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
        val tujuanWilayahStr = if (rbKtpAsli.isChecked) "ktp_asli" else "tempat_kos"
        val tujuanBody = tujuanWilayahStr.toRequestBody("text/plain".toMediaTypeOrNull())

        var partLampiran: MultipartBody.Part? = null
        attachmentFile?.let { file ->
            val reqBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            partLampiran = MultipartBody.Part.createFormData("dokumen_lampiran", file.name, reqBody)
        }

        apiService.createPengajuan(
            userId = userIdBody,
            jenisSurat = jenisSuratBody,
            keperluan = keperluanBody,
            tujuanWilayah = tujuanBody,
            lampiran = partLampiran
        ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, Any>>>,
                response: Response<ApiResponse<Map<String, Any>>>
            ) {
                progress.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    AlertDialog.Builder(this@ApplyLetterActivity)
                        .setTitle("Pengajuan Berhasil Dikirim!")
                        .setMessage("Permohonan Anda telah tercatat dalam sistem SIPANTAS dan langsung diteruskan ke Ketua RT untuk verifikasi tanda tangan digital.")
                        .setPositiveButton("Lihat Status Surat") { _, _ ->
                            val intent = Intent(this@ApplyLetterActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    val errMsg = body?.message ?: "Gagal mengirimkan pengajuan surat."
                    AlertDialog.Builder(this@ApplyLetterActivity)
                        .setTitle("Pengajuan Gagal")
                        .setMessage(errMsg)
                        .setPositiveButton("Coba Lagi", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progress.dismiss()
                AlertDialog.Builder(this@ApplyLetterActivity)
                    .setTitle("Koneksi Terputus")
                    .setMessage("Tidak dapat menghubungi server SIPANTAS.\n\nDetail: ${t.localizedMessage}")
                    .setPositiveButton("Tutup", null)
                    .show()
            }
        })
    }
}
