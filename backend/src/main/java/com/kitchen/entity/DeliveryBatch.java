package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "delivery_batch")
public class DeliveryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "delivery_area")
    private String deliveryArea;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "vehicle_no")
    private String vehicleNo;

    @Column(name = "order_ids", columnDefinition = "text")
    private String orderIds;

    private Integer totalQuantity;
}
