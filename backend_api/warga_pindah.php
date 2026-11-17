<?php
require_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'POST') {
        $action = isset($_POST['action']) ? trim($_POST['action']) : '';
        $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;

        if ($userId <= 0) {
            responseJson(false, "User ID wajib diisi", null, 400);
        }

        if ($action === 'ajukan_pindah') {
            $alamatTujuan = isset($_POST['alamat_tujuan']) ? trim($_POST['alamat_tujuan']) : '';
            $alasanPindah = isset($_POST['alasan_pindah']) ? trim($_POST['alasan_pindah']) : '';

            if (empty($alamatTujuan)) {
                responseJson(false, "Alamat tujuan pindah wajib diisi", null, 400);
            }

            // Update user status to 'proses_pindah'
            $stmt = $pdo->prepare("UPDATE users SET status_akun = 'proses_pindah', alamat_domisili = ?, updated_at = NOW() WHERE id = ?");
            $stmt->execute([$alamatTujuan, $userId]);

            // Create Surat Keterangan Pindah Domisili automatically
            $stmtUser = $pdo->prepare("SELECT * FROM users WHERE id = ?");
            $stmtUser->execute([$userId]);
            $user = $stmtUser->fetch();

            $nomorSurat = 'SIP/' . date('Ym') . '/DOM/' . str_pad($userId, 4, '0', STR_PAD_LEFT);
            $stmtSurat = $pdo->prepare("INSERT INTO pengajuan_surat (user_id, nomor_surat, jenis_surat, keperluan, tujuan_wilayah, status, created_at, updated_at) VALUES (?, ?, 'Surat Keterangan Pindah Domisili', ?, 'ktp_asli', 'menunggu_rt', NOW(), NOW())");
            $stmtSurat->execute([$userId, $nomorSurat, "Pindah domisili ke: " . $alamatTujuan . ". Alasan: " . $alasanPindah]);
            $pengajuanId = $pdo->lastInsertId();

            // Audit log
            $stmtLog = $pdo->prepare("INSERT INTO audit_log (pengajuan_id, action, actor_name, actor_role, notes, created_at) VALUES (?, 'SUBMITTED', ?, 'warga', 'Permohonan pindah domisili diajukan', NOW())");
            $stmtLog->execute([$pengajuanId, $user['nama_lengkap']]);

            responseJson(true, "Permohonan pindah domisili berhasil diajukan", [
                'user_id' => $userId,
                'status_akun' => 'proses_pindah',
                'pengajuan_id' => $pengajuanId,
                'nomor_surat' => $nomorSurat
            ]);

        } elseif ($action === 'terima_rt_baru') {
            // RT Baru accepts the relocated citizen
            $newKecamatan = isset($_POST['kecamatan_id']) ? (int)$_POST['kecamatan_id'] : null;
            $newKelurahan = isset($_POST['kelurahan_id']) ? (int)$_POST['kelurahan_id'] : null;
            $newRw = isset($_POST['rw_id']) ? (int)$_POST['rw_id'] : null;
            $newRt = isset($_POST['rt_id']) ? (int)$_POST['rt_id'] : null;

            $stmt = $pdo->prepare("UPDATE users SET status_akun = 'aktif', kecamatan_id = COALESCE(?, kecamatan_id), kelurahan_id = COALESCE(?, kelurahan_id), rw_id = COALESCE(?, rw_id), rt_id = COALESCE(?, rt_id), updated_at = NOW() WHERE id = ?");
            $stmt->execute([$newKecamatan, $newKelurahan, $newRw, $newRt, $userId]);

            responseJson(true, "Warga berhasil diterima di wilayah RT baru dan status akun telah Aktif kembali", [
                'user_id' => $userId,
                'status_akun' => 'aktif'
            ]);

        } else {
            responseJson(false, "Aksi tidak dikenali", null, 400);
        }
    } else {
        responseJson(false, "Method tidak didukung", null, 405);
    }
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
