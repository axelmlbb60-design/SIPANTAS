<?php
require_once 'config.php';

$userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
if ($userId <= 0) {
    responseJson(false, "User ID wajib diisi", null, 400);
}

try {
    $stmt = $pdo->prepare("SELECT * FROM notifikasi WHERE user_id = ? ORDER BY created_at DESC LIMIT 50");
    $stmt->execute([$userId]);
    $notifications = $stmt->fetchAll();

    // Mark as read
    $pdo->prepare("UPDATE notifikasi SET is_read = 1 WHERE user_id = ?")->execute([$userId]);

    responseJson(true, "Daftar notifikasi berhasil dimuat", $notifications);
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
