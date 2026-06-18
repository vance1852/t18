package com.kitchen.repository;

import com.kitchen.entity.MaterialStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaterialStockRepository extends JpaRepository<MaterialStock, Long> {
    List<MaterialStock> findByMaterialIdOrderByExpireDateAsc(Long materialId);

    @Query("SELECT ms FROM MaterialStock ms WHERE ms.materialId = :materialId AND ms.expireDate >= :date AND ms.quantity > 0 ORDER BY ms.expireDate ASC")
    List<MaterialStock> findAvailableStock(Long materialId, LocalDate date);
}
