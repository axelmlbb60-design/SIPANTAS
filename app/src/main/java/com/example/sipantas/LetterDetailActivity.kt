package com.example.sipantas

import android.app.ProgressDialog
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
import com.example.sipantas.data.AuditLogItem
import com.example.sipantas.data.PengajuanItem
import com.example.sipantas.data.QrVerificationData
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.example.sipantas.ui.SignatureView
import com.example.sipantas.util.QrHelper
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LetterDetailActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var pengajuanId: Int = 0
    private var currentPengajuan: PengajuanItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_letter_detail)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        pengajuanId = intent.getIntExtra("EXTRA_PENGAJUAN_ID", 0)
        val defaultServiceName = intent.getStringExtra("EXTRA_SERVICE_NAME") ?: "Surat Keterangan Domisili"
        findViewById<TextView>(R.id.tv_letter_title).text = defaultServiceName

        findViewById<MaterialButton>(R.id.btn_contact_support).setOnClickListener {
            val petugasName = currentPengajuan?.namaLengkap ?: "Ketua RT / RW"
            Toast.makeText(this, "Menghubungi petugas wilayah: $petugasName...", Toast.LENGTH_SHORT).show()
        }

        if (pengajuanId > 0) {
            loadPengajuanDetail()
        } else {
            // Demo default preview
            setupDemoDetail(defaultServiceName)
        }
    }

    private fun loadPengajuanDetail() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memuat data surat...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.getPengajuanDetail(pengajuanId).enqueue(object : Callback<ApiResponse<PengajuanItem>> {
            override fun onResponse(call: Call<ApiResponse<PengajuanItem>>, response: Response<ApiResponse<PengajuanItem>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    currentPengajuan = body.data
                    bindPengajuanData(body.data)
                } else {
                    Toast.makeText(this@LetterDetailActivity, "Gagal memuat detail pengajuan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PengajuanItem>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@LetterDetailActivity, "Koneksi offline: Menampilkan data tersimpan", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindPengajuanData(item: PengajuanItem) {
        findViewById<TextView>(R.id.tv_letter_title).text = item.jenisSurat
        findViewById<TextView>(R.id.tv_detail_letter_id).text = "Nomor Surat: ${item.nomorSurat}"
        findViewById<TextView>(R.id.tv_detail_applicant_name).text = "Pemohon: ${item.namaLengkap ?: sessionManager.getNama()}"
        findViewById<TextView>(R.id.tv_detail_applicant_nik).text = "NIK: ${item.nik ?: sessionManager.getNik()}"
        findViewById<TextView>(R.id.tv_detail_purpose).text = "Keperluan: ${item.keperluan}"
        findViewById<TextView>(R.id.tv_detail_destination).text = "Tujuan Wilayah: " + if (item.tujuanWilayah == "tempat_kos") "Wilayah Tempat Kos / Sementara" else "Wilayah Sesuai KTP Asli"
        findViewById<TextView>(R.id.tv_detail_date).text = "Diajukan: ${item.createdAt.take(19)}"

        // Status Badge
        val tvStatus = findViewById<TextView>(R.id.tv_detail_status_badge)
        when (item.status) {
            "menunggu_rt" -> {
                tvStatus.text = "Menunggu RT"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending))
            }
            "menunggu_kecamatan" -> {
                tvStatus.text = "Menunggu Kecamatan"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_info))
            }
            "selesai" -> {
                tvStatus.text = "Selesai (Resmi)"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            }
            "ditolak_rt" -> {
                tvStatus.text = "Ditolak RT"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
            }
            "ditolak_kecamatan" -> {
                tvStatus.text = "Ditolak Kecamatan"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_danger))
            }
            else -> {
                tvStatus.text = item.status
            }
        }

        // Catatan Penolakan
        val cardRejection = findViewById<View>(R.id.card_rejection_notice)
        val tvRejectionReason = findViewById<TextView>(R.id.tv_rejection_reason)
        if (!item.catatanPenolakan.isNullOrBlank()) {
            cardRejection.visibility = View.VISIBLE
            tvRejectionReason.text = item.catatanPenolakan
        } else {
            cardRejection.visibility = View.GONE
        }

        // QR Code Section (Fitur Keaslian Surat)
        val cardQr = findViewById<View>(R.id.card_qr_verification)
        val ivQr = findViewById<ImageView>(R.id.iv_qr_code)
        val tvQrToken = findViewById<TextView>(R.id.tv_qr_token)
        val btnVerify = findViewById<MaterialButton>(R.id.btn_verify_authenticity)

        if (item.status == "selesai" && !item.qrCodeToken.isNullOrBlank()) {
            cardQr.visibility = View.VISIBLE
            tvQrToken.text = item.qrCodeToken
            val bitmap = QrHelper.generateQrBitmap(item.qrCodeToken)
            if (bitmap != null) {
                ivQr.setImageBitmap(bitmap)
            }

            btnVerify.setOnClickListener {
                verifyQrOnline(item.qrCodeToken)
            }
        } else {
            cardQr.visibility = View.GONE
        }

        // Role Action Panels
        val currentRole = sessionManager.getRole()
        val layoutRtActions = findViewById<View>(R.id.layout_rt_actions)
        val layoutKecActions = findViewById<View>(R.id.layout_kecamatan_actions)
        val btnCancel = findViewById<View>(R.id.btn_cancel_submission)

        layoutRtActions.visibility = View.GONE
        layoutKecActions.visibility = View.GONE
        btnCancel.visibility = View.GONE

        if (currentRole == "rtrw" && item.status == "menunggu_rt") {
            layoutRtActions.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btn_rt_approve_signature).setOnClickListener {
                showDigitalSignatureCanvasDialog()
            }
            findViewById<MaterialButton>(R.id.btn_rt_reject).setOnClickListener {
                showRejectionDialog(isRt = true)
            }
        } else if ((currentRole == "kecamatan" || currentRole == "admin") && item.status == "menunggu_kecamatan") {
            layoutKecActions.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btn_kecamatan_approve).setOnClickListener {
                approveByKecamatan()
            }
            findViewById<MaterialButton>(R.id.btn_kecamatan_reject).setOnClickListener {
                showRejectionDialog(isRt = false)
            }
        } else if (currentRole == "warga" && item.status == "menunggu_rt") {
            btnCancel.visibility = View.VISIBLE
            btnCancel.setOnClickListener {
                confirmCancelSubmission()
            }
        }

        // Audit Logs Timeline
        renderAuditLogs(item.auditLogs)
    }

    private fun renderAuditLogs(logs: List<AuditLogItem>?) {
        val container = findViewById<LinearLayout>(R.id.container_audit_logs)
        container.removeAllViews()

        if (logs.isNullOrEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Belum ada riwayat audit tercatat."
                setTextColor(ContextCompat.getColor(this@LetterDetailActivity, R.color.text_muted))
                textSize = 13f
            }
            container.addView(emptyTv)
            return
        }

        val inflater = LayoutInflater.from(this)
        for (log in logs) {
            val itemView = inflater.inflate(R.layout.item_audit_log, container, false)
            val tvIcon = itemView.findViewById<TextView>(R.id.tv_audit_icon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tv_audit_title)
            val tvNotes = itemView.findViewById<TextView>(R.id.tv_audit_notes)
            val tvTime = itemView.findViewById<TextView>(R.id.tv_audit_time)

            when (log.action) {
                "SUBMITTED" -> {
                    tvIcon.text = "✉️"
                    tvTitle.text = "Diajukan oleh ${log.actorName} (Warga)"
                }
                "APPROVED_RT" -> {
                    tvIcon.text = "✍️"
                    tvTitle.text = "Disetujui oleh ${log.actorName} (RT/RW)"
                }
                "REJECTED_RT" -> {
                    tvIcon.text = "❌"
                    tvTitle.text = "Ditolak oleh ${log.actorName} (RT/RW)"
                }
                "APPROVED_KECAMATAN" -> {
                    tvIcon.text = "🏛️"
                    tvTitle.text = "Disahkan oleh ${log.actorName} (Kecamatan)"
                }
                "REJECTED_KECAMATAN" -> {
                    tvIcon.text = "❌"
                    tvTitle.text = "Ditolak oleh ${log.actorName} (Kecamatan)"
                }
                else -> {
                    tvIcon.text = "✓"
                    tvTitle.text = "${log.action} - ${log.actorName}"
                }
            }

            tvNotes.text = log.notes ?: "-"
            tvTime.text = log.createdAt

            container.addView(itemView)
        }
    }

    private fun showDigitalSignatureCanvasDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_digital_signature, null)
        val signatureView = dialogView.findViewById<SignatureView>(R.id.signature_view)
        val btnClear = dialogView.findViewById<MaterialButton>(R.id.btn_clear_signature)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_signature)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save_signature)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClear.setOnClickListener {
            signatureView.clear()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            if (!signatureView.isSigned()) {
                Toast.makeText(this, "Silakan bubuhkan tanda tangan kanvas terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val signatureBase64 = signatureView.getSignatureBase64()
            dialog.dismiss()
            submitRtApproval(signatureBase64)
        }

        dialog.show()
    }

    private fun submitRtApproval(signatureBase64: String?) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan tanda tangan & persetujuan RT/RW...")
            setCancelable(false)
            show()
        }

        val actorName = sessionManager.getNama()
        val apiService = ApiClient.getService(this)

        apiService.actionRt(
            id = pengajuanId,
            action = "setujui",
            nama = actorName,
            alasan = null,
            signature = signatureBase64
        ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@LetterDetailActivity, "Surat berhasil disetujui dan diteruskan ke Kecamatan!", Toast.LENGTH_LONG).show()
                    loadPengajuanDetail()
                } else {
                    Toast.makeText(this@LetterDetailActivity, body?.message ?: "Gagal menyetujui surat", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@LetterDetailActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun approveByKecamatan() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Mengesahkan surat dan menerbitkan QR Code resmi...")
            setCancelable(false)
            show()
        }

        val actorName = sessionManager.getNama()
        val apiService = ApiClient.getService(this)

        apiService.actionKecamatan(
            id = pengajuanId,
            action = "setujui",
            nama = actorName,
            alasan = null
        ).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@LetterDetailActivity, "Surat resmi berhasil diterbitkan dengan QR Code!", Toast.LENGTH_LONG).show()
                    loadPengajuanDetail()
                } else {
                    Toast.makeText(this@LetterDetailActivity, body?.message ?: "Gagal mengesahkan surat", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@LetterDetailActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showRejectionDialog(isRt: Boolean) {
        val input = EditText(this).apply {
            hint = "Tuliskan alasan penolakan secara jelas (Wajib)"
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Tolak Pengajuan Surat")
            .setMessage("Masukkan alasan penolakan wajib agar pemohon dapat memperbaiki persyaratannya:")
            .setView(input)
            .setPositiveButton("Tolak Surat") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) {
                    Toast.makeText(this, "Alasan penolakan WAJIB diisi!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                submitRejection(isRt, reason)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun submitRejection(isRt: Boolean, reason: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyimpan penolakan...")
            setCancelable(false)
            show()
        }

        val actorName = sessionManager.getNama()
        val apiService = ApiClient.getService(this)

        val call = if (isRt) {
            apiService.actionRt(pengajuanId, "tolak", actorName, reason, null)
        } else {
            apiService.actionKecamatan(pengajuanId, "tolak", actorName, reason)
        }

        call.enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@LetterDetailActivity, "Pengajuan surat ditolak dengan catatan tersimpan.", Toast.LENGTH_LONG).show()
                    loadPengajuanDetail()
                } else {
                    Toast.makeText(this@LetterDetailActivity, body?.message ?: "Gagal menolak surat", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@LetterDetailActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmCancelSubmission() {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Pengajuan")
            .setMessage("Apakah Anda yakin ingin membatalkan pengajuan surat ini? Pengajuan akan dihapus dari sistem.")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                val apiService = ApiClient.getService(this)
                apiService.deletePengajuan(pengajuanId, sessionManager.getUserId()).enqueue(object : Callback<ApiResponse<Void>> {
                    override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                        Toast.makeText(this@LetterDetailActivity, "Pengajuan berhasil dibatalkan.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                        Toast.makeText(this@LetterDetailActivity, "Gagal membatalkan pengajuan.", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Kembali", null)
            .show()
    }

    private fun verifyQrOnline(token: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Memeriksa keaslian kode QR di server...")
            setCancelable(false)
            show()
        }

        val apiService = ApiClient.getService(this)
        apiService.verifyQr(token).enqueue(object : Callback<ApiResponse<QrVerificationData>> {
            override fun onResponse(call: Call<ApiResponse<QrVerificationData>>, response: Response<ApiResponse<QrVerificationData>>) {
                progressDialog.dismiss()
                val body = response.body()
                if (response.isSuccessful && body != null && body.success && body.data != null) {
                    val d = body.data
                    AlertDialog.Builder(this@LetterDetailActivity)
                        .setTitle("✅ Dokumen TERVERIFIKASI & ASLI")
                        .setMessage(
                            "Status: Dokumen Resmi Terdaftar\n\n" +
                            "Nomor: ${d.nomorSurat}\n" +
                            "Jenis: ${d.jenisSurat}\n" +
                            "Pemohon: ${d.namaPemohon} (${d.nikMasked})\n" +
                            "Wilayah: ${d.wilayah}\n" +
                            "Tanggal Pengesahan: ${d.tanggalPengesahan}\n" +
                            "Token: ${d.qrToken}"
                        )
                        .setPositiveButton("Tutup", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@LetterDetailActivity)
                        .setTitle("❌ Verifikasi Gagal")
                        .setMessage(body?.message ?: "Kode QR tidak dikenali sebagai surat resmi SIPANTAS.")
                        .setPositiveButton("Tutup", null)
                        .show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<QrVerificationData>>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@LetterDetailActivity, "Gagal memverifikasi token secara online", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupDemoDetail(serviceName: String) {
        findViewById<TextView>(R.id.tv_letter_title).text = serviceName
        findViewById<TextView>(R.id.tv_detail_letter_id).text = "Nomor Surat: SKD-0311-007"
        findViewById<TextView>(R.id.tv_detail_applicant_name).text = "Pemohon: Budi Santoso"
        findViewById<TextView>(R.id.tv_detail_applicant_nik).text = "NIK: 7371010101010001"
        findViewById<TextView>(R.id.tv_detail_purpose).text = "Keperluan: Melamar pekerjaan"
        findViewById<TextView>(R.id.tv_detail_destination).text = "Tujuan Wilayah: Sesuai KTP Asli"
        findViewById<TextView>(R.id.tv_detail_date).text = "Diajukan: 17 Nov 2026"
    }
}
