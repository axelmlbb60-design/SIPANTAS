package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private var currentRole: String = "Warga"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentRole = intent.getStringExtra("EXTRA_SELECTED_ROLE") ?: "Warga"
        val tvRoleChip = findViewById<TextView>(R.id.tv_role_chip)
        tvRoleChip.text = "Masuk sebagai $currentRole  ·  Ganti peran"

        tvRoleChip.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        val navigateBack = {
            if (isTaskRoot) {
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            navigateBack()
        }

        onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }

        findViewById<TextView>(R.id.tv_register_link).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            Toast.makeText(this, "Petunjuk pemulihan akan dikirim ke nomor/email Anda.", Toast.LENGTH_SHORT).show()
        }

        val etIdentifier = findViewById<TextInputEditText>(R.id.et_identifier)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)

        findViewById<MaterialButton>(R.id.btn_login).setOnClickListener {
            val identifier = etIdentifier.text?.toString()?.trim()
            val password = etPassword.text?.toString()?.trim()

            // For quick demo/preview convenience:
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("EXTRA_USER_NAME", if (!identifier.isNullOrBlank()) identifier else "Budi Santoso")
            intent.putExtra("EXTRA_ROLE", currentRole)
            startActivity(intent)
            finishAffinity()
        }
    }
}
