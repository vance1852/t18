package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dish")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private String category;
}
