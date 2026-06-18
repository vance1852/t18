package com.kitchen.controller;

import com.kitchen.dto.Result;
import com.kitchen.entity.Equipment;
import com.kitchen.entity.EquipmentType;
import com.kitchen.repository.EquipmentRepository;
import com.kitchen.repository.EquipmentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    @GetMapping
    public Result<List<Equipment>> list() {
        return Result.success(equipmentRepository.findAll());
    }

    @GetMapping("/types")
    public Result<List<EquipmentType>> listTypes() {
        return Result.success(equipmentTypeRepository.findAll());
    }

    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        return equipmentRepository.findById(id)
            .map(Result::success)
            .orElse(Result.error("设备不存在"));
    }
}
