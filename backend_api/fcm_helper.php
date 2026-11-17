<?php
// fcm_helper.php - Helper untuk pengiriman Push Notification Firebase & In-App Notification

function sendFirebaseNotification($fcmToken, $title, $body, $data = []) {
    if (empty($fcmToken)) {
        return false;
    }

    // FCM Server Key (Dapat diganti dengan Firebase Cloud Messaging Server Key project Anda)
    $serverKey = 'YOUR_FIREBASE_SERVER_KEY';

    $url = 'https://fcm.googleapis.com/fcm/send';

    $fields = [
        'to' => $fcmToken,
        'notification' => [
            'title' => $title,
            'body' => $body,
            'sound' => 'default',
            'badge' => '1',
            'channel_id' => 'sipantas_notif_channel'
        ],
        'data' => array_merge([
            'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
            'title' => $title,
            'message' => $body
        ], $data),
        'priority' => 'high'
    ];

    $headers = [
        'Authorization: key=' . $serverKey,
        'Content-Type: application/json'
    ];

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);

    $result = curl_exec($ch);
    curl_close($ch);

    return $result;
}

function createInAppNotification($pdo, $userId, $title, $message, $type = 'info', $dataId = null) {
    try {
        // 1. Simpan ke database notifikasi MySQL
        $stmt = $pdo->prepare("INSERT INTO notifikasi (user_id, judul, pesan, tipe, data_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, 0, NOW())");
        $stmt->execute([$userId, $title, $message, $type, $dataId]);

        // 2. Ambil token FCM pengguna jika ada
        $stmtToken = $pdo->prepare("SELECT fcm_token FROM users WHERE id = ?");
        $stmtToken->execute([$userId]);
        $fcmToken = $stmtToken->fetchColumn();

        if (!empty($fcmToken)) {
            sendFirebaseNotification($fcmToken, $title, $message, [
                'type' => $type,
                'data_id' => $dataId
            ]);
        }
        return true;
    } catch (Exception $e) {
        error_log("Error in createInAppNotification: " . $e->getMessage());
        return false;
    }
}
