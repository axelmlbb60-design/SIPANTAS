<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    responseJson(false, "Method tidak diizinkan", null, 405);
}

$userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
$fcmToken = isset($_POST['fcm_token']) ? trim($_POST['fcm_token']) : '';

if ($userId <= 0 || empty($fcmToken)) {
    responseJson(false, "User ID dan FCM Token wajib diisi", null, 400);
}

try {
    $stmt = $pdo->prepare("UPDATE users SET fcm_token = ?, updated_at = NOW() WHERE id = ?");
    $stmt->execute([$fcmToken, $userId]);

    responseJson(true, "Token FCM berhasil diperbarui", [
        'user_id' => $userId,
        'fcm_token' => $fcmToken
    ]);
} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
