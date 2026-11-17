# Dokumentasi Fitur Utama, Database, dan REST API SIPANTAS

**SIPANTAS** (Sistem Informasi Pelayanan Administrasi Terpadu Masyarakat) adalah platform digital terintegrasi yang menghubungkan Warga, RT/RW, dan Pemerintah Kelurahan/Kecamatan dalam pelayanan surat pengantar, kependudukan, pelaporan, dan antrean loket secara transparan, akuntabel, dan digital.

---

## 1. Arsitektur & Teknologi

- **Mobile Client:** Android Native (Kotlin), SDK 35 (minSdk 24), Material Design 3, ViewBinding, Retrofit 2, Gson Converter, OkHttp 3 Logging Interceptor, ZXing QR Code Core.
- **Backend API:** PHP (PDO MySQL), CORS-enabled, RESTful JSON response architecture (`C:\xampp\htdocs\api_sipantas/` dan tersinkronisasi di folder `backend_api/`).
- **Database:** MySQL / MariaDB (`sipantas_db`).
- **Host / Server:** Apache XAMPP (`http://localhost/api_sipantas/`, alias Android Emulator: `http://10.0.2.2/api_sipantas/`, HP fisik: `http://<IP-LAN>/api_sipantas/`).

---

## 2. Struktur Database (`sipantas_db.sql`)

1. **`users`**: Menyimpan identitas warga dan petugas dengan validasi NIK 16-digit, email, no HP, sandi terenkripsi (BCrypt), role (`warga`, `rtrw`, `kecamatan`, `admin`), status akun (`menunggu_verifikasi`, `aktif`, `proses_pindah`, `ditolak`), tipe penduduk (`tetap`, `sementara`), alamat KTP, alamat domisili, foto KTP, dan relasi wilayah.
2. **`wilayah_kecamatan`**, **`wilayah_kelurahan`**, **`wilayah_rw`**, **`wilayah_rt`**: Struktur wilayah berjenjang untuk pembagian yurisdiksi RT/RW dan Kelurahan/Kecamatan.
3. **`pengajuan_surat`**: Data permohonan surat warga dengan atribut nomor surat unik, jenis surat, keperluan, dokumen lampiran, tujuan wilayah (`ktp_asli` / `tempat_kos`), status pengajuan (`menunggu_rt`, `disetujui_rt`, `ditolak_rt`, `menunggu_kecamatan`, `selesai`, `ditolak_kecamatan`), tanda tangan digital RT, catatan penolakan wajib, token QR Code, dan tanggal pengesahan.
4. **`audit_log`**: Catatan jejak audit tak terbantahkan (siapa yang mengajukan/menyetujui/menolak, kapan waktu tepatnya, peran aktor, dan catatan/alasan penolakan).
5. **`antrean_layanan`**: Nomor tiket antrean loket pelayanan kantor kecamatan/kelurahan (`A-001`, `A-002`, dst.) per tanggal dan per sesi waktu.
6. **`laporan_warga`**: Saluran aspirasi dan pengaduan warga dengan foto bukti kendala (maks 2MB), kategori, lokasi, status penanganan, dan tanggapan resmi petugas.

---

## 3. Fitur Utama & Bisnis Proses (Di Luar CRUD Biasa)

### A. Untuk Warga
1. **Registrasi Mandiri dengan Validasi Ketat:**
   - Validasi NIK wajib tepat 16 digit angka.
   - Pilihan wilayah berjenjang (Kecamatan → Kelurahan → RT/RW).
   - Unggah foto KTP / dokumen dengan pembatasan ukuran berkas maksimal 2MB (format JPG/PNG/PDF).
2. **Login NIK & Kata Sandi:**
   - Mendukung autentikasi berbasis role.
   - Dilengkapi tombol cepat demo account dan pemulihan akun.
3. **Pengajuan Surat Pengantar Digital:**
   - Pemohon dapat mengajukan berbagai jenis surat (Surat Keterangan Domisili, Surat Pengantar RT/RW, SKTM, SKU).
   - **Fitur Penduduk Sementara (Kos/Kontrak):** Dapat memilih tujuan surat: diajukan ke wilayah sesuai KTP asli atau ke wilayah tempat kos/kontrak saat ini.
