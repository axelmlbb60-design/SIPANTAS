<?php
require_once 'config.php';

$userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$role = isset($_GET['role']) ? trim($_GET['role']) : '';
$status = isset($_GET['status']) ? trim($_GET['status']) : '';
$search = isset($_GET['search']) ? trim($_GET['search']) : '';

try {
    $whereClauses = [];
    $params = [];

    // Filter by user for warga role
    if ($userId > 0 && ($role === 'warga' || empty($role))) {
        $whereClauses[] = "p.user_id = ?";
        $params[] = $userId;
    }

    // Role-based filtering for officers
    if ($role === 'rtrw') {
        // RT/RW can see submissions waiting for RT or relevant to their wilayah
        if (!empty($status)) {
            $whereClauses[] = "p.status = ?";
            $params[] = $status;
        } else {
            // Default view for RT/RW: prioritize menunggu_rt, but allow seeing all
            // If rt_id or rw_id is passed
            if (!empty($_GET['rt_id'])) {
                $whereClauses[] = "(u.rt_id = ? OR p.tujuan_wilayah = 'tempat_kos')";
                $params[] = (int)$_GET['rt_id'];
            }
        }
    } elseif ($role === 'kecamatan') {
        // Kecamatan sees submissions that passed RT (menunggu_kecamatan) or completed/rejected
        if (!empty($status)) {
            $whereClauses[] = "p.status = ?";
            $params[] = $status;
        }
    }

    // Explicit status filter if provided and not already handled
    if (!empty($status) && $role !== 'rtrw' && $role !== 'kecamatan') {
        $whereClauses[] = "p.status = ?";
        $params[] = $status;
    }

    if (!empty($search)) {
        $whereClauses[] = "(p.nomor_surat LIKE ? OR p.jenis_surat LIKE ? OR u.nama_lengkap LIKE ? OR u.nik LIKE ?)";
        $searchTerm = "%$search%";
        $params[] = $searchTerm;
        $params[] = $searchTerm;
        $params[] = $searchTerm;
        $params[] = $searchTerm;
    }

    $whereSql = !empty($whereClauses) ? "WHERE " . implode(" AND ", $whereClauses) : "";

    $sql = "SELECT 
                p.*,
                u.nama_lengkap,
                u.nik,
                u.no_hp,
                u.tipe_penduduk,
                u.alamat_domisili,
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
            $whereSql
            ORDER BY p.created_at DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $results = $stmt->fetchAll();

    // Map photo and signature URLs
    foreach ($results as &$item) {
        if (!empty($item['dokumen_lampiran'])) {
            $item['dokumen_lampiran_url'] = getBaseUrl() . '/' . $item['dokumen_lampiran'];
        }
        if (!empty($item['tanda_tangan_rt'])) {
            $item['tanda_tangan_rt_url'] = getBaseUrl() . '/' . $item['tanda_tangan_rt'];
        }
    }

    responseJson(true, "Data pengajuan berhasil dimuat", $results);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
