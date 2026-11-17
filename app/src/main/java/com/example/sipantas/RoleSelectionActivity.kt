package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_selection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.role_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.bottom_login_link).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val onRoleClickListener = View.OnClickListener { v ->
            val roleKey = when (v.id) {
                R.id.role_warga -> "warga"
                R.id.role_rt_rw -> "rtrw"
                R.id.role_kelurahan -> "kelurahan"
                R.id.role_admin -> "admin"
                else -> "warga"
            }
            val roleTitle = when (roleKey) {
                "warga" -> "Warga"
                "rtrw" -> "RT / RW"
                "kelurahan" -> "Petugas Kelurahan"
                "admin" -> "Admin Sistem"
                else -> "Warga"
            }

            if (roleKey == "warga") {
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("EXTRA_SELECTED_ROLE", roleTitle)
                startActivity(intent)
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Akses $roleTitle")
                    .setMessage("Pilih tindakan untuk akun $roleTitle:")
                    .setPositiveButton("Masuk") { _, _ ->
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.putExtra("EXTRA_SELECTED_ROLE", roleTitle)
                        startActivity(intent)
                    }
                    .setNeutralButton("Aktivasi Akun Baru") { _, _ ->
                        val intent = Intent(this, RegisterOfficerActivity::class.java)
                        intent.putExtra("EXTRA_ROLE", roleKey)
                        startActivity(intent)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        findViewById<View>(R.id.role_warga).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_rt_rw).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_kelurahan).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_admin).setOnClickListener(onRoleClickListener)
    }
}
