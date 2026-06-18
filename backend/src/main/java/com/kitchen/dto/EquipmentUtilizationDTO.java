package com.kitchen.dto;

import lombok.Data;

@Data
public class EquipmentUtilizationDTO {
    private Long equipmentId;
    private String equipmentName;
    private String equipmentTypeName;
    private Double utilizationRate;
    private Long workMinutes;
    private Long totalMinutes;
}
