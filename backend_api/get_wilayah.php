<?php
require_once "config.php";

$type = isset($_GET['type']) ? $_GET['type'] : 'all';

if ($type === 'kecamatan') {
    $res = mysqli_query($koneksi, "SELECT * FROM wilayah_kecamatan ORDER BY nama ASC");
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) { $data[] = $row; }
    responseJson(true, "Data kecamatan", $data);
} 
else if ($type === 'kelurahan') {
    $kecId = isset($_GET['kecamatan_id']) ? intval($_GET['kecamatan_id']) : 0;
    $query = $kecId > 0 ? "SELECT * FROM wilayah_kelurahan WHERE kecamatan_id = $kecId ORDER BY nama ASC" : "SELECT * FROM wilayah_kelurahan ORDER BY nama ASC";
    $res = mysqli_query($koneksi, $query);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) { $data[] = $row; }
    responseJson(true, "Data kelurahan", $data);
}
else if ($type === 'rw') {
    $kelId = isset($_GET['kelurahan_id']) ? intval($_GET['kelurahan_id']) : 0;
    $query = $kelId > 0 ? "SELECT * FROM wilayah_rw WHERE kelurahan_id = $kelId ORDER BY nomor_rw ASC" : "SELECT * FROM wilayah_rw ORDER BY nomor_rw ASC";
    $res = mysqli_query($koneksi, $query);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) { $data[] = $row; }
    responseJson(true, "Data RW", $data);
}
else if ($type === 'rt') {
    $rwId = isset($_GET['rw_id']) ? intval($_GET['rw_id']) : 0;
    $query = $rwId > 0 ? "SELECT * FROM wilayah_rt WHERE rw_id = $rwId ORDER BY nomor_rt ASC" : "SELECT * FROM wilayah_rt ORDER BY nomor_rt ASC";
    $res = mysqli_query($koneksi, $query);
    $data = [];
    while ($row = mysqli_fetch_assoc($res)) { $data[] = $row; }
    responseJson(true, "Data RT", $data);
}
else {
    // Return all for quick spinner binding
    $kecamatan = [];
    $kRes = mysqli_query($koneksi, "SELECT * FROM wilayah_kecamatan");
    while ($r = mysqli_fetch_assoc($kRes)) { $kecamatan[] = $r; }

    $kelurahan = [];
    $lRes = mysqli_query($koneksi, "SELECT * FROM wilayah_kelurahan");
    while ($r = mysqli_fetch_assoc($lRes)) { $kelurahan[] = $r; }

    $rw = [];
    $rwRes = mysqli_query($koneksi, "SELECT * FROM wilayah_rw");
    while ($r = mysqli_fetch_assoc($rwRes)) { $rw[] = $r; }

    $rt = [];
    $rtRes = mysqli_query($koneksi, "SELECT * FROM wilayah_rt");
    while ($r = mysqli_fetch_assoc($rtRes)) { $rt[] = $r; }

    responseJson(true, "Seluruh data hierarki wilayah", [
        "kecamatan" => $kecamatan,
        "kelurahan" => $kelurahan,
        "rw" => $rw,
        "rt" => $rt
    ]);
}
?>