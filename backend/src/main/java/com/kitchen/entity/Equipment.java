package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "equipment_type_id")
    private Long equipmentTypeId;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;

    public enum EquipmentStatus {
        AVAILABLE, MAINTENANCE
    }
}
