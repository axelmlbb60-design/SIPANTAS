<?php
require_once "config.php";

$userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
if ($userId <= 0) {
    responseJson(false, "User ID wajib diisi!");
}

$nama = isset($_POST['nama_lengkap']) ? trim($_POST['nama_lengkap']) : null;
$phone = isset($_POST['no_hp']) ? trim($_POST['no_hp']) : null;
$email = isset($_POST['email']) ? trim($_POST['email']) : null;
$domisili = isset($_POST['alamat_domisili']) ? trim($_POST['alamat_domisili']) : null;

// Ensure foto_profil column exists
$checkCol = mysqli_query($koneksi, "SHOW COLUMNS FROM users LIKE 'foto_profil'");
if (mysqli_num_rows($checkCol) == 0) {
    mysqli_query($koneksi, "ALTER TABLE users ADD COLUMN foto_profil VARCHAR(255) NULL AFTER foto_ktp");
}

$fotoProfilPath = null;
if (isset($_FILES['foto_profil']) && $_FILES['foto_profil']['error'] === UPLOAD_ERR_OK) {
    $fileTmp = $_FILES['foto_profil']['tmp_name'];
    $fileName = $_FILES['foto_profil']['name'];
    $fileSize = $_FILES['foto_profil']['size'];
    $fileExt = strtolower(pathinfo($fileName, PATHINFO_EXTENSION));

    if ($fileSize > 2097152) {
        responseJson(false, "Ukuran file foto melebihi batas 2MB!");
    }

    $allowedExt = ['jpg', 'jpeg', 'png'];
    if (!in_array($fileExt, $allowedExt)) {
        responseJson(false, "Format foto tidak didukung (gunakan JPG atau PNG)!");
    }

    $uploadDir = __DIR__ . "/uploads/profiles/";
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0777, true);
    }

    $newFileName = "profile_" . $userId . "_" . time() . "." . $fileExt;
    if (move_uploaded_file($fileTmp, $uploadDir . $newFileName)) {
        $fotoProfilPath = "uploads/profiles/" . $newFileName;
    }
}

// Build UPDATE query
$updates = [];
$params = [];
$types = "";

if ($nama !== null && $nama !== '') {
    $updates[] = "nama_lengkap = ?";
    $params[] = $nama;
    $types .= "s";
}
if ($phone !== null && $phone !== '') {
    $updates[] = "no_hp = ?";
    $params[] = $phone;
    $types .= "s";
}
if ($email !== null) {
    $updates[] = "email = ?";
    $params[] = $email;
    $types .= "s";
}
if ($domisili !== null) {
    $updates[] = "alamat_domisili = ?";
    $params[] = $domisili;
    $types .= "s";
}
if ($fotoProfilPath !== null) {
    $updates[] = "foto_profil = ?";
    $params[] = $fotoProfilPath;
    $types .= "s";
}

if (empty($updates)) {
    responseJson(false, "Tidak ada data profil yang diubah.");
}

$sql = "UPDATE users SET " . implode(", ", $updates) . ", updated_at = NOW() WHERE id = ?";
$params[] = $userId;
$types .= "i";

$stmt = mysqli_prepare($koneksi, $sql);
mysqli_stmt_bind_param($stmt, $types, ...$params);

if (mysqli_stmt_execute($stmt)) {
    // Fetch updated user
    $q = mysqli_query($koneksi, "SELECT * FROM users WHERE id = $userId");
    $updatedUser = mysqli_fetch_assoc($q);
    unset($updatedUser['password']);
    if (!empty($updatedUser['foto_profil'])) {
        $updatedUser['foto_profil_url'] = getBaseUrl() . '/' . $updatedUser['foto_profil'];
    }

    responseJson(true, "Data profil berhasil diperbarui!", $updatedUser);
} else {
    responseJson(false, "Gagal memperbarui profil: " . mysqli_error($koneksi));
}
?>
