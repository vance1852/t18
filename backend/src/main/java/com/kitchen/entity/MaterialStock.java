package com.kitchen.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "material_stock")
public class MaterialStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id")
    private Long materialId;

    private Double quantity;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "inbound_date")
    private LocalDate inboundDate;
}
