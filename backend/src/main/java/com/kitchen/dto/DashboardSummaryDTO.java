package com.kitchen.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSummaryDTO {
    private List<EquipmentUtilizationDTO> equipmentUtilizations;
    private Double onTimeDeliveryRate;
    private EquipmentUtilizationDTO bottleneckEquipment;
    private Double materialTurnoverRate;
    private Integer dailyOutput;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer delayedOrders;
    private Double avgEquipmentUtilization;
    private Double materialAvailabilityRate;
}
