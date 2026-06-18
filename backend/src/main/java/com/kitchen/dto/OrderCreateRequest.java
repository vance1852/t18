package com.kitchen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderCreateRequest {
    private String orderNo;
    private Long dishId;
    private Integer quantity;
    private LocalDateTime deliveryStartTime;
    private LocalDateTime deliveryEndTime;
    private String deliveryArea;
}
