package com.kitchen.repository;

import com.kitchen.entity.DeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
    List<DeliveryBatch> findByDepartureTimeBetweenOrderByDepartureTime(LocalDateTime start, LocalDateTime end);
}
