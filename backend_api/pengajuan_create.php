<?php
require_once "config.php";
require_once "fcm_helper.php";

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$jenis_surat = isset($_POST['jenis_surat']) ? trim($_POST['jenis_surat']) : '';
$keperluan = isset($_POST['keperluan']) ? trim($_POST['keperluan']) : '';
$tujuan_wilayah = isset($_POST['tujuan_wilayah']) ? trim($_POST['tujuan_wilayah']) : 'ktp_asli';

if ($user_id <= 0 || empty($jenis_surat) || empty($keperluan)) {
    responseJson(false, "User ID, jenis surat, dan keperluan wajib diisi!");
}

try {
    // Cek data user
    $uStmt = $pdo->prepare("SELECT nama_lengkap, status_akun, rt_id, rw_id FROM users WHERE id = ?");
    $uStmt->execute([$user_id]);
    $user = $uStmt->fetch();

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

    $insert = "INSERT INTO pengajuan_surat (nomor_surat, user_id, jenis_surat, keperluan, tujuan_wilayah, status, created_at, updated_at) 
               VALUES (?, ?, ?, ?, ?, 'menunggu_rt', NOW(), NOW())";
    $stmt = $pdo->prepare($insert);
    $stmt->execute([$nomor_surat, $user_id, $jenis_surat, $keperluan, $tujuan_wilayah]);
    $pengajuan_id = $pdo->lastInsertId();

    // Catat Jejak Audit (Audit Trail)
    $logSql = "INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) 
               VALUES (?, 'SUBMITTED', ?, 'warga', 'Permohonan surat diajukan mandiri via aplikasi SIPANTAS', NOW())";
    $logStmt = $pdo->prepare($logSql);
    $logStmt->execute([$pengajuan_id, $user['nama_lengkap']]);

    // Kirim Push Notification & In-App Alert ke Ketua RT / RW terkait
    $rtStmt = $pdo->prepare("SELECT id FROM users WHERE role = 'rtrw' LIMIT 5");
    $rtStmt->execute();
    $rtList = $rtStmt->fetchAll();

    foreach ($rtList as $rt) {
        $notifTitle = "📬 Permohonan Surat Baru Masuk!";
        $notifBody = "Warga {$user['nama_lengkap']} baru saja mengajukan {$jenis_surat} ({$nomor_surat}). Menunggu verifikasi & tanda tangan digital Anda.";
        createInAppNotification($pdo, $rt['id'], $notifTitle, $notifBody, 'pengajuan_baru', $pengajuan_id);
    }

    responseJson(true, "Permohonan $jenis_surat berhasil diajukan dengan nomor $nomor_surat!", [
        "id" => $pengajuan_id,
        "nomor_surat" => $nomor_surat,
        "status" => "menunggu_rt",
        "tujuan_wilayah" => $tujuan_wilayah
    ]);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
