<?php
require_once 'config.php';

$method = $_SERVER['REQUEST_METHOD'];

try {
    if ($method === 'POST') {
        $type = isset($_POST['type']) ? trim($_POST['type']) : ''; // 'kelurahan', 'rw', 'rt'
        $action = isset($_POST['action']) ? trim($_POST['action']) : 'create'; // 'create', 'update', 'delete'

        if ($type === 'kelurahan') {
            if ($action === 'create') {
                $kecamatanId = (int)$_POST['kecamatan_id'];
                $nama = trim($_POST['nama']);
                $stmt = $pdo->prepare("INSERT INTO wilayah_kelurahan (kecamatan_id, nama) VALUES (?, ?)");
                $stmt->execute([$kecamatanId, $nama]);
                responseJson(true, "Kelurahan berhasil ditambahkan", ['id' => $pdo->lastInsertId()]);
            } elseif ($action === 'delete') {
                $id = (int)$_POST['id'];
                $stmt = $pdo->prepare("DELETE FROM wilayah_kelurahan WHERE id = ?");
                $stmt->execute([$id]);
                responseJson(true, "Kelurahan berhasil dihapus");
            }
        } elseif ($type === 'rw') {
            if ($action === 'create') {
                $kelurahanId = (int)$_POST['kelurahan_id'];
                $nomor = trim($_POST['nomor_rw']);
                $stmt = $pdo->prepare("INSERT INTO wilayah_rw (kelurahan_id, nomor_rw) VALUES (?, ?)");
                $stmt->execute([$kelurahanId, $nomor]);
                responseJson(true, "RW berhasil ditambahkan", ['id' => $pdo->lastInsertId()]);
            } elseif ($action === 'delete') {
                $id = (int)$_POST['id'];
                $stmt = $pdo->prepare("DELETE FROM wilayah_rw WHERE id = ?");
                $stmt->execute([$id]);
                responseJson(true, "RW berhasil dihapus");
            }
        } elseif ($type === 'rt') {
            if ($action === 'create') {
                $rwId = (int)$_POST['rw_id'];
                $nomor = trim($_POST['nomor_rt']);
                $stmt = $pdo->prepare("INSERT INTO wilayah_rt (rw_id, nomor_rt) VALUES (?, ?)");
                $stmt->execute([$rwId, $nomor]);
                responseJson(true, "RT berhasil ditambahkan", ['id' => $pdo->lastInsertId()]);
            } elseif ($action === 'delete') {
                $id = (int)$_POST['id'];
                $stmt = $pdo->prepare("DELETE FROM wilayah_rt WHERE id = ?");
                $stmt->execute([$id]);
                responseJson(true, "RT berhasil dihapus");
            }
        } else {
            responseJson(false, "Tipe wilayah tidak valid", null, 400);
        }
    }
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
