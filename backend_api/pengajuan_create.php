<?php
require_once "config.php";

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$jenis_surat = isset($_POST['jenis_surat']) ? trim($_POST['jenis_surat']) : '';
$keperluan = isset($_POST['keperluan']) ? trim($_POST['keperluan']) : '';
$tujuan_wilayah = isset($_POST['tujuan_wilayah']) ? trim($_POST['tujuan_wilayah']) : 'ktp_asli';

if ($user_id <= 0 || empty($jenis_surat) || empty($keperluan)) {
    responseJson(false, "User ID, jenis surat, dan keperluan wajib diisi!");
}

// Cek data user
$uStmt = mysqli_prepare($koneksi, "SELECT nama_lengkap, status_akun FROM users WHERE id = ?");
mysqli_stmt_bind_param($uStmt, "i", $user_id);
mysqli_stmt_execute($uStmt);
$uRes = mysqli_stmt_get_result($uStmt);
$user = mysqli_fetch_assoc($uRes);

if (!$user) {
    responseJson(false, "Pengguna tidak ditemukan!");
}

// Generate nomor surat otomatis
$prefix = "SRT";
if (stripos($jenis_surat, "Domisili") !== false) $prefix = "SKD";
else if (stripos($jenis_surat, "Pengantar") !== false) $prefix = "SP";
else if (stripos($jenis_surat, "Tidak Mampu") !== false || stripos($jenis_surat, "SKTM") !== false) $prefix = "SKTM";
else if (stripos($jenis_surat, "Usaha") !== false || stripos($jenis_surat, "SKU") !== false) $prefix = "SKU";

$nomor_surat = $prefix . "-" . date("ymd") . "-" . rand(100, 999);

$insert = "INSERT INTO pengajuan_surat (nomor_surat, user_id, jenis_surat, keperluan, tujuan_wilayah, status) 
           VALUES (?, ?, ?, ?, ?, 'menunggu_rt')";
$stmt = mysqli_prepare($koneksi, $insert);
mysqli_stmt_bind_param($stmt, "sisss", $nomor_surat, $user_id, $jenis_surat, $keperluan, $tujuan_wilayah);

if (mysqli_stmt_execute($stmt)) {
    $pengajuan_id = mysqli_insert_id($koneksi);

    // Catat Jejak Audit (Audit Trail)
    $logSql = "INSERT INTO audit_log (pengajuan_id, action, actor_id, actor_name, actor_role, notes) 
               VALUES (?, 'DIAJUKAN', ?, ?, 'Warga', 'Permohonan surat diajukan mandiri via aplikasi SIPANTAS')";
    $logStmt = mysqli_prepare($koneksi, $logSql);
    mysqli_stmt_bind_param($logStmt, "iis", $pengajuan_id, $user_id, $user['nama_lengkap']);
    mysqli_stmt_execute($logStmt);

    responseJson(true, "Permohonan $jenis_surat berhasil diajukan dengan nomor $nomor_surat!", [
        "id" => $pengajuan_id,
        "nomor_surat" => $nomor_surat,
        "status" => "menunggu_rt",
        "tujuan_wilayah" => $tujuan_wilayah
    ]);
} else {
    responseJson(false, "Gagal mengajukan surat: " . mysqli_error($koneksi));
}
?>