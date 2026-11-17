<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responseJson(false, "Method tidak diizinkan", null, 405);
}

$pengajuanId = isset($_POST['pengajuan_id']) ? (int)$_POST['pengajuan_id'] : 0;
$action = isset($_POST['action']) ? trim($_POST['action']) : ''; // 'setujui' or 'tolak'
$petugasNama = isset($_POST['petugas_nama']) ? trim($_POST['petugas_nama']) : 'Petugas Kecamatan';
$alasan = isset($_POST['alasan']) ? trim($_POST['alasan']) : '';

if ($pengajuanId <= 0) {
    responseJson(false, "ID Pengajuan wajib diisi", null, 400);
}

if ($action !== 'setujui' && $action !== 'tolak') {
    responseJson(false, "Aksi tidak valid (pilih setujui atau tolak)", null, 400);
}

try {
    $stmtCheck = $pdo->prepare("SELECT id, status, nomor_surat FROM pengajuan_surat WHERE id = ?");
    $stmtCheck->execute([$pengajuanId]);
    $current = $stmtCheck->fetch();

    if (!$current) {
        responseJson(false, "Data pengajuan tidak ditemukan", null, 404);
    }

    if ($action === 'setujui') {
        // Generate a cryptographically secure validation token for the QR Code
        $qrToken = 'SIPANTAS-' . date('Ymd') . '-' . str_pad($pengajuanId, 5, '0', STR_PAD_LEFT) . '-' . strtoupper(bin2hex(random_bytes(4)));
        $newStatus = 'selesai';

        $stmtUpdate = $pdo->prepare("UPDATE pengajuan_surat SET status = ?, qr_code_token = ?, tanggal_selesai = NOW(), catatan_penolakan = NULL, updated_at = NOW() WHERE id = ?");
        $stmtUpdate->execute([$newStatus, $qrToken, $pengajuanId]);

        // Audit log
        $stmtLog = $pdo->prepare("INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) VALUES (?, 'APPROVED_KECAMATAN', ?, 'kecamatan', ?, NOW())");
        $notes = "Surat telah disetujui & disahkan oleh Kecamatan. Kode verifikasi digital QR: $qrToken diterbitkan.";
        $stmtLog->execute([$pengajuanId, $petugasNama, $notes]);

        responseJson(true, "Surat resmi berhasil disetujui dan diterbitkan oleh Kecamatan", [
            'pengajuan_id' => $pengajuanId,
            'status' => $newStatus,
            'qr_code_token' => $qrToken
        ]);

    } else { // 'tolak'
        if (empty($alasan)) {
            responseJson(false, "Alasan penolakan wajib diisi oleh pihak Kecamatan", null, 400);
        }

        $newStatus = 'ditolak_kecamatan';
        $stmtUpdate = $pdo->prepare("UPDATE pengajuan_surat SET status = ?, catatan_penolakan = ?, updated_at = NOW() WHERE id = ?");
        $stmtUpdate->execute([$newStatus, $alasan, $pengajuanId]);

        // Audit log
        $stmtLog = $pdo->prepare("INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) VALUES (?, 'REJECTED_KECAMATAN', ?, 'kecamatan', ?, NOW())");
        $stmtLog->execute([$pengajuanId, $petugasNama, "Ditolak oleh Kecamatan: " . $alasan]);

        responseJson(true, "Pengajuan ditolak oleh pihak Kecamatan", [
            'pengajuan_id' => $pengajuanId,
            'status' => $newStatus,
            'catatan_penolakan' => $alasan
        ]);
    }

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
