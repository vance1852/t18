package com.kitchen.controller;

import com.kitchen.dto.GenerateScheduleRequest;
import com.kitchen.dto.Result;
import com.kitchen.dto.ScheduleResultDTO;
import com.kitchen.dto.TaskRescheduleRequest;
import com.kitchen.dto.ScheduleTaskDTO;
import com.kitchen.service.scheduling.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SchedulingService schedulingService;

    @GetMapping
    public Result<ScheduleResultDTO> getSchedule() {
        try {
            return Result.success(schedulingService.getSchedule());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/generate")
    public Result<ScheduleResultDTO> generateSchedule(@RequestBody(required = false) GenerateScheduleRequest request) {
        try {
            ScheduleResultDTO result = schedulingService.generateSchedule(
                request != null ? request.getOrderIds() : null
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/optimize")
    public Result<ScheduleResultDTO> optimizeSchedule() {
        try {
            return Result.success(schedulingService.optimizeSchedule());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/tasks/{taskId}")
    public Result<ScheduleTaskDTO> rescheduleTask(
        @PathVariable Long taskId,
        @RequestBody TaskRescheduleRequest request
    ) {
        try {
            ScheduleTaskDTO result = schedulingService.rescheduleTask(
                taskId, request.getNewEquipmentId(), request.getNewStartTime()
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
