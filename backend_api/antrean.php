<?php
require_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        $userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
        $tanggal = isset($_GET['tanggal']) ? trim($_GET['tanggal']) : '';

        $where = [];
        $params = [];

        if ($userId > 0) {
            $where[] = "a.user_id = ?";
            $params[] = $userId;
        }

        if (!empty($tanggal)) {
            $where[] = "a.tanggal = ?";
            $params[] = $tanggal;
        }

        $whereSql = !empty($where) ? "WHERE " . implode(" AND ", $where) : "";

        $sql = "SELECT 
                    a.*,
                    u.nama_lengkap,
                    u.nik,
                    u.no_hp
                FROM antrean_layanan a
                LEFT JOIN users u ON a.user_id = u.id
                $whereSql
                ORDER BY a.tanggal DESC, a.nomor_tiket ASC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $antrean = $stmt->fetchAll();

        responseJson(true, "Daftar antrean berhasil dimuat", $antrean);

    } elseif ($method === 'POST') {
        $action = isset($_POST['action']) ? trim($_POST['action']) : 'create';

        if ($action === 'create') {
            $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
            $layanan = isset($_POST['layanan']) ? trim($_POST['layanan']) : 'Layanan Surat Kecamatan';
            $tanggal = isset($_POST['tanggal']) ? trim($_POST['tanggal']) : date('Y-m-d');
            $sesi = isset($_POST['sesi']) ? trim($_POST['sesi']) : 'Pagi (08:30 - 11:30)';

            if ($userId <= 0) {
                responseJson(false, "User ID wajib diisi", null, 400);
            }

            // Generate next ticket number for the day
            $stmtCount = $pdo->prepare("SELECT COUNT(*) FROM antrean_layanan WHERE tanggal = ?");
            $stmtCount->execute([$tanggal]);
            $count = (int)$stmtCount->fetchColumn();
            $nomorTiket = 'A-' . str_pad($count + 1, 3, '0', STR_PAD_LEFT);

            $stmt = $pdo->prepare("INSERT INTO antrean_layanan (nomor_tiket, user_id, layanan, tanggal, sesi, status, created_at) VALUES (?, ?, ?, ?, ?, 'menunggu', NOW())");
            $stmt->execute([$nomorTiket, $userId, $layanan, $tanggal, $sesi]);
            $id = $pdo->lastInsertId();

            responseJson(true, "Tiket antrean berhasil dibuat", [
                'id' => $id,
                'nomor_tiket' => $nomorTiket,
                'layanan' => $layanan,
                'tanggal' => $tanggal,
                'sesi' => $sesi,
                'status' => 'menunggu'
            ], 201);

        } elseif ($action === 'cancel') {
            $id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;

            if ($id <= 0) {
                responseJson(false, "ID antrean tidak valid", null, 400);
            }

            $stmt = $pdo->prepare("UPDATE antrean_layanan SET status = 'dibatalkan' WHERE id = ? AND (user_id = ? OR ? = 0)");
            $stmt->execute([$id, $userId, $userId]);

            responseJson(true, "Nomor antrean berhasil dibatalkan");

        } elseif ($action === 'update_status') {
            $id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $status = isset($_POST['status']) ? trim($_POST['status']) : ''; // 'menunggu', 'dipanggil', 'selesai', 'dibatalkan'

            if ($id <= 0 || empty($status)) {
                responseJson(false, "ID dan status wajib diisi", null, 400);
            }

            $stmt = $pdo->prepare("UPDATE antrean_layanan SET status = ? WHERE id = ?");
            $stmt->execute([$status, $id]);

            responseJson(true, "Status antrean berhasil diperbarui");
        } else {
            responseJson(false, "Aksi antrean tidak dikenali", null, 400);
        }
    } else {
        responseJson(false, "Method tidak didukung", null, 405);
    }
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
