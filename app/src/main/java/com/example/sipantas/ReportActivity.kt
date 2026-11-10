package com.example.sipantas

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.btn_upload_evidence).setOnClickListener {
            findViewById<TextView>(R.id.tv_evidence_status).text = "✓ Foto bukti kendala terlampir (bukti_laporan.jpg)"
            Toast.makeText(this, "Foto kendala berhasil dipilih", Toast.LENGTH_SHORT).show()
        }

        val etTitle = findViewById<TextInputEditText>(R.id.et_report_title)

        findViewById<MaterialButton>(R.id.btn_submit_report).setOnClickListener {
            val title = etTitle.text?.toString()?.trim()
            if (title.isNullOrBlank()) {
                etTitle.error = "Judul laporan wajib diisi"
                return@setOnClickListener
            }
            Toast.makeText(this, "Laporan berhasil dikirim ke RT/RW & Kelurahan!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
