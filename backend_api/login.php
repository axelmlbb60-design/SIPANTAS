<?php
require_once "config.php";

$nik = isset($_POST['nik']) ? trim($_POST['nik']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';

if (empty($nik) || empty($password)) {
    responseJson(false, "NIK dan password wajib diisi!");
}

$query = "SELECT u.*, 
                 k.nama AS nama_kecamatan, 
                 kel.nama AS nama_kelurahan, 
                 rw.nomor_rw, 
                 rt.nomor_rt 
          FROM users u
          LEFT JOIN wilayah_kecamatan k ON u.kecamatan_id = k.id
          LEFT JOIN wilayah_kelurahan kel ON u.kelurahan_id = kel.id
          LEFT JOIN wilayah_rw rw ON u.rw_id = rw.id
          LEFT JOIN wilayah_rt rt ON u.rt_id = rt.id
          WHERE u.nik = ?";

$stmt = mysqli_prepare($koneksi, $query);
mysqli_stmt_bind_param($stmt, "s", $nik);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

if ($row = mysqli_fetch_assoc($result)) {
    // Check password (plain or hash)
    if ($password === $row['password'] || password_verify($password, $row['password']) || md5($password) === $row['password']) {
        unset($row['password']); // Hapus password dari respons

        // Notifikasi badge permohonan baru untuk role RT/RW
        $unreadCount = 0;
        if ($row['role'] === 'rtrw') {
            $countQuery = "SELECT COUNT(*) as total FROM pengajuan_surat WHERE status = 'menunggu_rt'";
            $cRes = mysqli_query($koneksi, $countQuery);
            if ($cRow = mysqli_fetch_assoc($cRes)) {
                $unreadCount = (int)$cRow['total'];
            }
        }
        $row['notifikasi_unread'] = $unreadCount;

        responseJson(true, "Login berhasil!", $row);
    } else {
        responseJson(false, "Password yang Anda masukkan salah.");
    }
} else {
    responseJson(false, "Akun dengan NIK tersebut tidak ditemukan. Silakan registrasi terlebih dahulu.");
}
?>