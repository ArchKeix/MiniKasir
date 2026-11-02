package com.kasir.repository;

import com.kasir.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {

    @Query("select distinct t from Transaksi t join fetch t.detail d join fetch d.produk order by t.tanggal desc")
    List<Transaksi> findAllWithDetail();

    @Query("select distinct t from Transaksi t join fetch t.detail d join fetch d.produk where t.id = :id")
    Optional<Transaksi> findByIdWithDetail(@Param("id") Long id);

    /** null jika belum ada transaksi. */
    @Query("select sum(t.total) from Transaksi t")
    BigDecimal totalOmzet();
}
