package com.kitchen.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleResultDTO {
    private List<ScheduleTaskDTO> tasks;
    private Integer totalTasks;
    private Integer delayedOrders;
    private List<Long> delayedOrderIds;
}
