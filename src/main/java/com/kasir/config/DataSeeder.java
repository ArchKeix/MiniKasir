package com.kasir.config;

import com.kasir.entity.Kategori;
import com.kasir.entity.Produk;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Mengisi data contoh saat database masih kosong. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final KategoriRepository kategoriRepo;
    private final ProdukRepository produkRepo;

    public DataSeeder(KategoriRepository kategoriRepo, ProdukRepository produkRepo) {
        this.kategoriRepo = kategoriRepo;
        this.produkRepo = produkRepo;
    }

    @Override
    public void run(String... args) {
        if (kategoriRepo.count() > 0) {
            return;
        }
        Kategori makanan = kategoriRepo.save(new Kategori("Makanan"));
        Kategori minuman = kategoriRepo.save(new Kategori("Minuman"));

        produkRepo.save(new Produk("MK01", "Indomie Goreng", new BigDecimal("3500"), 50, makanan));
        produkRepo.save(new Produk("MK02", "Roti Tawar", new BigDecimal("16000"), 20, makanan));
        produkRepo.save(new Produk("MN01", "Teh Botol", new BigDecimal("5000"), 30, minuman));
        produkRepo.save(new Produk("MN02", "Air Mineral", new BigDecimal("3000"), 3, minuman));
    }
}