4. **Lapor Pindah Domisili:**
   - Warga dapat mengajukan permohonan pindah domisili yang secara otomatis mengubah status akun menjadi `proses_pindah` dan menerbitkan permohonan surat pindah.
5. **Banner Status Akun Real-Time:**
   - Tampil di bagian atas beranda:
     - Kuning/Amber: *Akun Menunggu Verifikasi RT*
     - Biru/Info: *Status: Proses Pindah Domisili*
     - Hijau: *Akun Terverifikasi*
6. **Profil & Ganti Kata Sandi:**
   - Form ganti kata sandi dengan validasi sandi saat ini dan verifikasi sandi baru minimal 6 karakter.

### B. Untuk RT / RW
1. **Verifikasi Warga Baru:**
   - Melihat daftar pendaftar baru yang menunggu verifikasi, mencocokkan data identitas terhadap foto dokumen KTP, lalu mengaktifkan atau menolak pendaftaran warga.
2. **Persetujuan Surat dengan Tanda Tangan Digital Kanvas (Digital Signature Canvas):**
   - RT/RW menandatangani surat secara langsung pada layar kanvas sentuh di aplikasi Android. Tanda tangan dienkode dan disimpan di server.
3. **Penolakan dengan Alasan Wajib:**
   - RT/RW tidak dapat menolak surat tanpa memberikan alasan penolakan tertulis yang jelas, yang akan langsung terbaca oleh warga.
4. **Badge Notifikasi Permohonan Masuk:**
   - Menghitung jumlah surat yang berstatus `menunggu_rt` dan menampilkan banner notifikasi di beranda RT/RW.
5. **Persetujuan Pelepasan & Penerimaan Warga Pindah:**
   - Memberikan surat pelepasan dan RT baru dapat menerima warga ke wilayah barunya.

### C. Untuk Kelurahan / Kecamatan & Admin
1. **Pengesahan Surat Tingkat Kecamatan & Nomor Antrean:**
   - Memeriksa surat yang telah disetujui RT/RW dan mengesahkan dokumen resmi.
2. **Penerbitan Kode Verifikasi QR Resmi:**
   - Saat surat disetujui kecamatan (`selesai`), sistem otomatis menghasilkan token kriptografis unik (`SIPANTAS-YYYYMMDD-XXXXX-HASH`) dan membuat QR Code digital.
3. **Dashboard Statistik Wilayah:**
   - Grafik & ringkasan statistik total surat yang diajukan, surat selesai, surat ditolak, pengajuan per jenis surat, serta persebaran pengajuan per kelurahan.
4. **Reset Kata Sandi Akun RT/RW:**
   - Pihak Kecamatan berwenang mereset password akun RT/RW di bawah yurisdiksinya jika terjadi lupa sandi.
5. **Kelola Data Wilayah (CRUD Wilayah):**
   - Penambahan dan pengelolaan data Kelurahan, RW, dan RT.

### D. Fitur Lintas Role, Keamanan & Reliabilitas
1. **Verifikasi Keaslian Dokumen Digital:**
   - Pindai atau cek kode token QR via endpoint `verify_qr.php` untuk membuktikan keaslian dokumen tanpa perlu cap basah manual.
2. **Jejak Audit (Audit Log) Lengkap:**
   - Riwayat langkah-langkah proses surat dari pendaftaran, tanda tangan RT, hingga pengesahan kecamatan tercatat detail (aktor, waktu, status, catatan).
3. **Batas Ukuran Upload File 2 MB:**
   - Divalidasi ganda di sisi klien Android dan di sisi backend PHP.
4. **Penanganan Mode Offline & Fleksibilitas IP Server:**
   - Aplikasi menampilkan dialog ramah saat offline tanpa mengalami crash dan menyediakan menu pengaturan IP/Base URL server langsung dari UI untuk memudahkan pengujian di perangkat fisik.

---

