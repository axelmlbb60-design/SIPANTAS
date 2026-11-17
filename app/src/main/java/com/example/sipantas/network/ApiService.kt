package com.example.sipantas.network

import com.example.sipantas.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // Auth
    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("nik") nik: String,
        @Field("password") pass: String
    ): Call<ApiResponse<UserModel>>

    @Multipart
    @POST("register.php")
    fun register(
        @Part("nik") nik: RequestBody,
        @Part("nama_lengkap") nama: RequestBody,
        @Part("password") pass: RequestBody,
        @Part("email") email: RequestBody?,
        @Part("no_hp") noHp: RequestBody?,
        @Part("role") role: RequestBody,
        @Part("tipe_penduduk") tipePenduduk: RequestBody,
        @Part("alamat_ktp") alamatKtp: RequestBody,
        @Part("alamat_domisili") alamatDomisili: RequestBody,
        @Part("kecamatan_id") kecId: RequestBody?,
        @Part("kelurahan_id") kelId: RequestBody?,
        @Part("rw_id") rwId: RequestBody?,
        @Part("rt_id") rtId: RequestBody?,
        @Part fotoKtp: MultipartBody.Part?
    ): Call<ApiResponse<UserModel>>

    // Wilayah
    @GET("get_wilayah.php")
    fun getWilayah(
        @Query("type") type: String,
        @Query("parent_id") parentId: Int? = null
    ): Call<ApiResponse<List<WilayahItem>>>

    // Pengajuan Surat
    @Multipart
    @POST("pengajuan_create.php")
    fun createPengajuan(
        @Part("user_id") userId: RequestBody,
        @Part("jenis_surat") jenisSurat: RequestBody,
        @Part("keperluan") keperluan: RequestBody,
        @Part("tujuan_wilayah") tujuanWilayah: RequestBody,
        @Part lampiran: MultipartBody.Part?
    ): Call<ApiResponse<Map<String, Any>>>

    @GET("pengajuan_list.php")
    fun getPengajuanList(
        @Query("user_id") userId: Int? = null,
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("rt_id") rtId: Int? = null,
        @Query("search") search: String? = null
    ): Call<ApiResponse<List<PengajuanItem>>>

    @GET("pengajuan_detail.php")
    fun getPengajuanDetail(
        @Query("id") id: Int
    ): Call<ApiResponse<PengajuanItem>>

    @FormUrlEncoded
    @POST("pengajuan_action_rt.php")
    fun actionRt(
        @Field("pengajuan_id") id: Int,
        @Field("action") action: String, // 'setujui' or 'tolak'
        @Field("petugas_nama") nama: String,
        @Field("alasan") alasan: String? = null,
        @Field("signature_base64") signature: String? = null
    ): Call<ApiResponse<Map<String, Any>>>

    @FormUrlEncoded
    @POST("pengajuan_action_kecamatan.php")
    fun actionKecamatan(
        @Field("pengajuan_id") id: Int,
        @Field("action") action: String, // 'setujui' or 'tolak'
        @Field("petugas_nama") nama: String,
        @Field("alasan") alasan: String? = null
    ): Call<ApiResponse<Map<String, Any>>>

    @GET("verify_qr.php")
    fun verifyQr(
        @Query("token") token: String
    ): Call<ApiResponse<QrVerificationData>>

    @FormUrlEncoded
    @POST("pengajuan_delete.php")
    fun deletePengajuan(
        @Field("pengajuan_id") id: Int,
        @Field("user_id") userId: Int
    ): Call<ApiResponse<Void>>

    // Verifikasi Warga (RT/RW)
    @GET("warga_verifikasi.php")
    fun getWargaMenunggu(
        @Query("rt_id") rtId: Int? = null,
        @Query("status") status: String? = "menunggu_verifikasi"
    ): Call<ApiResponse<List<UserModel>>>

    @FormUrlEncoded
    @POST("warga_verifikasi.php")
    fun verifikasiWarga(
        @Field("user_id") userId: Int,
        @Field("action") action: String, // 'aktifkan' or 'tolak'
        @Field("petugas_nama") nama: String,
        @Field("catatan") catatan: String? = null
    ): Call<ApiResponse<Map<String, Any>>>

    // Pindah Domisili
    @FormUrlEncoded
    @POST("warga_pindah.php")
    fun ajukanPindah(
        @Field("action") action: String = "ajukan_pindah",
        @Field("user_id") userId: Int,
        @Field("alamat_tujuan") alamat: String,
        @Field("alasan_pindah") alasan: String
    ): Call<ApiResponse<Map<String, Any>>>

    // Antrean
    @GET("antrean.php")
    fun getAntreanList(
        @Query("user_id") userId: Int? = null,
        @Query("tanggal") tanggal: String? = null
    ): Call<ApiResponse<List<AntreanItem>>>

    @FormUrlEncoded
    @POST("antrean.php")
    fun createAntrean(
        @Field("action") action: String = "create",
        @Field("user_id") userId: Int,
        @Field("layanan") layanan: String,
        @Field("tanggal") tanggal: String,
        @Field("sesi") sesi: String
    ): Call<ApiResponse<AntreanItem>>

    @FormUrlEncoded
    @POST("antrean.php")
    fun cancelAntrean(
        @Field("action") action: String = "cancel",
        @Field("id") id: Int,
        @Field("user_id") userId: Int
    ): Call<ApiResponse<Void>>

    // Laporan Warga
    @GET("laporan.php")
    fun getLaporanList(
        @Query("user_id") userId: Int? = null,
        @Query("status") status: String? = null
    ): Call<ApiResponse<List<LaporanItem>>>

    @Multipart
    @POST("laporan.php")
    fun createLaporan(
        @Part("action") action: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("kategori") kategori: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("lokasi") lokasi: RequestBody,
        @Part fotoBukti: MultipartBody.Part?
    ): Call<ApiResponse<Map<String, Any>>>

    // Dashboard
    @GET("dashboard_stats.php")
    fun getDashboardStats(): Call<ApiResponse<DashboardStats>>

    // Change / Reset Password
    @FormUrlEncoded
    @POST("change_password.php")
    fun changePassword(
        @Field("action") action: String, // 'change' or 'reset_by_admin'
        @Field("user_id") userId: Int,
        @Field("current_password") current: String? = null,
        @Field("new_password") newPass: String,
        @Field("admin_id") adminId: Int? = null
    ): Call<ApiResponse<Void>>

    // FCM Push Notification Token
    @FormUrlEncoded
    @POST("update_fcm_token.php")
    fun updateFcmToken(
        @Field("user_id") userId: Int,
        @Field("fcm_token") token: String
    ): Call<ApiResponse<Map<String, Any>>>
}
