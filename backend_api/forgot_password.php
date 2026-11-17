<?php
require_once "config.php";

$identifier = isset($_POST['identifier']) ? trim($_POST['identifier']) : '';

if (empty($identifier)) {
    responseJson(false, "Nomor ponsel, email, atau NIK wajib diisi!");
}

// Find user by NIK, no_hp, or email
$stmt = mysqli_prepare($koneksi, "SELECT id, nama_lengkap, no_hp, email, nik FROM users WHERE nik = ? OR no_hp = ? OR (email IS NOT NULL AND email = ?)");
mysqli_stmt_bind_param($stmt, "sss", $identifier, $identifier, $identifier);
mysqli_stmt_execute($stmt);
$res = mysqli_stmt_get_result($stmt);

if ($user = mysqli_fetch_assoc($res)) {
    // Generate temporary 6-digit OTP / reset code
    $resetCode = rand(100000, 999999);
    
    // Save notification to user
    $pesan = "Kode pemulihan kata sandi Anda adalah: $resetCode. Gunakan kode ini untuk verifikasi akun SIPANTAS Anda.";
    $notifStmt = mysqli_prepare($koneksi, "INSERT INTO notifikasi (user_id, judul, pesan, tipe, data_id, is_read) VALUES (?, 'Kode Pemulihan Kata Sandi', ?, 'info', 0, 0)");
    mysqli_stmt_bind_param($notifStmt, "is", $user['id'], $pesan);
    mysqli_stmt_execute($notifStmt);

    responseJson(true, "Petunjuk pemulihan telah dikirim ke " . ($user['email'] ? $user['email'] : $user['no_hp']) . ". Kode verifikasi pemulihan: $resetCode", [
        'user_id' => $user['id'],
        'nama_lengkap' => $user['nama_lengkap'],
        'reset_code' => $resetCode
    ]);
} else {
    responseJson(false, "Akun dengan identitas '$identifier' tidak ditemukan dalam sistem SIPANTAS.");
}
?>
