<?php
require_once 'config.php';

try {
    // Total counts by status
    $stmtStatus = $pdo->query("SELECT status, COUNT(*) as count FROM pengajuan_surat GROUP BY status");
    $statusRows = $stmtStatus->fetchAll();

    $counts = [
        'total' => 0,
        'menunggu_rt' => 0,
        'menunggu_kecamatan' => 0,
        'selesai' => 0,
        'ditolak' => 0
    ];

    foreach ($statusRows as $r) {
        $st = $r['status'];
        $cnt = (int)$r['count'];
        $counts['total'] += $cnt;
        if ($st === 'menunggu_rt') $counts['menunggu_rt'] += $cnt;
        elseif ($st === 'menunggu_kecamatan') $counts['menunggu_kecamatan'] += $cnt;
        elseif ($st === 'selesai') $counts['selesai'] += $cnt;
        elseif (strpos($st, 'ditolak') !== false) $counts['ditolak'] += $cnt;
    }

    // Submissions by jenis_surat
    $stmtJenis = $pdo->query("SELECT jenis_surat, COUNT(*) as count FROM pengajuan_surat GROUP BY jenis_surat ORDER BY count DESC");
    $perJenis = $stmtJenis->fetchAll();

    // Submissions by kelurahan
    $sqlKel = "SELECT 
                COALESCE(l.nama, 'Luar Wilayah') as kelurahan, 
                COUNT(p.id) as count 
               FROM pengajuan_surat p
               LEFT JOIN users u ON p.user_id = u.id
               LEFT JOIN wilayah_kelurahan l ON u.kelurahan_id = l.id
               GROUP BY l.id, l.nama
               ORDER BY count DESC";
    $stmtKel = $pdo->query($sqlKel);
    $perKelurahan = $stmtKel->fetchAll();

    // Users overview
    $stmtUsers = $pdo->query("SELECT status_akun, COUNT(*) as count FROM users WHERE role = 'warga' GROUP BY status_akun");
    $userRows = $stmtUsers->fetchAll();
    $wargaCounts = ['aktif' => 0, 'menunggu_verifikasi' => 0, 'proses_pindah' => 0];
    foreach ($userRows as $u) {
        if (isset($wargaCounts[$u['status_akun']])) {
            $wargaCounts[$u['status_akun']] = (int)$u['count'];
        }
    }

    responseJson(true, "Statistik dashboard berhasil dimuat", [
        'ringkasan_pengajuan' => $counts,
        'per_jenis_surat' => $perJenis,
        'per_kelurahan' => $perKelurahan,
        'statistik_warga' => $wargaCounts
    ]);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
