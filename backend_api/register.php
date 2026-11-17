<?php
require_once "config.php";

$nik = isset($_POST['nik']) ? trim($_POST['nik']) : '';
$nama = isset($_POST['nama_lengkap']) ? trim($_POST['nama_lengkap']) : '';
$phone = isset($_POST['no_hp']) ? trim($_POST['no_hp']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';
$kecamatan_id = isset($_POST['kecamatan_id']) ? intval($_POST['kecamatan_id']) : 1;
$kelurahan_id = isset($_POST['kelurahan_id']) ? intval($_POST['kelurahan_id']) : 1;
$rw_id = isset($_POST['rw_id']) ? intval($_POST['rw_id']) : 1;
$rt_id = isset($_POST['rt_id']) ? intval($_POST['rt_id']) : 1;
$tipe_penduduk = isset($_POST['tipe_penduduk']) ? trim($_POST['tipe_penduduk']) : 'tetap';
$alamat_ktp = isset($_POST['alamat_ktp']) ? trim($_POST['alamat_ktp']) : '';
$alamat_domisili = isset($_POST['alamat_domisili']) ? trim($_POST['alamat_domisili']) : '';

// Validasi NIK 16 digit
if (strlen($nik) !== 16 || !ctype_digit($nik)) {
    responseJson(false, "NIK harus tepat 16 digit angka!");
}

if (empty($nama) || empty($phone) || empty($password)) {
    responseJson(false, "Nama lengkap, nomor HP, dan kata sandi wajib diisi!");
}

// Cek apakah NIK sudah terdaftar
$check = mysqli_prepare($koneksi, "SELECT id FROM users WHERE nik = ?");
mysqli_stmt_bind_param($check, "s", $nik);
mysqli_stmt_execute($check);
mysqli_stmt_store_result($check);
if (mysqli_stmt_num_rows($check) > 0) {
    responseJson(false, "NIK $nik sudah terdaftar di sistem SIPANTAS!");
}

// Handle upload foto KTP / dokumen (Maks 2 MB, JPG/PNG/PDF)
$foto_ktp = "default_ktp.jpg";
if (isset($_FILES['foto_ktp']) && $_FILES['foto_ktp']['error'] === UPLOAD_ERR_OK) {
    $fileTmp = $_FILES['foto_ktp']['tmp_name'];
    $fileName = $_FILES['foto_ktp']['name'];
    $fileSize = $_FILES['foto_ktp']['size'];
    $fileExt = strtolower(pathinfo($fileName, PATHINFO_EXTENSION));

    // Validasi batas ukuran 2 MB (2 * 1024 * 1024 bytes)
    if ($fileSize > 2097152) {
        responseJson(false, "Ukuran file dokumen KTP melebihi batas maksimal 2 MB!");
    }

    $allowedExt = ['jpg', 'jpeg', 'png', 'pdf'];
    if (!in_array($fileExt, $allowedExt)) {
        responseJson(false, "Format file tidak didukung! Harap unggah format JPG, PNG, atau PDF.");
    }

    $uploadDir = __DIR__ . "/uploads/ktp/";
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0777, true);
    }

    $newFileName = "ktp_" . $nik . "_" . time() . "." . $fileExt;
    if (move_uploaded_file($fileTmp, $uploadDir . $newFileName)) {
        $foto_ktp = $newFileName;
    }
}

// Simpan ke database dengan status_akun 'menunggu_verifikasi'
$insert = "INSERT INTO users 
           (nik, nama_lengkap, no_hp, password, role, kecamatan_id, kelurahan_id, rw_id, rt_id, status_akun, tipe_penduduk, alamat_ktp, alamat_domisili, foto_ktp) 
           VALUES (?, ?, ?, ?, 'warga', ?, ?, ?, ?, 'menunggu_verifikasi', ?, ?, ?, ?)";

$stmt = mysqli_prepare($koneksi, $insert);
mysqli_stmt_bind_param($stmt, "ssssiiiissss", 
    $nik, $nama, $phone, $password, 
    $kecamatan_id, $kelurahan_id, $rw_id, $rt_id, 
    $tipe_penduduk, $alamat_ktp, $alamat_domisili, $foto_ktp
);

if (mysqli_stmt_execute($stmt)) {
    $newId = mysqli_insert_id($koneksi);
    responseJson(true, "Registrasi berhasil! Akun Anda sedang dalam proses verifikasi oleh RT/RW setempat.", [
        "id" => $newId,
        "nik" => $nik,
        "nama_lengkap" => $nama,
        "status_akun" => "menunggu_verifikasi"
    ]);
} else {
    responseJson(false, "Terjadi kesalahan saat menyimpan data: " . mysqli_error($koneksi));
}
?>