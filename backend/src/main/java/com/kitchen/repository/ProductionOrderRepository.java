package com.kitchen.repository;

import com.kitchen.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    List<ProductionOrder> findByStatus(ProductionOrder.OrderStatus status);
    List<ProductionOrder> findByDeliveryEndTimeBetweenOrderByDeliveryEndTimeAsc(LocalDateTime start, LocalDateTime end);
    List<ProductionOrder> findByStatusIn(List<ProductionOrder.OrderStatus> statuses);
}
