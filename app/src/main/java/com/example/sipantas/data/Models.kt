package com.example.sipantas.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
) : Serializable

data class UserModel(
    @SerializedName("id") val id: Int,
    @SerializedName("nik") val nik: String,
    @SerializedName("nama_lengkap") val namaLengkap: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    @SerializedName("role") val role: String, // 'warga', 'rtrw', 'kecamatan', 'admin'
    @SerializedName("status_akun") val statusAkun: String, // 'menunggu_verifikasi', 'aktif', 'proses_pindah', 'ditolak'
    @SerializedName("tipe_penduduk") val tipePenduduk: String? = "tetap", // 'tetap', 'sementara'
    @SerializedName("alamat_ktp") val alamatKtp: String? = null,
    @SerializedName("alamat_domisili") val alamatDomisili: String? = null,
    @SerializedName("foto_ktp") val fotoKtp: String? = null,
    @SerializedName("foto_ktp_url") val fotoKtpUrl: String? = null,
    @SerializedName("kecamatan_id") val kecamatanId: Int? = null,
    @SerializedName("kelurahan_id") val kelurahanId: Int? = null,
    @SerializedName("rw_id") val rwId: Int? = null,
    @SerializedName("rt_id") val rtId: Int? = null,
    @SerializedName("nama_kecamatan") val namaKecamatan: String? = null,
    @SerializedName("nama_kelurahan") val namaKelurahan: String? = null,
    @SerializedName("nomor_rw") val nomorRw: String? = null,
    @SerializedName("nomor_rt") val nomorRt: String? = null,
    @SerializedName("unread_badge_count") val unreadBadgeCount: Int? = 0
) : Serializable

data class WilayahItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String? = null,
    @SerializedName("nomor_rw") val nomorRw: String? = null,
    @SerializedName("nomor_rt") val nomorRt: String? = null
) : Serializable {
    fun getDisplayName(): String {
        return nama ?: nomorRw?.let { "RW $it" } ?: nomorRt?.let { "RT $it" } ?: "ID: $id"
    }

    override fun toString(): String {
        return getDisplayName()
    }
}

data class PengajuanItem(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("nomor_surat") val nomorSurat: String,
    @SerializedName("jenis_surat") val jenisSurat: String,
    @SerializedName("keperluan") val keperluan: String,
    @SerializedName("dokumen_lampiran") val dokumenLampiran: String? = null,
    @SerializedName("dokumen_lampiran_url") val dokumenLampiranUrl: String? = null,
    @SerializedName("tujuan_wilayah") val tujuanWilayah: String = "ktp_asli", // 'ktp_asli' or 'tempat_kos'
    @SerializedName("status") val status: String, // 'menunggu_rt', 'disetujui_rt', 'ditolak_rt', 'menunggu_kecamatan', 'selesai', 'ditolak_kecamatan'
    @SerializedName("catatan_penolakan") val catatanPenolakan: String? = null,
    @SerializedName("tanda_tangan_rt") val tandaTanganRt: String? = null,
    @SerializedName("tanda_tangan_rt_url") val tandaTanganRtUrl: String? = null,
    @SerializedName("qr_code_token") val qrCodeToken: String? = null,
    @SerializedName("tanggal_selesai") val tanggalSelesai: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("nama_lengkap") val namaLengkap: String? = null,
    @SerializedName("nik") val nik: String? = null,
    @SerializedName("no_hp") val noHp: String? = null,
    @SerializedName("nama_kecamatan") val namaKecamatan: String? = null,
    @SerializedName("nama_kelurahan") val namaKelurahan: String? = null,
    @SerializedName("nomor_rw") val nomorRw: String? = null,
    @SerializedName("nomor_rt") val nomorRt: String? = null,
    @SerializedName("audit_logs") val auditLogs: List<AuditLogItem>? = null
) : Serializable

data class AuditLogItem(
    @SerializedName("id") val id: Int,
    @SerializedName("pengajuan_id") val pengajuanId: Int,
    @SerializedName("action") val action: String,
    @SerializedName("actor_name") val actorName: String,
    @SerializedName("actor_role") val actorRole: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String
) : Serializable

data class AntreanItem(
    @SerializedName("id") val id: Int,
    @SerializedName("nomor_tiket") val nomorTiket: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("layanan") val layanan: String,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("sesi") val sesi: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("nama_lengkap") val namaLengkap: String? = null,
    @SerializedName("nik") val nik: String? = null
) : Serializable

data class LaporanItem(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("judul") val judul: String,
    @SerializedName("kategori") val kategori: String,
    @SerializedName("deskripsi") val deskripsi: String,
    @SerializedName("lokasi") val lokasi: String? = null,
    @SerializedName("foto_bukti") val fotoBukti: String? = null,
    @SerializedName("foto_bukti_url") val fotoBuktiUrl: String? = null,
    @SerializedName("status") val status: String, // 'terkirim', 'diproses', 'selesai', 'ditolak'
    @SerializedName("tanggapan_petugas") val tanggapanPetugas: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("nama_lengkap") val namaLengkap: String? = null
) : Serializable

data class DashboardStats(
    @SerializedName("ringkasan_pengajuan") val ringkasanPengajuan: Map<String, Int>? = null,
    @SerializedName("per_jenis_surat") val perJenisSurat: List<Map<String, Any>>? = null,
    @SerializedName("per_kelurahan") val perKelurahan: List<Map<String, Any>>? = null,
    @SerializedName("statistik_warga") val statistikWarga: Map<String, Int>? = null
) : Serializable

data class QrVerificationData(
    @SerializedName("is_valid") val isValid: Boolean,
    @SerializedName("nomor_surat") val nomorSurat: String? = null,
    @SerializedName("jenis_surat") val jenisSurat: String? = null,
    @SerializedName("nama_pemohon") val namaPemohon: String? = null,
    @SerializedName("nik_masked") val nikMasked: String? = null,
    @SerializedName("keperluan") val keperluan: String? = null,
    @SerializedName("wilayah") val wilayah: String? = null,
    @SerializedName("tanggal_pengesahan") val tanggalPengesahan: String? = null,
    @SerializedName("qr_token") val qrToken: String? = null
) : Serializable

data class NotificationItem(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("judul") val judul: String,
    @SerializedName("pesan") val pesan: String,
    @SerializedName("tipe") val tipe: String? = "info",
    @SerializedName("data_id") val dataId: Int? = 0,
    @SerializedName("is_read") val isRead: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("waktu_lalu") val waktuLalu: String? = null
) : Serializable
