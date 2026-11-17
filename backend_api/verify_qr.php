<?php
require_once 'config.php';

$token = isset($_GET['token']) ? trim($_GET['token']) : (isset($_POST['token']) ? trim($_POST['token']) : '');

if (empty($token)) {
    responseJson(false, "Kode QR / Token verifikasi wajib disertakan", null, 400);
}

try {
    $sql = "SELECT 
                p.id,
                p.nomor_surat,
                p.jenis_surat,
                p.keperluan,
                p.status,
                p.qr_code_token,
                p.tanggal_selesai,
                p.created_at,
                u.nama_lengkap,
                u.nik,
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
            WHERE p.qr_code_token = ?";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$token]);
    $letter = $stmt->fetch();

    if (!$letter) {
        responseJson(false, "Surat dengan kode verifikasi ini TIDAK DITEMUKAN atau TIDAK VALID dalam sistem SIPANTAS", [
            'is_valid' => false,
            'token' => $token
        ], 404);
    }

    if ($letter['status'] !== 'selesai') {
        responseJson(false, "Surat ditemukan namun statusnya belum selesai disahkan (" . $letter['status'] . ")", [
            'is_valid' => false,
            'status' => $letter['status']
        ], 400);
    }

    responseJson(true, "Dokumen ASLI dan TERVERIFIKASI secara digital oleh SIPANTAS", [
        'is_valid' => true,
        'nomor_surat' => $letter['nomor_surat'],
        'jenis_surat' => $letter['jenis_surat'],
        'nama_pemohon' => $letter['nama_lengkap'],
        'nik_masked' => substr($letter['nik'], 0, 6) . '******' . substr($letter['nik'], -4),
        'keperluan' => $letter['keperluan'],
        'wilayah' => "RT {$letter['nomor_rt']} / RW {$letter['nomor_rw']}, Kel. {$letter['nama_kelurahan']}, Kec. {$letter['nama_kecamatan']}",
        'tanggal_pengesahan' => $letter['tanggal_selesai'],
        'qr_token' => $letter['qr_code_token']
    ]);

} catch (PDOException $e) {
    responseJson(false, "Database error: " . $e->getMessage(), null, 500);
}
