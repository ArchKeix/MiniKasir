package com.kasir.controller;

import com.kasir.service.ChatAssistantService;
import com.kasir.service.ProductTools;
import com.kasir.service.TransactionTools;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class ChatController {

    private static final String RIWAYAT = "chatRiwayat";
    private static final String HAPUS_TERTUNDA = "chatHapusTertunda";
    private static final String TRANSAKSI_TERTUNDA = "chatTransaksiTertunda";

    private final ChatAssistantService assistant;
    private final ProductTools productTools;
    private final TransactionTools transactionTools;

    public ChatController(ChatAssistantService assistant, ProductTools productTools, TransactionTools transactionTools) {
        this.assistant = assistant;
        this.productTools = productTools;
        this.transactionTools = transactionTools;
    }

    @GetMapping("/chat")
    public String halaman(HttpSession session, Model model) {
        model.addAttribute("riwayat", riwayat(session));
        return "chat/index";
    }

    @PostMapping("/chat")
    @ResponseBody
    public Map<String, String> kirim(@RequestParam String pesan, HttpSession session) {
        if (pesan == null || pesan.isBlank()) {
            return Map.of("jawaban", "Silakan tulis pesan terlebih dahulu.");
        }
        String input = pesan.trim();
        List<Map<String, String>> konteks;
        synchronized (session) {
            tambah(session, "user", input);
            String kodeTertunda = (String) session.getAttribute(HAPUS_TERTUNDA);
            if (kodeTertunda != null) {
                String normal = input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").trim().replaceAll("\\s+", " ");
                if (normal.equals("BATAL") || normal.equals("TIDAK")) {
                    session.removeAttribute(HAPUS_TERTUNDA);
                    return balas(session, "Penghapusan dibatalkan.");
                }
                if (normal.equals("YA HAPUS " + kodeTertunda.toUpperCase(Locale.ROOT))) {
                    session.removeAttribute(HAPUS_TERTUNDA);
                    return balas(session, productTools.hapusSetelahKonfirmasi(kodeTertunda));
                }
                session.removeAttribute(HAPUS_TERTUNDA);
                return balas(session, "Konfirmasi tidak cocok; penghapusan dibatalkan. Silakan mulai permintaan lagi.");
            }
            String transaksiTertunda = (String) session.getAttribute(TRANSAKSI_TERTUNDA);
            if (transaksiTertunda != null) {
                String normal = input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_:, ]", " ").trim().replaceAll("\\s+", " ");
                session.removeAttribute(TRANSAKSI_TERTUNDA);
                if (normal.equals("YA CATAT TRANSAKSI") || normal.equals("YA CATAT TRANSAKSI ITEMS " + transaksiTertunda.replace(":", " ").replace(",", " ").toUpperCase(Locale.ROOT))
                        || input.trim().equalsIgnoreCase("YA CATAT TRANSAKSI ITEMS=" + transaksiTertunda))
                    return balas(session, transactionTools.catatSetelahKonfirmasi(transaksiTertunda));
                if (normal.equals("BATAL") || normal.equals("TIDAK"))
                    return balas(session, "Pencatatan transaksi dibatalkan; stok tidak berubah.");
                return balas(session, "Konfirmasi tidak cocok; transaksi tidak dicatat dan stok tidak berubah.");
            }
            List<Map<String, String>> riwayat = riwayat(session);
            konteks = List.copyOf(riwayat.subList(0, riwayat.size() - 1));
        }

        try {
            String jawaban = assistant.jawab(input, konteks);
            String hasil = jawaban == null ? "Tidak ada jawaban dari AI." : jawaban;
            synchronized (session) {
                String kode = ekstrakKonfirmasi(hasil, "YA HAPUS ([A-Z0-9_-]+)");
                String itemTransaksi = ekstrakKonfirmasi(hasil, "YA CATAT TRANSAKSI\\s+ITEMS=([A-Z0-9_:, -]+)");
                if (kode != null) session.setAttribute(HAPUS_TERTUNDA, kode);
                if (itemTransaksi != null) session.setAttribute(TRANSAKSI_TERTUNDA, itemTransaksi.replace(" ", ""));
                tambah(session, "assistant", hasil);
            }
            return Map.of("jawaban", hasil);
        } catch (Exception e) {
            String error = "Permintaan ke provider AI gagal. Periksa NARAYA_API_KEY, model, dan koneksi Naraya.";
            synchronized (session) { tambah(session, "assistant", error); }
            return Map.of("jawaban", error);
        }
    }

    @PostMapping("/chat/bersihkan")
    public String bersihkan(HttpSession session) {
        session.removeAttribute(RIWAYAT);
        session.removeAttribute(HAPUS_TERTUNDA);
        session.removeAttribute(TRANSAKSI_TERTUNDA);
        return "redirect:/chat";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> riwayat(HttpSession session) {
        List<Map<String, String>> result = (List<Map<String, String>>) session.getAttribute(RIWAYAT);
        if (result == null) {
            result = new ArrayList<>();
            session.setAttribute(RIWAYAT, result);
        }
        return result;
    }

    private Map<String, String> balas(HttpSession session, String isi) {
        tambah(session, "assistant", isi);
        return Map.of("jawaban", isi);
    }

    private void tambah(HttpSession session, String role, String isi) {
        riwayat(session).add(Map.of("role", role, "isi", isi));
    }

    private String ekstrakKonfirmasi(String jawaban, String pattern) {
        if (jawaban == null) return null;
        var matcher = java.util.regex.Pattern.compile("(?i)" + pattern).matcher(jawaban);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
