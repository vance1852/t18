package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dish_process")
public class DishProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_id")
    private Long dishId;

    @Column(name = "process_name")
    private String processName;

    @Column(name = "equipment_type_id")
    private Long equipmentTypeId;

    @Column(name = "standard_time")
    private Integer standardTime;

    @Column(name = "setup_time")
    private Integer setupTime;

    private Integer sequence;
}
