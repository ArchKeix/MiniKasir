package com.kasir.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "produk")
public class Produk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kode wajib diisi")
    @Size(max = 20, message = "Kode maksimal 20 karakter")
    @Column(nullable = false, unique = true, length = 20)
    private String kode;

    @NotBlank(message = "Nama wajib diisi")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    @Column(nullable = false, length = 100)
    private String nama;

    @NotNull(message = "Harga wajib diisi")
    @DecimalMin(value = "0", message = "Harga tidak boleh negatif")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal harga;

    @NotNull(message = "Stok wajib diisi")
    @Min(value = 0, message = "Stok tidak boleh negatif")
    @Column(nullable = false)
    private Integer stok;

    @ManyToOne
    @JoinColumn(name = "kategori_id")
    private Kategori kategori;

    public Produk() {}

    public Produk(String kode, String nama, BigDecimal harga, int stok, Kategori kategori) {
        this.kode = kode;
        this.nama = nama;
        this.harga = harga;
        this.stok = stok;
        this.kategori = kategori;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public BigDecimal getHarga() { return harga; }
    public void setHarga(BigDecimal harga) { this.harga = harga; }
    public Integer getStok() { return stok; }
    public void setStok(Integer stok) { this.stok = stok; }
    public Kategori getKategori() { return kategori; }
    public void setKategori(Kategori kategori) { this.kategori = kategori; }
}
