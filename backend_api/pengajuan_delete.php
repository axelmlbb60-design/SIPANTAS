<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responseJson(false, "Method tidak diizinkan", null, 405);
}

$pengajuanId = isset($_POST['pengajuan_id']) ? (int)$_POST['pengajuan_id'] : 0;
$userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;

if ($pengajuanId <= 0 || $userId <= 0) {
    responseJson(false, "ID pengajuan dan ID pengguna wajib diisi", null, 400);
}

try {
    $stmt = $pdo->prepare("SELECT id, status, user_id, nomor_surat FROM pengajuan_surat WHERE id = ?");
    $stmt->execute([$pengajuanId]);
    $item = $stmt->fetch();

    if (!$item) {
        responseJson(false, "Pengajuan tidak ditemukan", null, 404);
    }

    if ($item['user_id'] != $userId) {
        responseJson(false, "Anda tidak memiliki akses untuk membatalkan pengajuan ini", null, 403);
    }

    if ($item['status'] !== 'menunggu_rt') {
        responseJson(false, "Hanya pengajuan dengan status 'Menunggu Persetujuan RT' yang dapat dibatalkan", null, 400);
    }

    // Delete audit log first
    $stmtDelLog = $pdo->prepare("DELETE FROM audit_log WHERE pengajuan_id = ?");
    $stmtDelLog->execute([$pengajuanId]);

    // Delete pengajuan
    $stmtDel = $pdo->prepare("DELETE FROM pengajuan_surat WHERE id = ?");
    $stmtDel->execute([$pengajuanId]);

    responseJson(true, "Pengajuan surat berhasil dibatalkan dan dihapus");

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
