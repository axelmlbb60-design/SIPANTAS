package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userName = intent.getStringExtra("EXTRA_USER_NAME") ?: "Budi Santoso"
        val userRole = intent.getStringExtra("EXTRA_ROLE") ?: "Warga"
        findViewById<TextView>(R.id.tv_profile_name).text = userName

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.menu_documents).setOnClickListener {
            Toast.makeText(this, "Membuka dokumen tersimpan (KTP & KK)", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.menu_security).setOnClickListener {
            Toast.makeText(this, "Pengaturan Keamanan Akun", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.menu_support).setOnClickListener {
            Toast.makeText(this, "Menghubungi Pusat Bantuan SIPANTAS", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.menu_about).setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar dari akun SIPANTAS?")
                .setPositiveButton("Keluar") { _, _ ->
                    Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("EXTRA_SELECTED_ROLE", userRole)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
