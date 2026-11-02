package com.kasir.controller;

import com.kasir.entity.Kategori;
import com.kasir.entity.Produk;
import com.kasir.repository.KategoriRepository;
import com.kasir.repository.ProdukRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/produk")
public class ProdukController {

    private final ProdukRepository produkRepo;
    private final KategoriRepository kategoriRepo;

    public ProdukController(ProdukRepository produkRepo, KategoriRepository kategoriRepo) {
        this.produkRepo = produkRepo;
        this.kategoriRepo = kategoriRepo;
    }

    // READ (daftar + pencarian)
    @GetMapping
    public String daftar(@RequestParam(required = false) String q, Model model) {
        if (q == null || q.isBlank()) {
            model.addAttribute("daftar", produkRepo.findAllByOrderByNamaAsc());
        } else {
            model.addAttribute("daftar",
                    produkRepo.findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCaseOrderByNama(q.trim(), q.trim()));
        }
        model.addAttribute("q", q);
        return "produk/list";
    }

    // CREATE (form)
    @GetMapping("/baru")
    public String formBaru(Model model) {
        Produk p = new Produk();
        p.setHarga(BigDecimal.ZERO);
        p.setStok(0);
        return tampilForm(model, p, "Tambah Produk");
    }

    // UPDATE (form)
    @GetMapping("/{id}/ubah")
    public String formUbah(@PathVariable Long id, Model model) {
        Produk p = produkRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produk tidak ditemukan"));
        return tampilForm(model, p, "Ubah Produk");
    }

    // CREATE / UPDATE (simpan)
    @PostMapping("/simpan")
    public String simpan(@Valid @ModelAttribute("produk") Produk produk,
                         BindingResult result,
                         @RequestParam(required = false) Long kategoriId,
                         Model model,
                         RedirectAttributes ra) {

        Optional<Kategori> kategori = kategoriId == null ? Optional.empty() : kategoriRepo.findById(kategoriId);
        if (kategori.isPresent()) {
            produk.setKategori(kategori.get());
        } else {
            result.rejectValue("kategori", "required", "Pilih kategori");
        }

        // Kode harus unik
        if (produk.getKode() != null && !produk.getKode().isBlank()) {
            produk.setKode(produk.getKode().trim());
            produkRepo.findByKode(produk.getKode()).ifPresent(ada -> {
                if (!ada.getId().equals(produk.getId())) {
                    result.rejectValue("kode", "duplicate", "Kode sudah dipakai produk lain");
                }
            });
        }

        if (result.hasErrors()) {
            model.addAttribute("judul", produk.getId() == null ? "Tambah Produk" : "Ubah Produk");
            model.addAttribute("daftarKategori", kategoriRepo.findAllByOrderByNamaAsc());
            return "produk/form";
        }

        boolean baru = produk.getId() == null;
        produkRepo.save(produk);
        ra.addFlashAttribute("sukses", "Produk \"" + produk.getNama() + "\" berhasil " + (baru ? "ditambahkan" : "diperbarui"));
        return "redirect:/produk";
    }

    // DELETE
    @PostMapping("/{id}/hapus")
    public String hapus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            produkRepo.deleteById(id);
            ra.addFlashAttribute("sukses", "Produk berhasil dihapus");
        } catch (DataAccessException | TransactionException e) {
            ra.addFlashAttribute("galat", "Produk tidak bisa dihapus karena sudah pernah terjual");
        }
        return "redirect:/produk";
    }

    private String tampilForm(Model model, Produk produk, String judul) {
        model.addAttribute("produk", produk);
        model.addAttribute("judul", judul);
        model.addAttribute("daftarKategori", kategoriRepo.findAllByOrderByNamaAsc());
        return "produk/form";
    }
}
