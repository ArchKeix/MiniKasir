package com.kasir.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatAssistantService {

    private final ChatClient chatClient;
    private final ProductTools productTools;
    private final TransactionTools transactionTools;

    public ChatAssistantService(ChatClient.Builder builder, ProductTools productTools,
                                TransactionTools transactionTools) {
        this.chatClient = builder
                .defaultSystem("Anda adalah asisten kasir berbahasa Indonesia. "
                        + "Gunakan tool produk untuk fakta produk/stok dan tool transaksi untuk fakta penjualan/riwayat; jangan mengarang angka. "
                        + "Sebelum mencatat transaksi, gunakan siapkanTransaksi untuk membuat ringkasan dan minta konfirmasi; jangan pernah mencatat transaksi saat menyiapkan. "
                        + "Jika pengguna meminta mencatat transaksi, kumpulkan semua produk dan jumlahnya. Pastikan produk spesifik melalui kode atau hasil pencarian, lalu panggil siapkanTransaksi dengan format item kode:jumlah. "
                        + "Pesan konfirmasi transaksi harus ditampilkan persis sebagai: YA CATAT TRANSAKSI ITEMS=... dan jangan pernah mengubah formatnya. "
                        + "Untuk penghapusan, selalu gunakan tool mintaKonfirmasiHapus dan jangan pernah menghapus langsung. "
                        + "Jangan mengurangi stok lewat tool tambahStok. Jawab singkat dan jelas.")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.productTools = productTools;
        this.transactionTools = transactionTools;
    }

    public String jawab(String pesan, List<Map<String, String>> riwayat) {
        if (pesan == null || pesan.isBlank()) return "Silakan tulis pertanyaan atau perintah.";

        StringBuilder konteks = new StringBuilder();
        if (riwayat != null && !riwayat.isEmpty()) {
            konteks.append("Konteks percakapan sebelumnya (gunakan untuk memahami pesan terbaru):\\n");
            int mulai = Math.max(0, riwayat.size() - 8);
            for (int i = mulai; i < riwayat.size(); i++) {
                Map<String, String> item = riwayat.get(i);
                konteks.append(item.get("role")).append(": ").append(item.get("isi")).append("\\n");
            }
            konteks.append("\\nPesan terbaru pengguna: ");
        }
        konteks.append(pesan.trim());

        return chatClient.prompt()
                .user(konteks.toString())
                .tools(productTools, transactionTools)
                .call()
                .content();
    }
}
