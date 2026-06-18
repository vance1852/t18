package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "production_order")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "dish_id")
    private Long dishId;

    private Integer quantity;

    @Column(name = "delivery_start_time")
    private LocalDateTime deliveryStartTime;

    @Column(name = "delivery_end_time")
    private LocalDateTime deliveryEndTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "delivery_area")
    private String deliveryArea;

    public enum OrderStatus {
        PENDING, SCHEDULED, PRODUCING, COMPLETED, DELAYED
    }
}
