package com.kitchen.service.material;

import com.kitchen.dto.MaterialAvailabilityDTO;
import com.kitchen.entity.*;
import com.kitchen.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialStockRepository stockRepository;
    private final DishBomRepository bomRepository;
    private final ScheduleTaskRepository taskRepository;
    private final ProductionOrderRepository orderRepository;
    private final DishProcessRepository processRepository;

    public List<MaterialAvailabilityDTO> checkMaterialAvailability(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<ScheduleTask> tasks = taskRepository.findByDateRange(startOfDay, endOfDay);

        Map<Long, List<ScheduleTask>> orderTasks = tasks.stream()
            .collect(Collectors.groupingBy(ScheduleTask::getOrderId));

        Map<Long, Map<Long, Double>> materialRequirements = new HashMap<>();
        Map<Long, LocalDateTime> materialShortageTime = new HashMap<>();

        for (Map.Entry<Long, List<ScheduleTask>> entry : orderTasks.entrySet()) {
            Long orderId = entry.getKey();
            List<ScheduleTask> orderTaskList = entry.getValue();

            ProductionOrder order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                continue;
            }

            ScheduleTask firstTask = orderTaskList.stream()
                .filter(t -> t.getStartTime() != null)
                .min(Comparator.comparing(ScheduleTask::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

            if (firstTask == null) {
                continue;
            }

            List<DishBom> boms = bomRepository.findByDishId(order.getDishId());
            for (DishBom bom : boms) {
                Long materialId = bom.getMaterialId();
                Double requiredQty = bom.getQuantity() * order.getQuantity();

                materialRequirements
                    .computeIfAbsent(materialId, k -> new HashMap<>())
                    .merge(orderId, requiredQty, Double::sum);

                LocalDateTime currentShortage = materialShortageTime.get(materialId);
                if (currentShortage == null || firstTask.getStartTime().isBefore(currentShortage)) {
                    materialShortageTime.put(materialId, firstTask.getStartTime());
                }
            }
        }

        List<MaterialAvailabilityDTO> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : materialRequirements.entrySet()) {
            Long materialId = entry.getKey();
            double totalRequired = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();

            Material material = materialRepository.findById(materialId).orElse(null);
            if (material == null) {
                continue;
            }

            List<MaterialStock> stocks = stockRepository.findAvailableStock(materialId, date);
            double totalStock = stocks.stream().mapToDouble(MaterialStock::getQuantity).sum();

            double shortage = Math.max(0, totalRequired - totalStock);

            LocalDateTime shortageTime = null;
            if (shortage > 0) {
                double remainingStock = totalStock;
                List<Map.Entry<Long, Double>> sortedRequirements = entry.getValue().entrySet().stream()
                    .sorted(Comparator.comparing(e -> {
                        List<ScheduleTask> taskList = orderTasks.get(e.getKey());
                        if (taskList == null || taskList.isEmpty()) return LocalDateTime.MAX;
                        return taskList.stream()
                            .filter(t -> t.getStartTime() != null)
                            .min(Comparator.comparing(ScheduleTask::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(ScheduleTask::getStartTime)
                            .orElse(LocalDateTime.MAX);
                    }))
                    .collect(Collectors.toList());

                for (Map.Entry<Long, Double> reqEntry : sortedRequirements) {
                    remainingStock -= reqEntry.getValue();
                    if (remainingStock < 0) {
                        Long orderId = reqEntry.getKey();
                        List<ScheduleTask> taskList = orderTasks.get(orderId);
                        if (taskList != null && !taskList.isEmpty()) {
                            ScheduleTask firstTask = taskList.stream()
                                .filter(t -> t.getStartTime() != null)
                                .min(Comparator.comparing(ScheduleTask::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                                .orElse(null);
                            if (firstTask != null) {
                                shortageTime = firstTask.getStartTime();
                            }
                        }
                        break;
                    }
                }
            }

            MaterialAvailabilityDTO dto = new MaterialAvailabilityDTO();
            dto.setMaterialId(materialId);
            dto.setMaterialName(material.getName());
            dto.setMaterialCode(material.getCode());
            dto.setUnit(material.getUnit());
            dto.setRequiredQuantity(totalRequired);
            dto.setStockQuantity(totalStock);
            dto.setShortageQuantity(shortage);
            dto.setShortageTime(shortageTime);

            if (shortage > 0) {
                dto.setStatus("SHORTAGE");
            } else if (totalStock < totalRequired * 1.2) {
                dto.setStatus("TIGHT");
            } else {
                dto.setStatus("SUFFICIENT");
            }

            result.add(dto);
        }

        result.sort(Comparator.comparing(MaterialAvailabilityDTO::getMaterialName));

        return result;
    }
}
