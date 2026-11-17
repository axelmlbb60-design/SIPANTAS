<?php
require_once "config.php";

$userId = isset($_REQUEST['user_id']) ? (int)$_REQUEST['user_id'] : 0;
$action = isset($_REQUEST['action']) ? trim($_REQUEST['action']) : '';
$filter = isset($_REQUEST['filter']) ? trim($_REQUEST['filter']) : 'semua';

if ($userId <= 0) {
    responseJson(false, "User ID wajib diisi!");
}

// Handle Mark All as Read action
if ($action === 'mark_all_read') {
    mysqli_query($koneksi, "UPDATE notifikasi SET is_read = 1 WHERE user_id = $userId");
    responseJson(true, "Semua notifikasi ditandai telah dibaca.");
}

// Ensure sample notifications exist if user has none
$checkCount = mysqli_query($koneksi, "SELECT COUNT(*) as cnt FROM notifikasi WHERE user_id = $userId");
$rowCnt = mysqli_fetch_assoc($checkCount);
if ($rowCnt['cnt'] == 0) {
    $samples = [
        ['Surat Disetujui Ketua RT', 'Surat Keterangan Domisili Anda telah ditandatangani digital oleh Ketua RT 03.', 'surat', 1, 0],
        ['Pengingat Jadwal Antrean H-1', 'Antrean no A-012 untuk Legalisir dijadwalkan besok pukul 08:30 WITA.', 'antrean', 1, 0],
        ['Laporan Warga Diproses', 'Laporan Penerangan Jalan Padam sedang ditindaklanjuti oleh petugas.', 'laporan', 1, 1],
        ['Pengumuman Pelayanan Kelurahan', 'Penerbitan dokumen digital SIPANTAS beroperasi 24 jam dengan validasi QR Code.', 'info', 0, 1]
    ];
    $insStmt = mysqli_prepare($koneksi, "INSERT INTO notifikasi (user_id, judul, pesan, tipe, data_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW() - INTERVAL ? HOUR)");
    $h = 2;
    foreach ($samples as $s) {
        mysqli_stmt_bind_param($insStmt, "isssiii", $userId, $s[0], $s[1], $s[2], $s[3], $s[4], $h);
        mysqli_stmt_execute($insStmt);
        $h += 8;
    }
}

// Query notifications with optional filter
$query = "SELECT id, user_id, judul, pesan, tipe, data_id, is_read, created_at FROM notifikasi WHERE user_id = ?";
if ($filter !== '' && $filter !== 'semua') {
    $query .= " AND tipe = '" . mysqli_real_escape_string($koneksi, $filter) . "'";
}
$query .= " ORDER BY created_at DESC LIMIT 50";

$stmt = mysqli_prepare($koneksi, $query);
mysqli_stmt_bind_param($stmt, "i", $userId);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

$notifications = [];
while ($row = mysqli_fetch_assoc($result)) {
    // Format human-friendly time
    $createdTime = strtotime($row['created_at']);
    $diff = time() - $createdTime;
    if ($diff < 60) {
        $timeStr = "Baru saja";
    } elseif ($diff < 3600) {
        $timeStr = floor($diff / 60) . " mnt yang lalu";
    } elseif ($diff < 86400) {
        $timeStr = floor($diff / 3600) . " jam yang lalu";
    } else {
        $timeStr = floor($diff / 86400) . " hari yang lalu";
    }
    $row['waktu_lalu'] = $timeStr;
    $notifications[] = $row;
}

responseJson(true, "Daftar notifikasi berhasil dimuat", $notifications);
?>
