package com.kitchen.controller;

import com.kitchen.dto.DashboardSummaryDTO;
import com.kitchen.dto.EquipmentUtilizationDTO;
import com.kitchen.dto.Result;
import com.kitchen.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<DashboardSummaryDTO> getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getDashboardSummary(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/equipment-utilization")
    public Result<List<EquipmentUtilizationDTO>> getEquipmentUtilization(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getEquipmentUtilization(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/on-time-delivery-rate")
    public Result<Double> getOnTimeDeliveryRate(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getOnTimeDeliveryRate(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/bottleneck-equipment")
    public Result<EquipmentUtilizationDTO> getBottleneckEquipment(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getBottleneckEquipment(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/material-turnover")
    public Result<Double> getMaterialTurnover(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getMaterialTurnover(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/daily-output")
    public Result<Integer> getDailyOutput(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            return Result.success(dashboardService.getDailyOutput(date));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
