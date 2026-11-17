<?php
require_once "config.php";

$role = isset($_POST['role']) ? trim($_POST['role']) : '';
$kode_undangan = isset($_POST['kode_undangan']) ? trim($_POST['kode_undangan']) : '';
$nama = isset($_POST['nama_lengkap']) ? trim($_POST['nama_lengkap']) : '';
$no_hp = isset($_POST['no_hp']) ? trim($_POST['no_hp']) : '';
$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';
$nip = isset($_POST['nip']) ? trim($_POST['nip']) : '';
$sk_pengangkatan = isset($_POST['sk_pengangkatan']) ? trim($_POST['sk_pengangkatan']) : '';
$kelurahan_id = isset($_POST['kelurahan_id']) ? intval($_POST['kelurahan_id']) : 1;
$rw_id = isset($_POST['rw_id']) ? intval($_POST['rw_id']) : 1;
$rt_id = isset($_POST['rt_id']) ? intval($_POST['rt_id']) : 1;

if (empty($role) || empty($nama) || empty($password)) {
    responseJson(false, "Peran, nama lengkap, dan kata sandi wajib diisi!");
}

// Normalise role
$roleDb = 'warga';
if (stripos($role, 'rt') !== false || stripos($role, 'rw') !== false) {
    $roleDb = 'rtrw';
} elseif (stripos($role, 'kelurahan') !== false || stripos($role, 'petugas') !== false || stripos($role, 'kecamatan') !== false) {
    $roleDb = 'kecamatan';
} elseif (stripos($role, 'admin') !== false) {
    $roleDb = 'admin';
}

// Validate invitation code
if (empty($kode_undangan)) {
    responseJson(false, "Kode undangan aktivasi wajib diisi!");
}

// Auto generate 16 digit NIK for officer if not provided
$nik = isset($_POST['nik']) && strlen(trim($_POST['nik'])) === 16 ? trim($_POST['nik']) : '';
if (empty($nik)) {
    $prefix = ($roleDb === 'rtrw') ? '737102' : (($roleDb === 'kecamatan') ? '737103' : '737104');
    $nik = $prefix . substr(str_pad(time(), 10, '0', STR_PAD_LEFT), 0, 10);
}

// Check existing user by NIK
$check = mysqli_prepare($koneksi, "SELECT id FROM users WHERE nik = ?");
mysqli_stmt_bind_param($check, "s", $nik);
mysqli_stmt_execute($check);
mysqli_stmt_store_result($check);
if (mysqli_stmt_num_rows($check) > 0) {
    // Regenerate unique NIK if conflict
    $nik = substr($nik, 0, 12) . rand(1000, 9999);
}

// Insert new officer user
$alamat = "Wilayah Penugasan SIPANTAS";
$statusAkun = "aktif";

$stmt = mysqli_prepare($koneksi, "INSERT INTO users 
    (nik, nama_lengkap, no_hp, nip, email, sk_pengangkatan, kode_undangan, password, role, kecamatan_id, kelurahan_id, rw_id, rt_id, status_akun, alamat_ktp, alamat_domisili)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?)");

mysqli_stmt_bind_param($stmt, "sssssssssiiisss", 
    $nik, $nama, $no_hp, $nip, $email, $sk_pengangkatan, $kode_undangan, $password, $roleDb, 
    $kelurahan_id, $rw_id, $rt_id, $statusAkun, $alamat, $alamat
);

if (mysqli_stmt_execute($stmt)) {
    $newId = mysqli_insert_id($koneksi);
    
    // Add welcome notification
    $judulNotif = "Selamat Datang di SIPANTAS";
    $pesanNotif = "Akun Anda sebagai " . strtoupper($roleDb) . " telah aktif. Anda dapat langsung mengelola surat dan layanan warga.";
    mysqli_query($koneksi, "INSERT INTO notifikasi (user_id, judul, pesan, tipe, data_id, is_read) VALUES ($newId, '$judulNotif', '$pesanNotif', 'info', 0, 0)");

    responseJson(true, "Aktivasi akun $nama (" . strtoupper($roleDb) . ") berhasil! Silakan masuk dengan NIK: $nik", [
        'user_id' => $newId,
        'nik' => $nik,
        'nama_lengkap' => $nama,
        'role' => $roleDb
    ]);
} else {
    responseJson(false, "Gagal mengaktivasi akun: " . mysqli_error($koneksi));
}
?>
