# Kasir Mini (Spring Boot + Hibernate + PostgreSQL)

Aplikasi web kasir sederhana: kelola kategori dan produk (CRUD), lakukan transaksi
(stok berkurang otomatis, gagal = rollback), lihat riwayat dan cetak struk. Tersedia pula chatbot Spring AI untuk pencarian produk, pemeriksaan stok, tambah produk/stok, dan penghapusan produk dengan konfirmasi.

Teknologi: Java 17, Spring Boot 3.4, Spring AI 1.0 (Naraya OpenAI-compatible API), Spring MVC, Spring Data JPA/Hibernate, Thymeleaf, PostgreSQL.

## 1. Siapkan PostgreSQL

Pastikan service PostgreSQL berjalan. Panduan ini menggunakan PostgreSQL lokal dengan konfigurasi berikut:

```text
Host     : localhost
Port     : 5432
Database : kasir
Username : admin
Password : admin
```

### Setup database melalui PowerShell (tanpa pgAdmin)

Jika perintah `psql` sudah bisa dipanggil langsung seperti di bawah ini, gunakan panduan berikut:

```powershell
psql --version
```

Konfigurasi PostgreSQL yang digunakan:

```text
Administrator PostgreSQL : postgres
Password administrator   : admin
User aplikasi            : admin
Password aplikasi        : admin
Database                 : kasir
Host                     : localhost
Port                     : 5432
```

Buat atau reset user aplikasi `admin`:

```powershell
psql -U postgres -h localhost -p 5432 -d postgres -c "DO `$`$ BEGIN IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'admin') THEN ALTER ROLE admin WITH LOGIN PASSWORD 'admin'; ELSE CREATE ROLE admin WITH LOGIN PASSWORD 'admin'; END IF; END `$`$;"
```

Masukkan password user `postgres` saat diminta. Hapus database lama jika ingin memulai dari kondisi bersih, lalu buat ulang database `kasir`:

```powershell
psql -U postgres -h localhost -p 5432 -d postgres -c "DROP DATABASE IF EXISTS kasir WITH (FORCE);"
psql -U postgres -h localhost -p 5432 -d postgres -c "CREATE DATABASE kasir OWNER admin;"
```

> Perintah `DROP DATABASE` menghapus seluruh data lama. Lewati perintah tersebut jika ingin mempertahankan data.

Berikan akses ke schema `public`:

```powershell
psql -U postgres -h localhost -p 5432 -d kasir -c "GRANT ALL ON SCHEMA public TO admin; ALTER SCHEMA public OWNER TO admin;"
```

Tes login menggunakan user aplikasi:

```powershell
psql -U admin -h localhost -p 5432 -d kasir
```

Masukkan password `admin`. Jika muncul prompt `kasir=#`, koneksi berhasil. Keluar dari psql dengan:

```sql
\q
```

Jika `psql` belum dikenali, tambahkan folder PostgreSQL ke PATH untuk sesi PowerShell saat ini:

```powershell
$env:Path = "C:\Program Files\PostgreSQL\18\bin;$env:Path"
```

Sesuaikan angka `18` jika versi PostgreSQL kamu berbeda.

## 2. Jalankan

Jalankan perintah dari folder yang berisi `pom.xml`:

```powershell
cd C:\Users\Keino\Downloads\kasir-spring\kasir-spring
mvn spring-boot:run
```

Buka http://localhost:8080

Tabel dibuat otomatis (`ddl-auto=update`) dan data contoh (2 kategori, 4 produk) diisi
saat database masih kosong.

Konfigurasi database dapat diubah tanpa mengedit file menggunakan environment variable:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/kasir"
$env:DB_USER="admin"
$env:DB_PASSWORD="admin"
mvn spring-boot:run
```

## 4. Chatbot AI (Spring AI + Naraya)

Chatbot memakai Spring AI melalui API OpenAI-compatible Naraya dengan model `gpt-6-luna`, sama seperti konfigurasi model pi agent. `NARAYA_API_KEY` disimpan hanya sebagai environment variable; jangan menyalin key dari konfigurasi pi agent atau memasukkannya ke Git.

```powershell
# Isi .env terlebih dahulu, kemudian jalankan aplikasi
mvn spring-boot:run
```

Buka http://localhost:8080/chat. Chatbot menyediakan tool pencarian produk, tambah produk, tambah stok, melihat ringkasan omzet/jumlah transaksi, melihat riwayat transaksi terbaru, mencatat transaksi dengan konfirmasi, dan meminta konfirmasi sebelum hapus produk. Waktu transaksi menggunakan `LocalDateTime.now()`. Untuk mengonfirmasi penghapusan atau pencatatan transaksi, ketik persis kode konfirmasi yang ditampilkan. Jangan commit `.env`; file tersebut sudah masuk `.gitignore`.


Tes memakai H2 in-memory (`src/test/resources/application.properties`), jadi tidak butuh PostgreSQL.

## Halaman

| URL | Fungsi |
|---|---|
| `/` | Dashboard: omzet, jumlah transaksi, stok menipis |
| `/produk` | Daftar + cari produk, tambah, ubah, hapus |
| `/kategori` | Tambah dan hapus kategori |
| `/kasir` | Isi jumlah beli per produk, lalu Checkout |
| `/transaksi` | Riwayat transaksi |
| `/transaksi/{id}` | Struk (bisa dicetak) |

## Struktur

```
src/main/java/com/kasir/
├── KasirSpringApplication.java
├── entity/       Kategori, Produk, Transaksi, DetailTransaksi
├── repository/   KategoriRepository, ProdukRepository, TransaksiRepository
├── service/      KasirService, ProductTools, TransactionTools, ChatAssistantService
├── controller/   Home, Produk, Kategori, Kasir
└── config/       DataSeeder
src/main/resources/
├── application.properties
├── templates/    halaman Thymeleaf
└── static/css/   style.css
```

## Catatan

- Produk yang sudah pernah terjual tidak bisa dihapus (terikat foreign key); aplikasi menampilkan pesan.
- `KasirService.checkout` mengunci baris produk (`PESSIMISTIC_WRITE`) agar stok tidak balapan jika dua kasir bekerja bersamaan.
- Transaksi dari chatbot selalu menampilkan ringkasan dan membutuhkan konfirmasi sebelum stok dikurangi.
- Untuk produksi: ganti `ddl-auto=update` dengan migrasi Flyway/Liquibase dan tambahkan login (Spring Security).
