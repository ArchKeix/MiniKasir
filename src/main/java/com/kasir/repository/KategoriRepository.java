package com.kasir.repository;

import com.kasir.entity.Kategori;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KategoriRepository extends JpaRepository<Kategori, Long> {

    List<Kategori> findAllByOrderByNamaAsc();

    boolean existsByNamaIgnoreCase(String nama);
}
