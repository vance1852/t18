package com.kitchen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRescheduleRequest {
    private Long newEquipmentId;
    private LocalDateTime newStartTime;
}
