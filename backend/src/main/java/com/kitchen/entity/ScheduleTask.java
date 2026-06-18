package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schedule_task")
public class ScheduleTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "dish_id")
    private Long dishId;

    @Column(name = "dish_process_id")
    private Long dishProcessId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "setup_end_time")
    private LocalDateTime setupEndTime;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "process_name")
    private String processName;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name = "equipment_name")
    private String equipmentName;

    public enum TaskStatus {
        PENDING, PRODUCING, COMPLETED, DELAYED
    }
}
