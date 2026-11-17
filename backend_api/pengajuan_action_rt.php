<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responseJson(false, "Method tidak diizinkan", null, 405);
}

$pengajuanId = isset($_POST['pengajuan_id']) ? (int)$_POST['pengajuan_id'] : 0;
$action = isset($_POST['action']) ? trim($_POST['action']) : ''; // 'setujui' or 'tolak'
$petugasNama = isset($_POST['petugas_nama']) ? trim($_POST['petugas_nama']) : 'Ketua RT/RW';
$alasan = isset($_POST['alasan']) ? trim($_POST['alasan']) : '';
$signatureBase64 = isset($_POST['signature_base64']) ? trim($_POST['signature_base64']) : '';

if ($pengajuanId <= 0) {
    responseJson(false, "ID Pengajuan wajib diisi", null, 400);
}

if ($action !== 'setujui' && $action !== 'tolak') {
    responseJson(false, "Aksi tidak valid (pilih setujui atau tolak)", null, 400);
}

try {
    // Check current state of submission
    $stmtCheck = $pdo->prepare("SELECT id, status, nomor_surat FROM pengajuan_surat WHERE id = ?");
    $stmtCheck->execute([$pengajuanId]);
    $current = $stmtCheck->fetch();

    if (!$current) {
        responseJson(false, "Data pengajuan tidak ditemukan", null, 404);
    }

    if ($action === 'setujui') {
        $signaturePath = null;

        // Save digital canvas signature if provided
        if (!empty($signatureBase64)) {
            $sigDir = __DIR__ . '/uploads/signatures';
            if (!is_dir($sigDir)) {
                mkdir($sigDir, 0777, true);
            }
            // Strip data:image/png;base64, prefix if present
            $cleaned = preg_replace('#^data:image/\w+;base64,#i', '', $signatureBase64);
            $sigData = base64_decode($cleaned);
            if ($sigData !== false) {
                $filename = 'sig_rt_' . $pengajuanId . '_' . time() . '.png';
                file_put_contents($sigDir . '/' . $filename, $sigData);
                $signaturePath = 'uploads/signatures/' . $filename;
            }
        }

        // Update status to 'menunggu_kecamatan' (approved by RT, forwarded to Kecamatan)
        $newStatus = 'menunggu_kecamatan';
        $stmtUpdate = $pdo->prepare("UPDATE pengajuan_surat SET status = ?, tanda_tangan_rt = COALESCE(?, tanda_tangan_rt), catatan_penolakan = NULL, updated_at = NOW() WHERE id = ?");
        $stmtUpdate->execute([$newStatus, $signaturePath, $pengajuanId]);

        // Audit log
        $stmtLog = $pdo->prepare("INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) VALUES (?, 'APPROVED_RT', ?, 'rtrw', 'Disetujui oleh RT/RW dengan tanda tangan digital kanvas. Diteruskan ke Kecamatan.', NOW())");
        $stmtLog->execute([$pengajuanId, $petugasNama]);

        responseJson(true, "Pengajuan surat berhasil disetujui oleh RT/RW dan diteruskan ke Kecamatan", [
            'pengajuan_id' => $pengajuanId,
            'status' => $newStatus,
            'signature_path' => $signaturePath ? getBaseUrl() . '/' . $signaturePath : null
        ]);

    } else { // 'tolak'
        if (empty($alasan)) {
            responseJson(false, "Alasan penolakan wajib diisi oleh RT/RW", null, 400);
        }

        $newStatus = 'ditolak_rt';
        $stmtUpdate = $pdo->prepare("UPDATE pengajuan_surat SET status = ?, catatan_penolakan = ?, updated_at = NOW() WHERE id = ?");
        $stmtUpdate->execute([$newStatus, $alasan, $pengajuanId]);

        // Audit log
        $stmtLog = $pdo->prepare("INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) VALUES (?, 'REJECTED_RT', ?, 'rtrw', ?, NOW())");
        $stmtLog->execute([$pengajuanId, $petugasNama, "Ditolak oleh RT/RW: " . $alasan]);

        responseJson(true, "Pengajuan berhasil ditolak dengan alasan tersimpan", [
            'pengajuan_id' => $pengajuanId,
            'status' => $newStatus,
            'catatan_penolakan' => $alasan
        ]);
    }

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
