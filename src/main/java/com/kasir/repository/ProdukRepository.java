package com.kasir.repository;

import com.kasir.entity.Produk;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdukRepository extends JpaRepository<Produk, Long> {

    List<Produk> findAllByOrderByNamaAsc();

    List<Produk> findByNamaContainingIgnoreCaseOrKodeContainingIgnoreCaseOrderByNama(String nama, String kode);

    List<Produk> findByStokGreaterThanOrderByNama(int stok);

    List<Produk> findByStokLessThanEqualOrderByStok(int batas);

    Optional<Produk> findByKode(String kode);

    boolean existsByKategoriId(Long kategoriId);

    /** Kunci baris produk selama transaksi checkout supaya stok tidak balapan antar pembeli. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produk p where p.id = :id")
    Optional<Produk> findByIdForUpdate(@Param("id") Long id);
}
