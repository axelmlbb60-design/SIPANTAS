<?php
require_once 'config.php';
require_once 'fcm_helper.php';

// Script Pengingat Antrean H-1: Otomatis mendeteksi warga yang memiliki antrean besok
try {
    // Ambil tanggal besok
    $besok = date('Y-m-d', strtotime('+1 day'));

    $sql = "SELECT a.*, u.nama_lengkap, u.fcm_token 
            FROM antrean_layanan a
            JOIN users u ON a.user_id = u.id
            WHERE a.tanggal = ? AND a.status = 'menunggu'";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$besok]);
    $antreanBesok = $stmt->fetchAll();

    $notifiedCount = 0;
    foreach ($antreanBesok as $item) {
        $title = "🔔 Pengingat Antrean Loket (Besok H-1)";
        $body = "Halo {$item['nama_lengkap']}, besok Anda memiliki jadwal kunjungan di {$item['layanan']} pada sesi {$item['sesi']}. Nomor Tiket: {$item['nomor_tiket']}. Jangan lupa membawa KTP asli!";

        createInAppNotification($pdo, $item['user_id'], $title, $body, 'antrean_h_min_1', $item['id']);
        $notifiedCount++;
    }

    responseJson(true, "Berhasil mengirim pengingat antrean H-1 kepada $notifiedCount warga", [
        'tanggal_target' => $besok,
        'jumlah_diingatkan' => $notifiedCount
    ]);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
