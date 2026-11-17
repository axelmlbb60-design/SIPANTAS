package com.example.sipantas.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "sipantas_pref"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NIK = "nik"
        private const val KEY_NAMA = "nama_lengkap"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "no_hp"
        private const val KEY_ROLE = "role"
        private const val KEY_STATUS_AKUN = "status_akun"
        private const val KEY_TIPE_PENDUDUK = "tipe_penduduk"
        private const val KEY_ALAMAT_DOMISILI = "alamat_domisili"
        private const val KEY_NAMA_KELURAHAN = "nama_kelurahan"
        private const val KEY_NAMA_KECAMATAN = "nama_kecamatan"
        private const val KEY_NOMOR_RT = "nomor_rt"
        private const val KEY_NOMOR_RW = "nomor_rw"
        private const val KEY_BASE_URL = "base_url"

        // Default base URL: 10.0.2.2 is Android Emulator's alias for host machine localhost
        const val DEFAULT_BASE_URL = "http://10.0.2.2/api_sipantas/"
    }

    fun saveUserSession(user: UserModel) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_NIK, user.nik)
            putString(KEY_NAMA, user.namaLengkap)
            putString(KEY_EMAIL, user.email ?: "")
            putString(KEY_PHONE, user.noHp ?: "")
            putString(KEY_ROLE, user.role)
            putString(KEY_STATUS_AKUN, user.statusAkun)
            putString(KEY_TIPE_PENDUDUK, user.tipePenduduk ?: "tetap")
            putString(KEY_ALAMAT_DOMISILI, user.alamatDomisili ?: "")
            putString(KEY_NAMA_KELURAHAN, user.namaKelurahan ?: "")
            putString(KEY_NAMA_KECAMATAN, user.namaKecamatan ?: "")
            putString(KEY_NOMOR_RT, user.nomorRt ?: "")
            putString(KEY_NOMOR_RW, user.nomorRw ?: "")
            apply()
        }
    }

    fun updateUserStatus(status: String) {
        prefs.edit().putString(KEY_STATUS_AKUN, status).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getNik(): String = prefs.getString(KEY_NIK, "") ?: ""
    fun getNama(): String = prefs.getString(KEY_NAMA, "Pengguna") ?: "Pengguna"
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getRole(): String = prefs.getString(KEY_ROLE, "warga") ?: "warga"
    fun getStatusAkun(): String = prefs.getString(KEY_STATUS_AKUN, "aktif") ?: "aktif"
    fun getTipePenduduk(): String = prefs.getString(KEY_TIPE_PENDUDUK, "tetap") ?: "tetap"
    fun getAlamatDomisili(): String = prefs.getString(KEY_ALAMAT_DOMISILI, "") ?: ""
    fun getKelurahan(): String = prefs.getString(KEY_NAMA_KELURAHAN, "") ?: ""
    fun getKecamatan(): String = prefs.getString(KEY_NAMA_KECAMATAN, "") ?: ""
    fun getRt(): String = prefs.getString(KEY_NOMOR_RT, "") ?: ""
    fun getRw(): String = prefs.getString(KEY_NOMOR_RW, "") ?: ""

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        var formatted = url.trim()
        if (!formatted.endsWith("/")) formatted += "/"
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
    }

    fun logout() {
        val currentBaseUrl = getBaseUrl()
        prefs.edit().clear().apply()
        setBaseUrl(currentBaseUrl) // Keep base URL
    }
}
