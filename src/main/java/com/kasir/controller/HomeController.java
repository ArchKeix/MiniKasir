package com.kasir.controller;

import com.kasir.repository.ProdukRepository;
import com.kasir.repository.TransaksiRepository;
import com.kasir.service.KasirService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final int BATAS_STOK_MENIPIS = 5;

    private final ProdukRepository produkRepo;
    private final TransaksiRepository transaksiRepo;
    private final KasirService kasirService;

    public HomeController(ProdukRepository produkRepo, TransaksiRepository transaksiRepo,
                          KasirService kasirService) {
        this.produkRepo = produkRepo;
        this.transaksiRepo = transaksiRepo;
        this.kasirService = kasirService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("jumlahProduk", produkRepo.count());
        model.addAttribute("jumlahTransaksi", transaksiRepo.count());
        model.addAttribute("omzet", kasirService.omzet());
        model.addAttribute("batas", BATAS_STOK_MENIPIS);
        model.addAttribute("stokMenipis", produkRepo.findByStokLessThanEqualOrderByStok(BATAS_STOK_MENIPIS));
        return "index";
    }
}
