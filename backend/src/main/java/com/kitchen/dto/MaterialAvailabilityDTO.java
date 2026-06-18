package com.kitchen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialAvailabilityDTO {
    private Long materialId;
    private String materialName;
    private String materialCode;
    private String unit;
    private Double requiredQuantity;
    private Double stockQuantity;
    private Double shortageQuantity;
    private LocalDateTime shortageTime;
    private String status;
}
