# PRD — Chatbot AI untuk Aplikasi Kasir

**Status:** Draft untuk implementasi tugas  
**Proyek:** Kasir Mini (Spring Boot, Hibernate/JPA, PostgreSQL)

## 1. Ringkasan

Tambahkan chatbot berbasis Spring AI ke aplikasi kasir yang sudah ada. Chatbot dapat mencari produk dan mengecek stok menggunakan tool calling. Untuk demonstrasi aksi tulis, chatbot dapat menambah produk/stok dan menghapus produk dengan konfirmasi eksplisit.

Semua validasi dan perubahan data tetap dilakukan oleh aplikasi melalui service dan repository yang ada. Model AI tidak mendapat akses SQL atau database langsung.

## 2. Tujuan

- Mendemonstrasikan integrasi Spring AI dan tool calling.
- Menggunakan data kasir nyata melalui Hibernate/JPA.
- Menunjukkan bahwa aksi destruktif—penghapusan produk—memerlukan persetujuan pengguna.
- Menyediakan eksperimen yang dapat diuji dan dijelaskan dalam dokumentasi tugas.

### Di luar cakupan

Checkout melalui chatbot, riwayat percakapan permanen, RAG, multi-agent, dashboard insight AI, serta sistem role/auth baru. Fitur kasir yang ada tetap dapat digunakan seperti biasa.

## 3. Pengguna

Pengguna aplikasi kasir yang sedang menjalankan demo lokal. Aplikasi belum menunjukkan autentikasi; karena itu chatbot versi tugas tidak boleh dianggap siap digunakan secara publik atau multi-pengguna.

## 4. Fitur versi pertama

### Tool baca

- Cari produk berdasarkan kode atau nama.
- Tampilkan stok dan detail produk.
- Jika hasil pencarian ambigu, minta pengguna memilih produk yang dimaksud.

Contoh: “Cek stok KOPI01” atau “Cari produk gula.”

### Tool tambah

- Tambah produk dengan kode, nama, harga, stok, dan kategori bila diperlukan.
- Tambah stok produk yang sudah ada.
- Jika input wajib kurang atau tidak valid, jangan melakukan perubahan; minta informasi yang diperlukan.

### Tool hapus

- Hapus produk berdasarkan identitas yang jelas, misalnya kode produk.
- Sebelum menghapus, tampilkan kode dan nama produk lalu minta konfirmasi eksplisit.
- Penghapusan hanya berjalan setelah konfirmasi untuk produk yang sama.
- Jika dibatalkan, objek tidak ditemukan, atau produk tidak dapat dihapus karena aturan database, sampaikan hasilnya tanpa mengubah data.

## 5. Persyaratan fungsional

| ID | Persyaratan |
|---|---|
| FR-01 | Pengguna dapat mengirim pesan melalui halaman chatbot. |
| FR-02 | Chatbot mencari produk dan membaca stok melalui tool aplikasi. |
| FR-03 | Chatbot dapat menambah produk atau stok melalui service aplikasi. |
| FR-04 | Chatbot meminta klarifikasi untuk input wajib yang kurang atau hasil yang ambigu. |
| FR-05 | Penghapusan produk selalu meminta konfirmasi eksplisit sebelum dijalankan. |
| FR-06 | Konfirmasi hanya berlaku untuk produk yang ditampilkan dan aksi penghapusan yang sedang menunggu. |
| FR-07 | Validasi, aturan penghapusan, dan persistensi dijalankan oleh aplikasi, bukan model. |
| FR-08 | Kegagalan AI/provider menampilkan pesan yang jelas dan tidak mengubah data. |

## 6. Alur konfirmasi penghapusan

1. Pengguna meminta penghapusan produk dengan kode atau identitas yang dapat dicari.
2. Aplikasi mencari produk dan menampilkan kode serta namanya.
3. Aplikasi meminta jawaban konfirmasi yang jelas, misalnya: **“Yakin hapus KOPI01 — Kopi Robusta? Balas ‘Ya, hapus KOPI01’ untuk melanjutkan.”**
4. Aplikasi menyimpan aksi tertunda di session pengguna.
5. Jika pengguna menyetujui penghapusan produk yang sama, aplikasi memvalidasi ulang lalu menjalankan penghapusan.
6. Jika pengguna membatalkan atau menjawab tidak sesuai, penghapusan tidak dilakukan dan aksi tertunda dibatalkan.

