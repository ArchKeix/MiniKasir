package com.kasir.controller;

import com.kasir.entity.Kategori;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/kategori")
public class KategoriController {

    private final KategoriRepository kategoriRepo;
    private final ProdukRepository produkRepo;

    public KategoriController(KategoriRepository kategoriRepo, ProdukRepository produkRepo) {
        this.kategoriRepo = kategoriRepo;
        this.produkRepo = produkRepo;
    }

    @GetMapping
    public String daftar(Model model) {
        model.addAttribute("daftar", kategoriRepo.findAllByOrderByNamaAsc());
        return "kategori/list";
    }

    @PostMapping("/simpan")
    public String simpan(@RequestParam String nama, RedirectAttributes ra) {
        String bersih = nama == null ? "" : nama.trim();
        if (bersih.isEmpty()) {
            ra.addFlashAttribute("galat", "Nama kategori wajib diisi");
        } else if (bersih.length() > 50) {
            ra.addFlashAttribute("galat", "Nama kategori maksimal 50 karakter");
        } else if (kategoriRepo.existsByNamaIgnoreCase(bersih)) {
            ra.addFlashAttribute("galat", "Kategori \"" + bersih + "\" sudah ada");
        } else {
            kategoriRepo.save(new Kategori(bersih));
            ra.addFlashAttribute("sukses", "Kategori \"" + bersih + "\" ditambahkan");
        }
        return "redirect:/kategori";
    }

    @PostMapping("/{id}/hapus")
    public String hapus(@PathVariable Long id, RedirectAttributes ra) {
        if (produkRepo.existsByKategoriId(id)) {
            ra.addFlashAttribute("galat", "Kategori masih dipakai oleh produk, tidak bisa dihapus");
        } else {
            kategoriRepo.deleteById(id);
            ra.addFlashAttribute("sukses", "Kategori dihapus");
        }
        return "redirect:/kategori";
    }
}
