package com.kasir.controller;

import com.kasir.entity.Transaksi;
import com.kasir.repository.ProdukRepository;
import com.kasir.service.KasirService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class KasirController {

    private static final String PREFIX = "qty_";

    private final ProdukRepository produkRepo;
    private final KasirService kasirService;

    public KasirController(ProdukRepository produkRepo, KasirService kasirService) {
        this.produkRepo = produkRepo;
        this.kasirService = kasirService;
    }

    // Halaman kasir: pilih jumlah tiap produk
    @GetMapping("/kasir")
    public String kasir(Model model) {
        model.addAttribute("daftar", produkRepo.findByStokGreaterThanOrderByNama(0));
        return "kasir/index";
    }

    // Checkout: form mengirim parameter qty_{idProduk}=jumlah
    @PostMapping("/kasir/checkout")
    public String checkout(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        Map<Long, Integer> keranjang = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!e.getKey().startsWith(PREFIX) || e.getValue() == null || e.getValue().isBlank()) {
                    continue;
                }
                int qty = Integer.parseInt(e.getValue().trim());
                if (qty == 0) {
                    continue;   // tidak dibeli
                }
                keranjang.put(Long.parseLong(e.getKey().substring(PREFIX.length())), qty);
            }
            Transaksi trx = kasirService.checkout(keranjang);
            return "redirect:/transaksi/" + trx.getId();
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("galat", "Jumlah harus berupa angka bulat");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("galat", e.getMessage());
        }
        return "redirect:/kasir";
    }

    // Riwayat transaksi
    @GetMapping("/transaksi")
    public String riwayat(Model model) {
        model.addAttribute("daftar", kasirService.riwayat());
        model.addAttribute("omzet", kasirService.omzet());
        return "transaksi/list";
    }

    // Struk
    @GetMapping("/transaksi/{id}")
    public String struk(@PathVariable Long id, Model model) {
        Transaksi t = kasirService.cariTransaksi(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaksi tidak ditemukan"));
        model.addAttribute("t", t);
        return "transaksi/struk";
    }
}
