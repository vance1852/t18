package com.kitchen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleTaskDTO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long dishId;
    private String dishName;
    private Long dishProcessId;
    private String processName;
    private Long equipmentId;
    private String equipmentName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime setupEndTime;
    private String status;
    private Boolean isDelayed;
}
