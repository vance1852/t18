package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dish_bom")
public class DishBom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_id")
    private Long dishId;

    @Column(name = "material_id")
    private Long materialId;

    private Double quantity;
}
