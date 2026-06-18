package com.kitchen.controller;

import com.kitchen.dto.Result;
import com.kitchen.entity.DeliveryBatch;
import com.kitchen.service.delivery.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/plan")
    public Result<List<DeliveryBatch>> getPlan() {
        try {
            return Result.success(deliveryService.getDeliveryPlan());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/plan/generate")
    public Result<List<DeliveryBatch>> generatePlan() {
        try {
            return Result.success(deliveryService.generateDeliveryPlan());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