Konfirmasi yang sudah digunakan tidak boleh digunakan ulang. Tidak perlu membangun tabel aksi tertunda pada versi pertama; session cukup untuk demo lokal satu pengguna. Persistensi konfirmasi baru dipertimbangkan jika kebutuhan penggunaan lintas sesi muncul.

## 7. Rancangan teknis minimal

```text
Halaman chat (Thymeleaf)
  → ChatController
  → ChatAssistantService (Spring AI)
  → Tool service: pencarian / tambah / hapus / konfirmasi
  → service dan repository yang ada
  → Hibernate/JPA → PostgreSQL
```

Perubahan kode yang diperkirakan:

```text
controller/ChatController.java
templates/chat/index.html
service/ChatAssistantService.java
service/ProductTools.java (atau satu service tool yang sesuai)
```

Gunakan `ProdukRepository` dan service domain yang sudah ada. Jangan membuat abstraksi terpisah untuk setiap operasi kecuali struktur proyek atau pengujian benar-benar membutuhkannya. Penghapusan harus menggunakan aturan aplikasi/database yang sudah berlaku. Jangan menambahkan tool SQL generik.

## 8. Konfigurasi

Proyek menggunakan Java 17 dan Spring Boot 3.2.5. Sebelum menambahkan dependency, verifikasi versi Spring AI yang kompatibel dengan versi proyek dan ikuti konfigurasi provider untuk versi tersebut.

- API key disimpan sebagai environment variable, bukan di source code atau Git.
- Dokumentasikan cara mengatur key dan menjalankan aplikasi.
- Jika chatbot tidak dapat dikonfigurasi, fitur kasir yang sudah ada tetap harus bisa dijalankan.

Nama dependency dan properti konfigurasi ditetapkan saat implementasi setelah provider dan versi dipilih.

## 9. Keamanan dan batasan

- Model hanya boleh memanggil tool yang didefinisikan secara eksplisit.
- Validasi input tetap dilakukan di aplikasi.
- Jangan mengirim API key atau data sensitif ke model.
- Jangan menganggap konfirmasi bahasa alami yang tidak jelas sebagai persetujuan.
- Fitur ini untuk eksperimen lokal; sebelum penggunaan publik dibutuhkan autentikasi, otorisasi, dan penilaian keamanan tambahan.

## 10. Pengujian dan kriteria selesai

Implementasi selesai jika:

- Pengguna dapat bertanya lewat halaman chatbot.
- Pencarian dan pemeriksaan stok menggunakan data database.
- Tambah produk/stok berhasil untuk input valid dan ditolak untuk input tidak valid.
- Produk ambigu menghasilkan klarifikasi, bukan perubahan.
- Penghapusan tanpa persetujuan tidak menghapus data.
- Persetujuan untuk produk yang ditampilkan dapat menghapusnya jika aturan aplikasi mengizinkan.
- Persetujuan salah atau pembatalan tidak mengubah data.
- API key tidak masuk ke repository.
- Test aplikasi yang relevan lulus.

Gunakan test untuk aturan tool dan konfirmasi; panggilan model nyata boleh diuji manual agar test otomatis tidak bergantung pada API eksternal atau biaya provider.

## 11. Eksperimen dan dokumentasi tugas

Catat provider/model dan versi Spring AI, langkah konfigurasi, contoh pertanyaan, tool yang dipilih model, hasil, serta kasus ambigu/gagal. Sertakan 5W + 1H dan diagram yang relevan:

- Use case: cari produk, cek stok, tambah produk/stok, hapus dengan konfirmasi.
- Sequence: pencarian dan penghapusan dengan konfirmasi.
- Class diagram: controller, assistant/tool service, repository, dan entitas terkait.
- ERD: gunakan model data kasir yang sudah ada; tidak perlu tabel chatbot baru untuk versi pertama.

## 12. Rencana implementasi

1. Verifikasi kompatibilitas dan pilih provider Spring AI.
2. Tambahkan konfigurasi aman dan halaman chat sederhana.
3. Implementasikan tool baca dan uji.
4. Implementasikan tool tambah dengan validasi dan uji.
5. Implementasikan hapus dengan konfirmasi berbasis session dan uji kasus setuju/batal/salah.
6. Jalankan eksperimen manual dan lengkapi dokumentasi serta diagram.
