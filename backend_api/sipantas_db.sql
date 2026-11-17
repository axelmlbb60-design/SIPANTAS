-- Database: sipantas_db
-- SIPANTAS (Sistem Pelayanan Administrasi Terpadu Antar Sektor)

CREATE DATABASE IF NOT EXISTS `sipantas_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `sipantas_db`;

-- 1. Tabel Wilayah Kecamatan
CREATE TABLE IF NOT EXISTS `wilayah_kecamatan` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `nama` VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- 2. Tabel Wilayah Kelurahan
CREATE TABLE IF NOT EXISTS `wilayah_kelurahan` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `kecamatan_id` INT NOT NULL,
    `nama` VARCHAR(100) NOT NULL,
    FOREIGN KEY (`kecamatan_id`) REFERENCES `wilayah_kecamatan`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Tabel Wilayah RW
CREATE TABLE IF NOT EXISTS `wilayah_rw` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `kelurahan_id` INT NOT NULL,
    `nomor_rw` VARCHAR(10) NOT NULL,
    FOREIGN KEY (`kelurahan_id`) REFERENCES `wilayah_kelurahan`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Tabel Wilayah RT
CREATE TABLE IF NOT EXISTS `wilayah_rt` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `rw_id` INT NOT NULL,
    `nomor_rt` VARCHAR(10) NOT NULL,
    FOREIGN KEY (`rw_id`) REFERENCES `wilayah_rw`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Tabel Pengguna / Users
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `nik` VARCHAR(16) NOT NULL UNIQUE,
    `nama_lengkap` VARCHAR(100) NOT NULL,
    `no_hp` VARCHAR(20) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` ENUM('warga', 'rtrw', 'kecamatan', 'admin') DEFAULT 'warga',
    `kecamatan_id` INT DEFAULT NULL,
    `kelurahan_id` INT DEFAULT NULL,
    `rw_id` INT DEFAULT NULL,
    `rt_id` INT DEFAULT NULL,
    `status_akun` ENUM('menunggu_verifikasi', 'aktif', 'proses_pindah') DEFAULT 'menunggu_verifikasi',
    `tipe_penduduk` ENUM('tetap', 'sementara_kos') DEFAULT 'tetap',
    `alamat_ktp` TEXT,
    `alamat_domisili` TEXT,
    `foto_ktp` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`kecamatan_id`) REFERENCES `wilayah_kecamatan`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`kelurahan_id`) REFERENCES `wilayah_kelurahan`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`rw_id`) REFERENCES `wilayah_rw`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`rt_id`) REFERENCES `wilayah_rt`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. Tabel Pengajuan Surat
CREATE TABLE IF NOT EXISTS `pengajuan_surat` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `nomor_surat` VARCHAR(50) UNIQUE,
    `user_id` INT NOT NULL,
    `jenis_surat` VARCHAR(100) NOT NULL,
    `keperluan` TEXT NOT NULL,
    `tujuan_wilayah` ENUM('ktp_asli', 'tempat_kos') DEFAULT 'ktp_asli',
    `status` ENUM('menunggu_rt', 'disetujui_rt', 'ditolak_rt', 'menunggu_kecamatan', 'selesai', 'ditolak_kecamatan') DEFAULT 'menunggu_rt',
    `tanda_tangan_rt` TEXT DEFAULT NULL,
    `catatan_penolakan` TEXT DEFAULT NULL,
    `qr_code_token` VARCHAR(100) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Tabel Jejak Audit (Audit Trail)
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `pengajuan_id` INT NOT NULL,
    `action` VARCHAR(50) NOT NULL,
    `actor_id` INT DEFAULT NULL,
    `actor_name` VARCHAR(100) NOT NULL,
    `actor_role` VARCHAR(50) NOT NULL,
    `notes` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`pengajuan_id`) REFERENCES `pengajuan_surat`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Tabel Pemesanan Antrean
CREATE TABLE IF NOT EXISTS `antrean_layanan` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `nomor_tiket` VARCHAR(20) UNIQUE NOT NULL,
    `user_id` INT NOT NULL,
    `layanan` VARCHAR(100) NOT NULL,
    `tanggal` DATE NOT NULL,
    `sesi` VARCHAR(50) NOT NULL,
    `status` ENUM('menunggu', 'selesai', 'batal') DEFAULT 'menunggu',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Tabel Laporan Pengaduan Warga
CREATE TABLE IF NOT EXISTS `laporan_warga` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `judul` VARCHAR(150) NOT NULL,
    `kategori` VARCHAR(50) NOT NULL,
    `deskripsi` TEXT NOT NULL,
    `lokasi` TEXT,
    `foto_bukti` VARCHAR(255) DEFAULT NULL,
    `status` ENUM('menunggu', 'diproses', 'selesai') DEFAULT 'menunggu',
    `tanggapan_petugas` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ========================================================
-- DATA AWAL / SEED DATA UNTUK PENGUJIAN
-- ========================================================

-- Insert Wilayah
INSERT INTO `wilayah_kecamatan` (`id`, `nama`) VALUES 
(1, 'Kecamatan Biringkanaya'),
(2, 'Kecamatan Tamalanrea')
ON DUPLICATE KEY UPDATE `nama`=VALUES(`nama`);

