<?php
require_once "config.php";

$res = mysqli_query($koneksi, "DESCRIBE notifikasi");
while ($r = mysqli_fetch_assoc($res)) {
    echo $r['Field'] . " - " . $r['Type'] . "\n";
}
?>
