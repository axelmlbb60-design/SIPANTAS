<?php
require_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'GET') {
        $userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
        $status = isset($_GET['status']) ? trim($_GET['status']) : '';

        $where = [];
        $params = [];

        if ($userId > 0) {
            $where[] = "l.user_id = ?";
            $params[] = $userId;
        }

        if (!empty($status)) {
            $where[] = "l.status = ?";
            $params[] = $status;
        }

        $whereSql = !empty($where) ? "WHERE " . implode(" AND ", $where) : "";

        $sql = "SELECT 
                    l.*,
                    u.nama_lengkap,
                    u.nik,
                    u.no_hp
                FROM laporan_warga l
                LEFT JOIN users u ON l.user_id = u.id
                $whereSql
                ORDER BY l.created_at DESC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $laporan = $stmt->fetchAll();

        foreach ($laporan as &$item) {
            if (!empty($item['foto_bukti'])) {
                $item['foto_bukti_url'] = getBaseUrl() . '/' . $item['foto_bukti'];
            }
        }

        responseJson(true, "Daftar laporan warga berhasil dimuat", $laporan);

    } elseif ($method === 'POST') {
        $action = isset($_POST['action']) ? trim($_POST['action']) : 'create';

        if ($action === 'create') {
            $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
            $judul = isset($_POST['judul']) ? trim($_POST['judul']) : '';
            $kategori = isset($_POST['kategori']) ? trim($_POST['kategori']) : 'Infrastruktur';
            $deskripsi = isset($_POST['deskripsi']) ? trim($_POST['deskripsi']) : '';
            $lokasi = isset($_POST['lokasi']) ? trim($_POST['lokasi']) : '';

            if ($userId <= 0 || empty($judul) || empty($deskripsi)) {
                responseJson(false, "User ID, judul, dan deskripsi laporan wajib diisi", null, 400);
            }

            $fotoBuktiPath = null;
            if (isset($_FILES['foto_bukti']) && $_FILES['foto_bukti']['error'] === UPLOAD_ERR_OK) {
                $file = $_FILES['foto_bukti'];
                $maxSize = 2 * 1024 * 1024; // 2 MB
                if ($file['size'] > $maxSize) {
                    responseJson(false, "Ukuran foto bukti melebihi batas maksimal 2MB", null, 400);
                }

                $ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
                $allowed = ['jpg', 'jpeg', 'png'];
                if (!in_array($ext, $allowed)) {
                    responseJson(false, "Format foto bukti tidak didukung (harus JPG atau PNG)", null, 400);
                }

                $uploadDir = __DIR__ . '/uploads/laporan';
                if (!is_dir($uploadDir)) {
                    mkdir($uploadDir, 0777, true);
                }

                $filename = 'laporan_' . $userId . '_' . time() . '.' . $ext;
                $targetFile = $uploadDir . '/' . $filename;

                if (move_uploaded_file($file['tmp_name'], $targetFile)) {
                    $fotoBuktiPath = 'uploads/laporan/' . $filename;
                }
            }

            $stmt = $pdo->prepare("INSERT INTO laporan_warga (user_id, judul, kategori, deskripsi, lokasi, foto_bukti, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'terkirim', NOW(), NOW())");
            $stmt->execute([$userId, $judul, $kategori, $deskripsi, $lokasi, $fotoBuktiPath]);
            $id = $pdo->lastInsertId();

            responseJson(true, "Laporan warga berhasil dikirim", [
                'id' => $id,
                'judul' => $judul,
                'status' => 'terkirim',
                'foto_bukti' => $fotoBuktiPath ? getBaseUrl() . '/' . $fotoBuktiPath : null
            ], 201);

        } elseif ($action === 'update_status') {
            $id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $status = isset($_POST['status']) ? trim($_POST['status']) : '';
            $tanggapan = isset($_POST['tanggapan']) ? trim($_POST['tanggapan']) : '';

            if ($id <= 0 || empty($status)) {
                responseJson(false, "ID laporan dan status wajib diisi", null, 400);
            }

            $stmt = $pdo->prepare("UPDATE laporan_warga SET status = ?, tanggapan_petugas = ?, updated_at = NOW() WHERE id = ?");
            $stmt->execute([$status, $tanggapan, $id]);

            responseJson(true, "Tanggapan dan status laporan berhasil diperbarui");

        } elseif ($action === 'delete') {
            $id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;

            $stmt = $pdo->prepare("DELETE FROM laporan_warga WHERE id = ? AND (user_id = ? OR ? = 0)");
            $stmt->execute([$id, $userId, $userId]);

            responseJson(true, "Laporan berhasil dihapus");
        } else {
            responseJson(false, "Aksi laporan tidak dikenali", null, 400);
        }
    } else {
        responseJson(false, "Method tidak didukung", null, 405);
    }
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