INSERT INTO `wilayah_kelurahan` (`id`, `kecamatan_id`, `nama`) VALUES 
(1, 1, 'Kelurahan Paccerakkang'),
(2, 1, 'Kelurahan Daya'),
(3, 2, 'Kelurahan Tamalanrea Indah')
ON DUPLICATE KEY UPDATE `nama`=VALUES(`nama`);

INSERT INTO `wilayah_rw` (`id`, `kelurahan_id`, `nomor_rw`) VALUES 
(1, 1, '01'),
(2, 1, '02'),
(3, 1, '03')
ON DUPLICATE KEY UPDATE `nomor_rw`=VALUES(`nomor_rw`);

INSERT INTO `wilayah_rt` (`id`, `rw_id`, `nomor_rt`) VALUES 
(1, 2, '01'),
(2, 2, '02'),
(3, 2, '03')
ON DUPLICATE KEY UPDATE `nomor_rt`=VALUES(`nomor_rt`);

-- Insert Users Demo (Password default teks biasa atau md5: warga123, rtrw123, kecamatan123, admin123)
INSERT INTO `users` (`id`, `nik`, `nama_lengkap`, `no_hp`, `password`, `role`, `kecamatan_id`, `kelurahan_id`, `rw_id`, `rt_id`, `status_akun`, `tipe_penduduk`, `alamat_ktp`, `alamat_domisili`) VALUES
(1, '7371010101010001', 'Budi Santoso', '081234567890', 'warga123', 'warga', 1, 1, 2, 3, 'aktif', 'tetap', 'Jl. Paccerakkang No. 12', 'Jl. Paccerakkang No. 12'),
(2, '7371010101010002', 'Siti Rahmawati', '081298765432', 'warga123', 'warga', 1, 1, 2, 3, 'menunggu_verifikasi', 'sementara_kos', 'Jl. Asal KTP Kab. Bone', 'Kost Melati Kamar 05 RT 03 / RW 02 Paccerakkang'),
(3, '7371010202020001', 'Ahmad Fauzi (Ketua RT 03)', '082155566677', 'rtrw123', 'rtrw', 1, 1, 2, 3, 'aktif', 'tetap', 'Jl. Paccerakkang RT 03 / RW 02', 'Jl. Paccerakkang RT 03 / RW 02'),
(4, '7371010303030001', 'Drs. H. Mulyadi (Kecamatan)', '081377788899', 'kecamatan123', 'kecamatan', 1, 1, NULL, NULL, 'aktif', 'tetap', 'Kantor Kecamatan Biringkanaya', 'Kantor Kecamatan Biringkanaya'),
(5, '7371010404040001', 'Administrator Sistem', '081100011122', 'admin123', 'admin', NULL, NULL, NULL, NULL, 'aktif', 'tetap', 'Makassar', 'Makassar')
ON DUPLICATE KEY UPDATE `nama_lengkap`=VALUES(`nama_lengkap`);

-- Insert Sample Pengajuan Surat
INSERT INTO `pengajuan_surat` (`id`, `nomor_surat`, `user_id`, `jenis_surat`, `keperluan`, `tujuan_wilayah`, `status`, `tanda_tangan_rt`, `qr_code_token`) VALUES
(1, 'SKD-0311-007', 1, 'Surat Keterangan Domisili', 'Persyaratan melamar pekerjaan dan administrasi perbankan', 'ktp_asli', 'menunggu_rt', NULL, NULL),
(2, 'SP-0311-002', 1, 'Surat Pengantar RT / RW', 'Pengurusan pembuatan Kartu Keluarga baru di Kelurahan', 'ktp_asli', 'selesai', 'data:image/png;base64,sample_signature', 'SIPANTAS-VERIF-73710101-002')
ON DUPLICATE KEY UPDATE `status`=VALUES(`status`);

-- Insert Sample Audit Log
INSERT INTO `audit_log` (`pengajuan_id`, `action`, `actor_name`, `actor_role`, `notes`) VALUES
(1, 'DIAJUKAN', 'Budi Santoso', 'Warga', 'Permohonan surat diajukan mandiri via aplikasi SIPANTAS.'),
(2, 'DIAJUKAN', 'Budi Santoso', 'Warga', 'Pengajuan surat pengantar online.'),
(2, 'DISETUJUI_RT', 'Ahmad Fauzi (Ketua RT 03)', 'RT / RW', 'Data warga valid dan disetujui dengan tanda tangan digital.'),
(2, 'SELESAI', 'Drs. H. Mulyadi', 'Kecamatan', 'Surat telah diverifikasi dan QR Code keaslian diterbitkan.');

-- Insert Sample Antrean
INSERT INTO `antrean_layanan` (`nomor_tiket`, `user_id`, `layanan`, `tanggal`, `sesi`, `status`) VALUES
('A-012', 1, 'Legalisir Berkas Kependudukan', CURDATE(), 'Sesi 1 (08:30 - 10:00 WITA)', 'menunggu')
ON DUPLICATE KEY UPDATE `status`=VALUES(`status`);

-- Insert Sample Laporan
INSERT INTO `laporan_warga` (`user_id`, `judul`, `kategori`, `deskripsi`, `lokasi`, `status`) VALUES
(1, 'Lampu Jalan Padam di Lorong 2', 'Infrastruktur', 'Lampu penerangan jalan padam sejak 2 hari lalu di dekat pos ronda.', 'RT 03 / RW 02 Paccerakkang', 'menunggu')
ON DUPLICATE KEY UPDATE `status`=VALUES(`status`);
