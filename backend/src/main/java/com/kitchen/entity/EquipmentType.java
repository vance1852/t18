package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "equipment_type")
public class EquipmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;
}
