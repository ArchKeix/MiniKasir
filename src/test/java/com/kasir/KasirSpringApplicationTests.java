package com.kasir;

import com.kasir.entity.Kategori;
import com.kasir.entity.Produk;
import com.kasir.entity.Transaksi;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import com.kasir.service.KasirService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KasirSpringApplicationTests {

    @Autowired MockMvc mvc;
    @Autowired KasirService kasir;
    @Autowired ProdukRepository produkRepo;
    @Autowired KategoriRepository kategoriRepo;

    @Test
    void halamanUtamaTampil() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")));
        mvc.perform(get("/produk")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Indomie Goreng")));
        mvc.perform(get("/kategori")).andExpect(status().isOk());
        mvc.perform(get("/kasir")).andExpect(status().isOk());
        mvc.perform(get("/transaksi")).andExpect(status().isOk());
        mvc.perform(get("/produk/baru")).andExpect(status().isOk());
    }

    @Test
    void tambahProdukLewatForm() throws Exception {
        Kategori k = kategoriRepo.findAllByOrderByNamaAsc().get(0);
        mvc.perform(post("/produk/simpan")
                        .param("kode", "TST01").param("nama", "Produk Tes")
                        .param("harga", "7500").param("stok", "10")
                        .param("kategoriId", String.valueOf(k.getId())))
                .andExpect(status().is3xxRedirection());

        Produk p = produkRepo.findByKode("TST01").orElseThrow();
        assertEquals("Produk Tes", p.getNama());
        assertEquals(10, p.getStok());
    }

    @Test
    void validasiMenolakDataKosongDanKodeGanda() throws Exception {
        mvc.perform(post("/produk/simpan").param("kode", "").param("nama", "")
                        .param("harga", "-1").param("stok", "-5"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kode wajib diisi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pilih kategori")));

        Kategori k = kategoriRepo.findAllByOrderByNamaAsc().get(0);
        mvc.perform(post("/produk/simpan").param("kode", "MK01").param("nama", "Duplikat")
                        .param("harga", "1000").param("stok", "1")
                        .param("kategoriId", String.valueOf(k.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kode sudah dipakai")));
    }

    @Test
    void checkoutMengurangiStokDanGagalMelakukanRollback() {
        Produk mie = produkRepo.findByKode("MK01").orElseThrow();
        Produk air = produkRepo.findByKode("MN02").orElseThrow();
        int stokMieAwal = mie.getStok();
        int stokAirAwal = air.getStok();

        // Berhasil
        Map<Long, Integer> ok = new LinkedHashMap<>();
        ok.put(mie.getId(), 2);
        Transaksi t = kasir.checkout(ok);
        assertEquals(0, new BigDecimal("7000").compareTo(t.getTotal()));
        assertEquals(stokMieAwal - 2, produkRepo.findById(mie.getId()).orElseThrow().getStok());

        // Gagal: air mineral stoknya tidak cukup -> seluruh transaksi batal
        Map<Long, Integer> gagal = new LinkedHashMap<>();
        gagal.put(mie.getId(), 1);
        gagal.put(air.getId(), stokAirAwal + 100);
        assertThrows(IllegalStateException.class, () -> kasir.checkout(gagal));

        assertEquals(stokMieAwal - 2, produkRepo.findById(mie.getId()).orElseThrow().getStok(),
                "stok mie tidak boleh berkurang karena transaksi di-rollback");
        assertEquals(stokAirAwal, produkRepo.findById(air.getId()).orElseThrow().getStok());
    }

    @Test
    void checkoutLewatWebMenampilkanStruk() throws Exception {
        Produk teh = produkRepo.findByKode("MN01").orElseThrow();
        mvc.perform(post("/kasir/checkout").param("qty_" + teh.getId(), "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/transaksi/*"));

        mvc.perform(post("/kasir/checkout").param("qty_" + teh.getId(), "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kasir"));
    }
}
