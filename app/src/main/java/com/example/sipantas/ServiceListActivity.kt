package com.example.sipantas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sipantas.data.ApiResponse
import com.example.sipantas.data.PengajuanItem
import com.example.sipantas.data.SessionManager
import com.example.sipantas.network.ApiClient
import com.example.sipantas.ui.BottomNavHelper
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ServiceListActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SubmissionAdapter
    private val submissionList = mutableListOf<PengajuanItem>()

    private lateinit var tvHeaderTitle: TextView
    private lateinit var bannerOfficer: LinearLayout
    private lateinit var tvOfficerBannerTitle: TextView
    private lateinit var tvOfficerBannerDesc: TextView
    private lateinit var containerWargaActions: LinearLayout
    private lateinit var btnToggleApplyNew: MaterialButton
    private lateinit var containerServiceCatalog: LinearLayout
    private lateinit var tvSubmissionsTitle: TextView
    private lateinit var containerEmpty: LinearLayout
    private lateinit var tvEmptyMsg: TextView
    private lateinit var rvSubmissions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_list)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.service_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupBottomNav()
        setupRoleUi()
        setupServiceCatalog()
    }

    override fun onResume() {
        super.onResume()
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.TAB_PENGAJUAN)
        loadSubmissions()
    }

    private fun initViews() {
        tvHeaderTitle = findViewById(R.id.tv_service_header_title)
        bannerOfficer = findViewById(R.id.banner_officer)
        tvOfficerBannerTitle = findViewById(R.id.tv_officer_banner_title)
        tvOfficerBannerDesc = findViewById(R.id.tv_officer_banner_desc)
        containerWargaActions = findViewById(R.id.container_warga_actions)
        btnToggleApplyNew = findViewById(R.id.btn_toggle_apply_new)
        containerServiceCatalog = findViewById(R.id.container_service_catalog)
        tvSubmissionsTitle = findViewById(R.id.tv_submissions_title)
        containerEmpty = findViewById(R.id.container_empty_submissions)
        tvEmptyMsg = findViewById(R.id.tv_empty_submissions_msg)
        rvSubmissions = findViewById(R.id.rv_submissions_list)

        rvSubmissions.layoutManager = LinearLayoutManager(this)
        adapter = SubmissionAdapter(submissionList) { item ->
            val intent = Intent(this, LetterDetailActivity::class.java).apply {
                putExtra("EXTRA_PENGAJUAN_ID", item.id)
                putExtra("EXTRA_SERVICE_NAME", item.jenisSurat)
            }
            startActivity(intent)
        }
        rvSubmissions.adapter = adapter
    }

    private fun setupBottomNav() {
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.TAB_PENGAJUAN)
    }

    private fun setupRoleUi() {
        val role = sessionManager.getRole()

        when (role) {
            "rtrw" -> {
                tvHeaderTitle.text = "Verifikasi Surat (RT/RW)"
                bannerOfficer.visibility = View.VISIBLE
                tvOfficerBannerTitle.text = "Verifikasi Surat Warga RT/RW"
                tvOfficerBannerDesc.text = "Berikut daftar permohonan surat warga yang membutuhkan pemeriksaan data dan tanda tangan digital (TTD) Anda."
                // RT/RW tidak boleh melakukan layanan surat (hanya memeriksa/menandatangani)
                containerWargaActions.visibility = View.GONE
                tvSubmissionsTitle.text = "Surat Masuk Menunggu Tindak Lanjut RT"
                tvEmptyMsg.text = "Tidak ada permohonan surat warga yang menunggu tanda tangan saat ini."
            }
            "kecamatan" -> {
                tvHeaderTitle.text = "Pengesahan Surat Kelurahan"
                bannerOfficer.visibility = View.VISIBLE
                tvOfficerBannerTitle.text = "Pengesahan Surat Kelurahan / Kecamatan"
                tvOfficerBannerDesc.text = "Berikut daftar surat warga yang telah ditandatangani RT dan siap disahkan dengan QR Code resmi."
                // Kelurahan tidak boleh membuat permohonan surat (hanya mengesahkan)
                containerWargaActions.visibility = View.GONE
                tvSubmissionsTitle.text = "Surat Masuk Menunggu Pengesahan Kelurahan"
                tvEmptyMsg.text = "Tidak ada surat yang menunggu pengesahan saat ini."
            }
            else -> { // warga
                tvHeaderTitle.text = "Pengajuan Surat Saya"
                bannerOfficer.visibility = View.GONE
                containerWargaActions.visibility = View.VISIBLE
                tvSubmissionsTitle.text = "Riwayat & Status Surat Anda"
                tvEmptyMsg.text = "Anda belum memiliki pengajuan surat. Klik tombol di atas untuk membuat permohonan baru."

                btnToggleApplyNew.setOnClickListener {
                    if (containerServiceCatalog.visibility == View.VISIBLE) {
                        containerServiceCatalog.visibility = View.GONE
                        btnToggleApplyNew.text = "+ Ajukan Permohonan Surat Baru"
                    } else {
                        containerServiceCatalog.visibility = View.VISIBLE
                        btnToggleApplyNew.text = "▲ Sembunyikan Pilihan Surat"
                    }
                }
            }
        }
    }

    private fun setupServiceCatalog() {
        val onSelectService = View.OnClickListener { v ->
            val serviceTitle = when (v.id) {
                R.id.item_domisili -> "Surat Keterangan Domisili"
                R.id.item_pengantar_rtrw -> "Surat Pengantar RT / RW"
                R.id.item_sktm -> "Surat Keterangan Tidak Mampu"
                R.id.item_sku -> "Surat Keterangan Usaha"
                else -> "Surat Keterangan Domisili"
            }
            val intent = Intent(this, ApplyLetterActivity::class.java).apply {
                putExtra("EXTRA_LETTER_TYPE", serviceTitle)
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.item_domisili)?.setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_pengantar_rtrw)?.setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sktm)?.setOnClickListener(onSelectService)
        findViewById<View>(R.id.item_sku)?.setOnClickListener(onSelectService)
    }

    private fun loadSubmissions() {
        val role = sessionManager.getRole()
        val userId = sessionManager.getUserId()
        val apiService = ApiClient.getService(this)

        val call: Call<ApiResponse<List<PengajuanItem>>> = when (role) {
            "rtrw" -> apiService.getPengajuanList(role = "rtrw")
            "kecamatan" -> apiService.getPengajuanList(role = "kecamatan", status = "menunggu_kecamatan")
            else -> apiService.getPengajuanList(userId = userId)
        }

        call.enqueue(object : Callback<ApiResponse<List<PengajuanItem>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<PengajuanItem>>>,
                response: Response<ApiResponse<List<PengajuanItem>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.data ?: emptyList()
                    submissionList.clear()
                    submissionList.addAll(list)
                    adapter.updateData(submissionList)

                    if (submissionList.isEmpty()) {
                        containerEmpty.visibility = View.VISIBLE
                        rvSubmissions.visibility = View.GONE
                    } else {
                        containerEmpty.visibility = View.GONE
                        rvSubmissions.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@ServiceListActivity, "Gagal memuat surat: ${response.body()?.message ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PengajuanItem>>>, t: Throwable) {
                Toast.makeText(this@ServiceListActivity, "Koneksi gagal: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
