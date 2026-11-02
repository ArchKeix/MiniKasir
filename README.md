# Kasir Mini (Spring Boot + Hibernate + PostgreSQL)

Aplikasi web kasir sederhana: kelola kategori dan produk (CRUD), lakukan transaksi
(stok berkurang otomatis, gagal = rollback), lihat riwayat dan cetak struk.

Teknologi: Java 17, Spring Boot 3.2 (Spring MVC, Spring Data JPA/Hibernate 6, Validation), Thymeleaf, PostgreSQL.

## 1. Siapkan PostgreSQL

Pastikan service PostgreSQL berjalan, lalu buat database `kasir`. Dengan pgAdmin, buat database bernama `kasir` dengan owner `admin`. Atau dari PowerShell jika `psql` sudah tersedia:

```powershell
psql -U admin -h localhost -p 5432 -d postgres -c "CREATE DATABASE kasir OWNER admin;"
```

Konfigurasi bawaan proyek sudah disiapkan untuk PostgreSQL lokal berikut:

```text
Host     : localhost
Port     : 5432
Database : kasir
Username : admin
Password : admin
```

Jika password atau nama user berbeda, gunakan environment variable `DB_USER` dan `DB_PASSWORD` saat menjalankan aplikasi melalui terminal, atau sesuaikan langsung di file application.properties.

## 2. Jalankan

Kamu bisa menjalankan aplikasi ini melalui IDE seperti IntelliJ IDEA atau langsung dari PowerShell/Terminal.

Opsi A: Menggunakan IntelliJ IDEA (Direkomendasikan)
1.Buka folder proyek di IntelliJ IDEA.

2.Buka file src/main/resources/application.properties. Pastikan spring.datasource.username dan spring.datasource.password sudah sesuai dengan akun PostgreSQL kamu (default: admin).

3.Opsional: Jika port 8080 terbentur dengan service EDB PostgreSQL, tambahkan baris server.port=8081 di bagian paling atas file application.properties.

4.Buka file src/main/java/com/kasir/KasirSpringApplication.java.

5.Klik ikon Run (Play hijau) di sebelah kiri metode main.

6.Buka http://localhost:8080 (atau http://localhost:8081 jika port diubah).

Opsi B: Menggunakan PowerShell / Terminal
Jalankan perintah dari folder yang berisi `pom.xml`:

```powershell
mvn spring-boot:run
```

(Gunakan .\mvnw spring-boot:run jika perintah mvn tidak dikenali oleh sistem).

Untuk konfigurasi database berbeda tanpa mengedit file:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/kasir"
$env:DB_USER="admin"
$env:DB_PASSWORD="admin"
mvn spring-boot:run
```

Tabel dibuat otomatis (ddl-auto=update) dan data contoh (2 kategori, 4 produk) diisi
saat database masih kosong.

## 3. Tes

```bash
mvn test
```

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
├── service/      KasirService   (checkout @Transactional)
├── controller/   Home, Produk, Kategori, Kasir
└── config/       DataSeeder
src/main/resources/
├── application.properties
├── templates/    halaman Thymeleaf
└── static/css/   style.css
```

## Catatan

- Produk yang sudah pernah terjual tidak bisa dihapus (terikat foreign key); aplikasi menampilkan pesan.
- `checkout` mengunci baris produk (`PESSIMISTIC_WRITE`) agar stok tidak balapan jika dua kasir bekerja bersamaan.
- Untuk produksi: ganti `ddl-auto=update` dengan migrasi Flyway/Liquibase dan tambahkan login (Spring Security).