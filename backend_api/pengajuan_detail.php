<?php
require_once 'config.php';

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
if ($id <= 0) {
    responseJson(false, "ID pengajuan tidak valid", null, 400);
}

try {
    $sql = "SELECT 
                p.*,
                u.nama_lengkap,
                u.nik,
                u.no_hp,
                u.email,
                u.tipe_penduduk,
                u.alamat_ktp,
                u.alamat_domisili,
                u.foto_ktp,
                k.nama as nama_kecamatan,
                l.nama as nama_kelurahan,
                rw.nomor_rw,
                rt.nomor_rt
            FROM pengajuan_surat p
            LEFT JOIN users u ON p.user_id = u.id
            LEFT JOIN wilayah_kecamatan k ON u.kecamatan_id = k.id
            LEFT JOIN wilayah_kelurahan l ON u.kelurahan_id = l.id
            LEFT JOIN wilayah_rw rw ON u.rw_id = rw.id
            LEFT JOIN wilayah_rt rt ON u.rt_id = rt.id
            WHERE p.id = ?";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$id]);
    $pengajuan = $stmt->fetch();

    if (!$pengajuan) {
        responseJson(false, "Pengajuan tidak ditemukan", null, 404);
    }

    // Attach full URLs
    if (!empty($pengajuan['dokumen_lampiran'])) {
        $pengajuan['dokumen_lampiran_url'] = getBaseUrl() . '/' . $pengajuan['dokumen_lampiran'];
    }
    if (!empty($pengajuan['tanda_tangan_rt'])) {
        $pengajuan['tanda_tangan_rt_url'] = getBaseUrl() . '/' . $pengajuan['tanda_tangan_rt'];
    }
    if (!empty($pengajuan['foto_ktp'])) {
        $pengajuan['foto_ktp_url'] = getBaseUrl() . '/' . $pengajuan['foto_ktp'];
    }

    // Fetch audit log
    $stmtLog = $pdo->prepare("SELECT * FROM audit_log WHERE pengajuan_id = ? ORDER BY created_at ASC");
    $stmtLog->execute([$id]);
    $logs = $stmtLog->fetchAll();

    $pengajuan['audit_logs'] = $logs;

    responseJson(true, "Detail pengajuan berhasil dimuat", $pengajuan);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
