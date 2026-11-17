<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Content-Type: application/json; charset=UTF-8");

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$host = "localhost";
$user = "root";
$pass = "";
$db   = "sipantas_db";

try {
    $koneksi = @mysqli_connect($host, $user, $pass, $db);
    if (!$koneksi) {
        echo json_encode([
            "success" => false,
            "message" => "Database MySQL belum berjalan atau database 'sipantas_db' belum di-import. Pastikan MySQL di XAMPP sudah aktif dan sipantas_db.sql telah di-import!"
        ]);
        exit;
    }
    mysqli_set_charset($koneksi, "utf8mb4");

    // Also initialize PDO for scripts utilizing PDO
    $pdo = new PDO("mysql:host=$host;dbname=$db;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Koneksi ke MySQL gagal: " . $e->getMessage()
    ]);
    exit;
}

function responseJson($success, $message, $data = null) {
    $res = [
        "success" => $success,
        "message" => $message
    ];
    if ($data !== null) {
        $res["data"] = $data;
    }
    echo json_encode($res, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}
?>
