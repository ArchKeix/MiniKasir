package com.kasir.service;

import com.kasir.entity.Produk;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductTools {

    private final ProdukRepository produkRepo;
    private final KategoriRepository kategoriRepo;

    public ProductTools(ProdukRepository produkRepo, KategoriRepository kategoriRepo) {
        this.produkRepo = produkRepo;
        this.kategoriRepo = kategoriRepo;
    }

    @Tool(description = "Cari produk berdasarkan potongan nama atau kode dan tampilkan stok, harga, serta kode. Gunakan untuk pertanyaan tentang produk atau stok.")
    public List<ProductInfo> cariProduk(@ToolParam(description = "Nama atau kode produk") String kataKunci) {
        if (kataKunci == null || kataKunci.isBlank()) return List.of();
        return produkRepo.findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCaseOrderByNama(
                        kataKunci.trim(), kataKunci.trim()).stream()
                .map(p -> new ProductInfo(p.getKode(), p.getNama(), p.getHarga(), p.getStok()))
                .toList();
    }

    @Tool(description = "Tambahkan produk baru. Harus tersedia kode unik, nama, harga tidak negatif, stok tidak negatif, dan nama kategori yang sudah ada.")
    @Transactional
    public String tambahProduk(
            @ToolParam(description = "Kode unik produk, maksimal 20 karakter") String kode,
            @ToolParam(description = "Nama produk, maksimal 100 karakter") String nama,
            @ToolParam(description = "Harga produk dalam rupiah") BigDecimal harga,
            @ToolParam(description = "Stok awal, bilangan bulat non-negatif") Integer stok,
            @ToolParam(description = "Nama kategori yang sudah terdaftar") String namaKategori) {
        if (kode == null || kode.isBlank() || kode.trim().length() > 20
                || nama == null || nama.isBlank() || nama.trim().length() > 100
                || harga == null || harga.signum() < 0 || stok == null || stok < 0
                || namaKategori == null || namaKategori.isBlank()) {
            return "Data tidak valid. Periksa kode, nama, harga, stok, dan kategori.";
        }
        String kodeBersih = kode.trim();
        if (produkRepo.findByKode(kodeBersih).isPresent()) return "Kode produk sudah digunakan.";
        var kategori = kategoriRepo.findAllByOrderByNamaAsc().stream()
                .filter(k -> k.getNama().equalsIgnoreCase(namaKategori.trim()))
                .findFirst();
        if (kategori.isEmpty()) return "Kategori tidak ditemukan. Gunakan kategori yang sudah ada.";
        Produk produk = new Produk(kodeBersih, nama.trim(), harga, stok, kategori.get());
        produkRepo.save(produk);
        return "Produk " + produk.getNama() + " (" + produk.getKode() + ") berhasil ditambahkan.";
    }

    @Tool(description = "Tambahkan stok ke produk yang sudah ada berdasarkan kode. Gunakan hanya untuk penambahan stok; stok tidak boleh dikurangi lewat tool ini.")
    @Transactional
    public String tambahStok(
            @ToolParam(description = "Kode produk") String kode,
            @ToolParam(description = "Jumlah stok positif yang ditambahkan") Integer jumlah) {
        if (kode == null || kode.isBlank() || jumlah == null || jumlah <= 0)
            return "Kode wajib diisi dan jumlah tambahan stok harus lebih dari nol.";
        Optional<Produk> hasil = produkRepo.findByKode(kode.trim());
        if (hasil.isEmpty()) return "Produk tidak ditemukan.";
        Produk produk = hasil.get();
        produk.setStok(Math.addExact(produk.getStok(), jumlah));
        return "Stok " + produk.getNama() + " berhasil ditambah " + jumlah + ". Stok sekarang " + produk.getStok() + ".";
    }

    @Tool(description = "Minta persetujuan sebelum menghapus produk. Tool ini hanya menyiapkan konfirmasi dan tidak menghapus produk.")
    public String mintaKonfirmasiHapus(@ToolParam(description = "Kode produk yang akan dihapus") String kode) {
        if (kode == null || kode.isBlank()) return "Kode produk wajib disebutkan.";
        Optional<Produk> hasil = produkRepo.findByKode(kode.trim());
        if (hasil.isEmpty()) return "Produk tidak ditemukan.";
        Produk p = hasil.get();
        return "Konfirmasi diperlukan untuk menghapus " + p.getKode() + " — " + p.getNama()
                + ". Ketik persis: YA HAPUS " + p.getKode();
    }

    @Transactional
    public String hapusSetelahKonfirmasi(String kode) {
        Optional<Produk> hasil = produkRepo.findByKode(kode.trim());
        if (hasil.isEmpty()) return "Produk tidak ditemukan; tidak ada perubahan.";
        try {
            produkRepo.delete(hasil.get());
            produkRepo.flush();
            return "Produk " + hasil.get().getKode() + " berhasil dihapus.";
        } catch (RuntimeException e) {
            return "Produk tidak bisa dihapus, kemungkinan sudah pernah terjual. Tidak ada perubahan.";
        }
    }

    public record ProductInfo(String kode, String nama, BigDecimal harga, Integer stok) {}
}
