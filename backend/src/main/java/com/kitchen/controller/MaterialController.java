package com.kitchen.controller;

import com.kitchen.dto.MaterialAvailabilityDTO;
import com.kitchen.dto.Result;
import com.kitchen.entity.Material;
import com.kitchen.entity.MaterialStock;
import com.kitchen.repository.MaterialRepository;
import com.kitchen.repository.MaterialStockRepository;
import com.kitchen.service.material.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialRepository materialRepository;
    private final MaterialStockRepository stockRepository;

    @GetMapping
    public Result<List<Material>> listMaterials() {
        return Result.success(materialRepository.findAll());
    }

    @GetMapping("/availability")
    public Result<List<MaterialAvailabilityDTO>> checkAvailability(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(materialService.checkMaterialAvailability(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/stock")
    public Result<List<MaterialStock>> getStock(@PathVariable Long id) {
        return Result.success(stockRepository.findByMaterialIdOrderByExpireDateAsc(id));
    }
}
