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

class LetterDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_letter_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val serviceName = intent.getStringExtra("EXTRA_SERVICE_NAME")
        if (!serviceName.isNullOrBlank()) {
            findViewById<TextView>(R.id.tv_letter_title).text = serviceName
        }

        findViewById<MaterialButton>(R.id.btn_contact_support).setOnClickListener {
            Toast.makeText(this, "Menghubungi petugas RT 03 (Ahmad Fauzi)...", Toast.LENGTH_SHORT).show()
        }
    }
}
