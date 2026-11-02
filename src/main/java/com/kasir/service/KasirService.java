package com.kasir.service;

import com.kasir.entity.DetailTransaksi;
import com.kasir.entity.Produk;
import com.kasir.entity.Transaksi;
import com.kasir.repository.ProdukRepository;
import com.kasir.repository.TransaksiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KasirService {

    private final ProdukRepository produkRepo;
    private final TransaksiRepository transaksiRepo;

    public KasirService(ProdukRepository produkRepo, TransaksiRepository transaksiRepo) {
        this.produkRepo = produkRepo;
        this.transaksiRepo = transaksiRepo;
    }

    /**
     * Checkout: keranjang = (idProduk -> jumlah).
     * Semua langkah berada dalam SATU transaksi database. Jika satu produk
     * stoknya kurang, seluruh transaksi dibatalkan (rollback) karena
     * RuntimeException keluar dari method ini.
     */
    @Transactional
    public Transaksi checkout(Map<Long, Integer> keranjang) {
        if (keranjang == null || keranjang.isEmpty()) {
            throw new IllegalArgumentException("Keranjang masih kosong");
        }

        Transaksi trx = new Transaksi();

        for (Map.Entry<Long, Integer> item : keranjang.entrySet()) {
            int qty = item.getValue();
            if (qty <= 0) {
                throw new IllegalArgumentException("Jumlah harus lebih dari 0");
            }

            Produk p = produkRepo.findByIdForUpdate(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produk id " + item.getKey() + " tidak ditemukan"));

            if (p.getStok() < qty) {
                throw new IllegalStateException(
                        "Stok " + p.getNama() + " tidak cukup (sisa " + p.getStok() + ")");
            }

            p.setStok(p.getStok() - qty);   // dirty checking: otomatis di-UPDATE saat commit
            trx.tambahDetail(new DetailTransaksi(p, qty));
        }

        return transaksiRepo.save(trx);     // cascade ke DetailTransaksi
    }

    @Transactional(readOnly = true)
    public List<Transaksi> riwayat() {
        return transaksiRepo.findAllWithDetail();
    }

    @Transactional(readOnly = true)
    public Optional<Transaksi> cariTransaksi(Long id) {
        return transaksiRepo.findByIdWithDetail(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal omzet() {
        BigDecimal total = transaksiRepo.totalOmzet();
        return total == null ? BigDecimal.ZERO : total;
    }
}
