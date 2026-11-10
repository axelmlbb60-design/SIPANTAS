package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val userName = intent.getStringExtra("EXTRA_USER_NAME") ?: "Budi Santoso"
        val userRole = intent.getStringExtra("EXTRA_ROLE") ?: "Warga"

        val tvGreeting = findViewById<TextView>(R.id.tv_greeting)
        tvGreeting.text = "Halo, $userName"

        val tvSubBadge = findViewById<TextView>(R.id.tv_sub_badge)
        tvSubBadge.text = "$userRole · RT 03 / RW 02 · Kelurahan Contoh"

        findViewById<View>(R.id.btn_header_profile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("EXTRA_USER_NAME", userName)
            intent.putExtra("EXTRA_ROLE", userRole)
            startActivity(intent)
        }

        // Hero button -> ServiceList
        findViewById<MaterialButton>(R.id.btn_hero_apply).setOnClickListener {
            startActivity(Intent(this, ServiceListActivity::class.java))
        }

        // Antrean -> QueueActivity
        findViewById<View>(R.id.action_antrean).setOnClickListener {
            startActivity(Intent(this, QueueActivity::class.java))
        }

        // Lapor warga -> ReportActivity
        findViewById<View>(R.id.action_lapor).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        // Click on recent application card -> LetterDetailActivity
        findViewById<View>(R.id.card_active_letter).setOnClickListener {
            val intent = Intent(this, LetterDetailActivity::class.java)
            intent.putExtra("EXTRA_SERVICE_NAME", "Surat Keterangan Domisili")
            startActivity(intent)
        }

        // Bottom Navigation Bar Clicks
        findViewById<View>(R.id.nav_beranda).setOnClickListener {
            // Already on Beranda
        }

        findViewById<View>(R.id.nav_pengajuan).setOnClickListener {
            startActivity(Intent(this, ServiceListActivity::class.java))
        }

        findViewById<View>(R.id.nav_kabar).setOnClickListener {
            Toast.makeText(this, "Kabar Warga: Semua pengumuman kelurahan up to date.", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.nav_bantuan).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("EXTRA_USER_NAME", userName)
            intent.putExtra("EXTRA_ROLE", userRole)
            startActivity(intent)
        }
    }
}
