<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responseJson(false, "Method tidak diizinkan", null, 405);
}

$action = isset($_POST['action']) ? trim($_POST['action']) : 'change'; // 'change' or 'reset_by_admin'
$userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
$newPassword = isset($_POST['new_password']) ? trim($_POST['new_password']) : '';

if ($userId <= 0 || empty($newPassword)) {
    responseJson(false, "User ID dan password baru wajib diisi", null, 400);
}

if (strlen($newPassword) < 6) {
    responseJson(false, "Password baru minimal 6 karakter", null, 400);
}

try {
    $stmt = $pdo->prepare("SELECT id, password, role, nama_lengkap FROM users WHERE id = ?");
    $stmt->execute([$userId]);
    $user = $stmt->fetch();

    if (!$user) {
        responseJson(false, "Pengguna tidak ditemukan", null, 404);
    }

    if ($action === 'change') {
        $currentPassword = isset($_POST['current_password']) ? trim($_POST['current_password']) : '';
        if (empty($currentPassword)) {
            responseJson(false, "Password lama wajib diisi", null, 400);
        }

        if (!password_verify($currentPassword, $user['password'])) {
            responseJson(false, "Password saat ini salah", null, 400);
        }

        $hashed = password_hash($newPassword, PASSWORD_BCRYPT);
        $stmtUp = $pdo->prepare("UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?");
        $stmtUp->execute([$hashed, $userId]);

        responseJson(true, "Password berhasil diperbarui");

    } elseif ($action === 'reset_by_admin') {
        $adminId = isset($_POST['admin_id']) ? (int)$_POST['admin_id'] : 0;
        // Verify admin
        $stmtAdmin = $pdo->prepare("SELECT role FROM users WHERE id = ?");
        $stmtAdmin->execute([$adminId]);
        $adminRole = $stmtAdmin->fetchColumn();

        if ($adminRole !== 'kecamatan' && $adminRole !== 'admin') {
            responseJson(false, "Hanya Kecamatan atau Admin yang berhak mereset password akun RT/RW", null, 403);
        }

        $hashed = password_hash($newPassword, PASSWORD_BCRYPT);
        $stmtUp = $pdo->prepare("UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?");
        $stmtUp->execute([$hashed, $userId]);

        responseJson(true, "Password akun {$user['nama_lengkap']} berhasil direset oleh Kecamatan");

    } else {
        responseJson(false, "Aksi tidak dikenali", null, 400);
    }

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
