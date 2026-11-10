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
            val roleName = when (v.id) {
                R.id.role_warga -> "Warga"
                R.id.role_rt_rw -> "RT / RW"
                R.id.role_kelurahan -> "Petugas Kelurahan"
                R.id.role_admin -> "Admin Sistem"
                else -> "Warga"
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("EXTRA_SELECTED_ROLE", roleName)
            startActivity(intent)
        }

        findViewById<View>(R.id.role_warga).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_rt_rw).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_kelurahan).setOnClickListener(onRoleClickListener)
        findViewById<View>(R.id.role_admin).setOnClickListener(onRoleClickListener)
    }
}
