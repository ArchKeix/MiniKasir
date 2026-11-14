package com.kasir.service;

import com.kasir.entity.Kategori;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductToolsTests {

    @Autowired ProductTools tools;
    @Autowired ProdukRepository produkRepo;
    @Autowired KategoriRepository kategoriRepo;

    @BeforeEach
    void bersihkan() {
        produkRepo.deleteAll();
        kategoriRepo.deleteAll();
        kategoriRepo.save(new Kategori("Minuman"));
    }

    @Test
    void tambahProdukValidBerhasil() {
        String hasil = tools.tambahProduk("TEST1", "Produk Tes", new java.math.BigDecimal("1200"), 4, "Minuman");
        assertThat(hasil).contains("berhasil ditambahkan");
        assertThat(produkRepo.findByKode("TEST1")).isPresent();
    }

    @Test
    void produkDenganKodeDuplikatDitolak() {
        tools.tambahProduk("TEST1", "Produk Tes", new java.math.BigDecimal("1200"), 4, "Minuman");
        String hasil = tools.tambahProduk("TEST1", "Produk Lain", new java.math.BigDecimal("1500"), 2, "Minuman");
        assertThat(hasil).contains("sudah digunakan");
        assertThat(produkRepo.count()).isEqualTo(1);
    }

    @Test
    void hapusToolHanyaMemintaKonfirmasiDanTidakMenghapus() {
        tools.tambahProduk("TEST1", "Produk Tes", new java.math.BigDecimal("1200"), 4, "Minuman");
        String hasil = tools.mintaKonfirmasiHapus("TEST1");
        assertThat(hasil).contains("YA HAPUS TEST1");
        assertThat(produkRepo.findByKode("TEST1")).isPresent();
    }
}
