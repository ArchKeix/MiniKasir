package com.kasir.service;

import com.kasir.entity.DetailTransaksi;
import com.kasir.entity.Transaksi;
import com.kasir.repository.ProdukRepository;
import com.kasir.repository.TransaksiRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TransactionTools {

    private final TransaksiRepository transaksiRepo;
    private final ProdukRepository produkRepo;
    private final KasirService kasirService;

    public TransactionTools(TransaksiRepository transaksiRepo, ProdukRepository produkRepo, KasirService kasirService) {
        this.transaksiRepo = transaksiRepo;
        this.produkRepo = produkRepo;
        this.kasirService = kasirService;
    }

    @Tool(description = "Siapkan transaksi baru dari daftar kode produk dan jumlah. Jangan mencatat atau mengurangi stok; tool ini hanya membuat ringkasan dan meminta konfirmasi. Format item harus seperti MK01:2,MN02:1.")
    public String siapkanTransaksi(
            @ToolParam(description = "Daftar kode produk dan jumlah, format KODE:JUMLAH dipisahkan koma") String item) {
        if (item == null || item.isBlank()) return "Daftar item wajib diisi.";
        try {
            Map<Long, Integer> keranjang = parseKeranjang(item);
            StringBuilder ringkasan = new StringBuilder("Transaksi siap dicatat (waktu: sekarang):\\n");
            for (Map.Entry<Long, Integer> entry : keranjang.entrySet()) {
                var produk = produkRepo.findById(entry.getKey()).orElseThrow();
                ringkasan.append("- ").append(produk.getKode()).append(" — ")
                        .append(produk.getNama()).append(" x ").append(entry.getValue())
                        .append(" (Rp ").append(produk.getHarga().multiply(BigDecimal.valueOf(entry.getValue()))).append(")\\n");
            }
            ringkasan.append("Balas persis: YA CATAT TRANSAKSI ITEMS=").append(item.trim().replaceAll("\\s+", ""));
            return ringkasan.toString();
        } catch (Exception e) {
            return "Format transaksi tidak valid atau produk tidak ditemukan. Gunakan format KODE:JUMLAH, contoh MK01:2,MN02:1.";
        }
    }

    @Transactional
    public String catatSetelahKonfirmasi(String item) {
        try {
            Transaksi transaksi = kasirService.checkout(parseKeranjang(item));
            return "Transaksi berhasil dicatat pada " + transaksi.getTanggal() + " dengan total Rp " + transaksi.getTotal() + ".";
        } catch (RuntimeException e) {
            return "Transaksi gagal dicatat: " + e.getMessage();
        }
    }

    private Map<Long, Integer> parseKeranjang(String item) {
        Map<Long, Integer> keranjang = new java.util.LinkedHashMap<>();
        for (String bagian : item.split(",")) {
            String[] nilai = bagian.trim().split(":");
            if (nilai.length != 2) throw new IllegalArgumentException();
            var produk = produkRepo.findByKode(nilai[0].trim()).orElseThrow();
            int jumlah = Integer.parseInt(nilai[1].trim());
            if (jumlah <= 0) throw new IllegalArgumentException();
            keranjang.merge(produk.getId(), jumlah, Integer::sum);
        }
        return keranjang;
    }


    @Tool(description = "Tampilkan ringkasan penjualan kasir: jumlah transaksi dan total omzet dari seluruh riwayat transaksi. Gunakan untuk pertanyaan tentang total penjualan atau omzet.")
    @Transactional(readOnly = true)
    public SalesSummary ringkasanPenjualan() {
        BigDecimal omzet = transaksiRepo.totalOmzet();
        return new SalesSummary(transaksiRepo.count(), omzet == null ? BigDecimal.ZERO : omzet);
    }

    @Tool(description = "Tampilkan riwayat transaksi terbaru beserta ID, tanggal, item, jumlah, dan total. Gunakan untuk pertanyaan tentang riwayat penjualan.")
    @Transactional(readOnly = true)
    public List<TransactionInfo> riwayatTransaksi(
            @ToolParam(description = "Jumlah transaksi terbaru yang ingin ditampilkan, maksimal 20") Integer batas) {
        int jumlah = batas == null ? 10 : Math.min(Math.max(batas, 1), 20);
        return transaksiRepo.findAllWithDetail().stream()
                .limit(jumlah)
                .map(this::ubahKeInfo)
                .toList();
    }

    private TransactionInfo ubahKeInfo(Transaksi transaksi) {
        List<ItemInfo> item = transaksi.getDetail().stream()
                .map(d -> new ItemInfo(d.getProduk().getKode(), d.getProduk().getNama(), d.getJumlah(), d.getSubtotal()))
                .toList();
        return new TransactionInfo(transaksi.getId(), transaksi.getTanggal(), item, transaksi.getTotal());
    }

    public record SalesSummary(long jumlahTransaksi, BigDecimal omzet) {}

    public record TransactionInfo(Long id, LocalDateTime tanggal, List<ItemInfo> item, BigDecimal total) {}

    public record ItemInfo(String kode, String nama, int jumlah, BigDecimal subtotal) {}
}
