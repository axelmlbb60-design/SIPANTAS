package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ServiceListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_list)

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
            val intent = Intent(this, LetterDetailActivity::class.java)
            intent.putExtra("EXTRA_SERVICE_NAME", serviceTitle)
            startActivity(intent)
        }

        findViewById<View>(R.id.item_domisili).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_pengantar_rtrw).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sktm).setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sku).setOnClickListener(onSelectService)
    }
}
