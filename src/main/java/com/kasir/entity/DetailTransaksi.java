package com.kasir.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detail_transaksi")
public class DetailTransaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaksi_id")
    private Transaksi transaksi;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produk_id")
    private Produk produk;

    private int jumlah;

    /** Harga saat transaksi (snapshot), agar riwayat tidak berubah bila harga produk diubah. */
    @Column(name = "harga_satuan", precision = 12, scale = 2)
    private BigDecimal hargaSatuan;

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal;

    public DetailTransaksi() {}

    public DetailTransaksi(Produk produk, int jumlah) {
        this.produk = produk;
        this.jumlah = jumlah;
        this.hargaSatuan = produk.getHarga();
        this.subtotal = hargaSatuan.multiply(BigDecimal.valueOf(jumlah));
    }

    public Long getId() { return id; }
    public Transaksi getTransaksi() { return transaksi; }
    public void setTransaksi(Transaksi transaksi) { this.transaksi = transaksi; }
    public Produk getProduk() { return produk; }
    public int getJumlah() { return jumlah; }
    public BigDecimal getHargaSatuan() { return hargaSatuan; }
    public BigDecimal getSubtotal() { return subtotal; }
}