## 4. Daftar Endpoint REST API

| Endpoint | Method | Fungsi | Parameter Utama |
|---|---|---|---|
| `/login.php` | POST | Login pengguna semua role | `nik`, `password` |
| `/register.php` | POST | Registrasi warga baru (Multi-part) | `nik`, `nama_lengkap`, `password`, `no_hp`, `tipe_penduduk`, `foto_ktp` (maks 2MB), `alamat_ktp`, `alamat_domisili`, `kecamatan_id`, `kelurahan_id`, `rw_id`, `rt_id` |
| `/get_wilayah.php` | GET | Ambil data wilayah berjenjang | `type` (`kecamatan`/`kelurahan`/`rw`/`rt`), `parent_id` |
| `/pengajuan_create.php` | POST | Pengajuan surat baru warga | `user_id`, `jenis_surat`, `keperluan`, `tujuan_wilayah` (`ktp_asli`/`tempat_kos`), `dokumen_lampiran` (maks 2MB) |
| `/pengajuan_list.php` | GET | Daftar pengajuan | `user_id`, `role`, `status`, `rt_id`, `search` |
| `/pengajuan_detail.php` | GET | Detail pengajuan lengkap + Jejak Audit | `id` |
| `/pengajuan_action_rt.php` | POST | Persetujuan TTD kanvas / penolakan RT | `pengajuan_id`, `action` (`setujui`/`tolak`), `petugas_nama`, `signature_base64`, `alasan` (wajib jika tolak) |
| `/pengajuan_action_kecamatan.php` | POST | Pengesahan surat & terbitkan token QR | `pengajuan_id`, `action` (`setujui`/`tolak`), `petugas_nama`, `alasan` |
| `/verify_qr.php` | GET/POST | Verifikasi keabsahan dokumen QR | `token` |
| `/pengajuan_delete.php` | POST | Pembatalan pengajuan oleh warga | `pengajuan_id`, `user_id` |
| `/warga_verifikasi.php` | GET/POST | List & aktivasi pendaftaran warga baru oleh RT | `status`, `user_id`, `action` (`aktifkan`/`tolak`) |
| `/warga_pindah.php` | POST | Alur perpindahan domisili warga | `action` (`ajukan_pindah`/`terima_rt_baru`), `user_id`, `alamat_tujuan`, `alasan_pindah` |
| `/antrean.php` | GET/POST | CRUD pemesanan tiket antrean loket | `action` (`create`/`cancel`/`update_status`), `user_id`, `layanan`, `tanggal`, `sesi` |
| `/laporan.php` | GET/POST | CRUD aduan & pelaporan warga | `action` (`create`/`update_status`/`delete`), `user_id`, `judul`, `deskripsi`, `lokasi`, `foto_bukti` (maks 2MB) |
| `/dashboard_stats.php` | GET | Statistik surat & demografi kecamatan | *(None)* |
| `/change_password.php` | POST | Ganti password mandiri / reset oleh Kecamatan | `action` (`change`/`reset_by_admin`), `user_id`, `current_password`, `new_password` |
| `/wilayah_crud.php` | POST | CRUD data wilayah Kelurahan, RW, dan RT | `type`, `action`, `nama`/`nomor` |

---

## 5. Akun Demo untuk Pengujian Cepat

| Peran (Role) | NIK | Kata Sandi | Deskripsi |
|---|---|---|---|
| **Warga (Aktif)** | `7371010101010001` | `warga123` | Budi Santoso (RT 03 / RW 02, Kel. Daya) |
| **Warga (Baru)** | `7371010101010002` | `warga123` | Siti Rahmawati (Status: Menunggu Verifikasi) |
| **Ketua RT / RW** | `7371010202020001` | `rtrw123` | Ahmad Fauzi (Ketua RT 03 / RW 02) |
| **Petugas Kecamatan** | `7371010303030001` | `kecamatan123` | Drs. H. Mulyadi (Kecamatan Biringkanaya) |
| **Admin Sistem** | `7371010404040001` | `admin123` | Administrator Sistem SIPANTAS |
