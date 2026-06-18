package com.kitchen.service.dashboard;

import com.kitchen.dto.DashboardSummaryDTO;
import com.kitchen.dto.EquipmentUtilizationDTO;
import com.kitchen.dto.MaterialAvailabilityDTO;
import com.kitchen.entity.*;
import com.kitchen.repository.*;
import com.kitchen.service.material.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final ScheduleTaskRepository taskRepository;
    private final ProductionOrderRepository orderRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockRepository stockRepository;
    private final DishBomRepository bomRepository;
    private final MaterialService materialService;

    public List<EquipmentUtilizationDTO> getEquipmentUtilization(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ScheduleTask> tasks = taskRepository.findByDateRange(startOfDay, endOfDay);

        Map<Long, Long> equipmentWorkMinutes = new HashMap<>();
        for (ScheduleTask task : tasks) {
            LocalDateTime taskStart = task.getStartTime().isBefore(startOfDay) ? startOfDay : task.getStartTime();
            LocalDateTime taskEnd = task.getEndTime().isAfter(endOfDay) ? endOfDay : task.getEndTime();

            if (taskEnd.isAfter(taskStart)) {
                long minutes = ChronoUnit.MINUTES.between(taskStart, taskEnd);
                equipmentWorkMinutes.merge(task.getEquipmentId(), minutes, Long::sum);
            }
        }

        long totalMinutes = ChronoUnit.MINUTES.between(startOfDay, endOfDay);

        List<Equipment> allEquipment = equipmentRepository.findAll();
        List<EquipmentUtilizationDTO> result = new ArrayList<>();

        for (Equipment eq : allEquipment) {
            EquipmentUtilizationDTO dto = new EquipmentUtilizationDTO();
            dto.setEquipmentId(eq.getId());
            dto.setEquipmentName(eq.getName());

            EquipmentType type = equipmentTypeRepository.findById(eq.getEquipmentTypeId()).orElse(null);
            dto.setEquipmentTypeName(type != null ? type.getName() : "");

            long workMinutes = equipmentWorkMinutes.getOrDefault(eq.getId(), 0L);
            dto.setWorkMinutes(workMinutes);
            dto.setTotalMinutes(totalMinutes);
            dto.setUtilizationRate(totalMinutes > 0 ? (double) workMinutes / totalMinutes * 100 : 0.0);

            result.add(dto);
        }

        result.sort(Comparator.comparing(EquipmentUtilizationDTO::getUtilizationRate).reversed());

        return result;
    }

    public Double getOnTimeDeliveryRate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ProductionOrder> orders = orderRepository.findByDeliveryEndTimeBetweenOrderByDeliveryEndTimeAsc(
            startOfDay, endOfDay
        );

        if (orders.isEmpty()) {
            return 100.0;
        }

        int onTimeCount = 0;
        for (ProductionOrder order : orders) {
            List<ScheduleTask> tasks = taskRepository.findByOrderIdOrderByStartTime(order.getId());
            if (tasks.isEmpty()) {
                continue;
            }
            ScheduleTask lastTask = tasks.get(tasks.size() - 1);
            if (!lastTask.getEndTime().isAfter(order.getDeliveryEndTime())) {
                onTimeCount++;
            }
        }

        return (double) onTimeCount / orders.size() * 100;
    }

    public EquipmentUtilizationDTO getBottleneckEquipment(LocalDate date) {
        List<EquipmentUtilizationDTO> utilizations = getEquipmentUtilization(date);
        return utilizations.isEmpty() ? null : utilizations.get(0);
    }

    public Double getMaterialTurnover(LocalDate date) {
        List<Material> materials = materialRepository.findAll();
        if (materials.isEmpty()) {
            return 0.0;
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ScheduleTask> tasks = taskRepository.findByDateRange(startOfDay, endOfDay);
        Set<Long> orderIds = tasks.stream()
            .map(ScheduleTask::getOrderId)
            .collect(Collectors.toSet());

        List<ProductionOrder> orders = orderRepository.findAllById(orderIds);

        Map<Long, Double> materialUsage = new HashMap<>();
        for (ProductionOrder order : orders) {
            List<DishBom> boms = bomRepository.findByDishId(order.getDishId());
            for (DishBom bom : boms) {
                materialUsage.merge(bom.getMaterialId(), bom.getQuantity() * order.getQuantity(), Double::sum);
            }
        }

        double totalUsage = materialUsage.values().stream().mapToDouble(Double::doubleValue).sum();

        double totalStock = 0;
        for (Material material : materials) {
            List<MaterialStock> stocks = stockRepository.findByMaterialIdOrderByExpireDateAsc(material.getId());
            totalStock += stocks.stream().mapToDouble(MaterialStock::getQuantity).sum();
        }

        if (totalStock == 0) {
            return 0.0;
        }

        return totalUsage / totalStock;
    }

    public Integer getDailyOutput(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ScheduleTask> tasks = taskRepository.findByDateRange(startOfDay, endOfDay);
        Set<Long> orderIds = tasks.stream()
            .map(ScheduleTask::getOrderId)
            .collect(Collectors.toSet());

        List<ProductionOrder> orders = orderRepository.findAllById(orderIds);
        return orders.stream().mapToInt(ProductionOrder::getQuantity).sum();
    }

    public DashboardSummaryDTO getDashboardSummary(LocalDate date) {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        List<EquipmentUtilizationDTO> utilizations = getEquipmentUtilization(date);
        summary.setEquipmentUtilizations(utilizations);

        double avgUtil = utilizations.stream()
            .mapToDouble(EquipmentUtilizationDTO::getUtilizationRate)
            .average()
            .orElse(0.0);
        summary.setAvgEquipmentUtilization(avgUtil);

        summary.setOnTimeDeliveryRate(getOnTimeDeliveryRate(date));
        summary.setBottleneckEquipment(getBottleneckEquipment(date));
        summary.setMaterialTurnoverRate(getMaterialTurnover(date));
        summary.setDailyOutput(getDailyOutput(date));

        List<MaterialAvailabilityDTO> materialAvailability = getMaterialAvailabilityList(date);
        long sufficientCount = materialAvailability.stream()
            .filter(m -> "SUFFICIENT".equals(m.getStatus()) || "TIGHT".equals(m.getStatus()))
            .count();
        double materialRate = materialAvailability.isEmpty() ? 100.0 :
            (double) sufficientCount / materialAvailability.size() * 100;
        summary.setMaterialAvailabilityRate(materialRate);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ProductionOrder> allOrders = orderRepository.findByDeliveryEndTimeBetweenOrderByDeliveryEndTimeAsc(
            startOfDay, endOfDay
        );
        summary.setTotalOrders(allOrders.size());

        int completedCount = 0;
        int delayedCount = 0;

        for (ProductionOrder order : allOrders) {
            List<ScheduleTask> tasks = taskRepository.findByOrderIdOrderByStartTime(order.getId());
            if (!tasks.isEmpty()) {
                ScheduleTask lastTask = tasks.get(tasks.size() - 1);
                if (lastTask.getEndTime().isAfter(order.getDeliveryEndTime())) {
                    delayedCount++;
                }
                if (lastTask.getStatus() == ScheduleTask.TaskStatus.COMPLETED) {
                    completedCount++;
                }
            }
        }

        summary.setCompletedOrders(completedCount);
        summary.setDelayedOrders(delayedCount);

        return summary;
    }

    private List<MaterialAvailabilityDTO> getMaterialAvailabilityList(LocalDate date) {
        try {
            return materialService.checkMaterialAvailability(date);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
