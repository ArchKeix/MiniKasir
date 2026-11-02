package com.kasir.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaksi")
public class Transaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime tanggal = LocalDateTime.now();

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "transaksi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetailTransaksi> detail = new ArrayList<>();

    public void tambahDetail(DetailTransaksi d) {
        d.setTransaksi(this);
        detail.add(d);
        total = total.add(d.getSubtotal());
    }

    public Long getId() { return id; }
    public LocalDateTime getTanggal() { return tanggal; }
    public BigDecimal getTotal() { return total; }
    public List<DetailTransaksi> getDetail() { return detail; }
}
