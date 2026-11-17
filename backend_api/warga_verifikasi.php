<?php
require_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        // List citizens waiting for verification
        $rtId = isset($_GET['rt_id']) ? (int)$_GET['rt_id'] : 0;
        $status = isset($_GET['status']) ? trim($_GET['status']) : 'menunggu_verifikasi';

        $where = ["u.role = 'warga'"];
        $params = [];

        if (!empty($status)) {
            $where[] = "u.status_akun = ?";
            $params[] = $status;
        }

        if ($rtId > 0) {
            $where[] = "u.rt_id = ?";
            $params[] = $rtId;
        }

        $sql = "SELECT 
                    u.id,
                    u.nik,
                    u.nama_lengkap,
                    u.email,
                    u.no_hp,
                    u.tipe_penduduk,
                    u.alamat_ktp,
                    u.alamat_domisili,
                    u.foto_ktp,
                    u.status_akun,
                    u.created_at,
                    k.nama as nama_kecamatan,
                    l.nama as nama_kelurahan,
                    rw.nomor_rw,
                    rt.nomor_rt
                FROM users u
                LEFT JOIN wilayah_kecamatan k ON u.kecamatan_id = k.id
                LEFT JOIN wilayah_kelurahan l ON u.kelurahan_id = l.id
                LEFT JOIN wilayah_rw rw ON u.rw_id = rw.id
                LEFT JOIN wilayah_rt rt ON u.rt_id = rt.id
                WHERE " . implode(" AND ", $where) . "
                ORDER BY u.created_at DESC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $users = $stmt->fetchAll();

        foreach ($users as &$u) {
            if (!empty($u['foto_ktp'])) {
                $u['foto_ktp_url'] = getBaseUrl() . '/' . $u['foto_ktp'];
            }
        }

        responseJson(true, "Data warga berhasil dimuat", $users);

    } elseif ($method === 'POST') {
        $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
        $action = isset($_POST['action']) ? trim($_POST['action']) : ''; // 'aktifkan' or 'tolak'
        $petugasNama = isset($_POST['petugas_nama']) ? trim($_POST['petugas_nama']) : 'Ketua RT';
        $catatan = isset($_POST['catatan']) ? trim($_POST['catatan']) : '';

        if ($userId <= 0) {
            responseJson(false, "ID warga wajib diisi", null, 400);
        }

        if ($action !== 'aktifkan' && $action !== 'tolak') {
            responseJson(false, "Aksi tidak valid (pilih aktifkan atau tolak)", null, 400);
        }

        $newStatus = ($action === 'aktifkan') ? 'aktif' : 'ditolak';

        $stmt = $pdo->prepare("UPDATE users SET status_akun = ?, updated_at = NOW() WHERE id = ?");
        $stmt->execute([$newStatus, $userId]);

        $msg = ($action === 'aktifkan') 
            ? "Akun warga berhasil diverifikasi dan diaktifkan" 
            : "Pendaftaran akun warga ditolak";

        responseJson(true, $msg, [
            'user_id' => $userId,
            'status_akun' => $newStatus
        ]);
    } else {
        responseJson(false, "Method tidak didukung", null, 405);
    }

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
