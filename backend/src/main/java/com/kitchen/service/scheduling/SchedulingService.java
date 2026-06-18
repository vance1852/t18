package com.kitchen.service.scheduling;

import com.kitchen.dto.ScheduleResultDTO;
import com.kitchen.dto.ScheduleTaskDTO;
import com.kitchen.entity.*;
import com.kitchen.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final ProductionOrderRepository orderRepository;
    private final ScheduleTaskRepository taskRepository;
    private final DishRepository dishRepository;
    private final DishProcessRepository dishProcessRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    @Transactional
    public ScheduleResultDTO generateSchedule(List<Long> orderIds) {
        List<ProductionOrder> orders;
        if (orderIds == null || orderIds.isEmpty()) {
            orders = orderRepository.findByStatusIn(
                Arrays.asList(ProductionOrder.OrderStatus.PENDING, ProductionOrder.OrderStatus.SCHEDULED)
            );
        } else {
            orders = orderRepository.findAllById(orderIds);
        }

        orders.sort(Comparator.comparing(ProductionOrder::getDeliveryEndTime));

        for (ProductionOrder order : orders) {
            taskRepository.deleteByOrderId(order.getId());
        }

        Map<Long, List<ScheduleTask>> equipmentTasks = new HashMap<>();
        List<Equipment> allEquipment = equipmentRepository.findByStatus(Equipment.EquipmentStatus.AVAILABLE);
        for (Equipment eq : allEquipment) {
            equipmentTasks.put(eq.getId(), new ArrayList<>());
        }

        Map<Long, ScheduleTask> orderLastTasks = new HashMap<>();
        List<Long> delayedOrderIds = new ArrayList<>();

        for (ProductionOrder order : orders) {
            List<DishProcess> processes = dishProcessRepository.findByDishIdOrderBySequence(order.getDishId());
            if (processes.isEmpty()) {
                continue;
            }

            Dish dish = dishRepository.findById(order.getDishId()).orElse(null);
            String dishName = dish != null ? dish.getName() : "";

            LocalDateTime prevTaskEndTime = null;
            boolean orderDelayed = false;

            for (DishProcess process : processes) {
                List<Equipment> availableEquipment = equipmentRepository
                    .findByEquipmentTypeIdAndStatus(process.getEquipmentTypeId(), Equipment.EquipmentStatus.AVAILABLE);

                if (availableEquipment.isEmpty()) {
                    continue;
                }

                Equipment bestEquipment = null;
                LocalDateTime bestStartTime = null;
                int minSetupTime = Integer.MAX_VALUE;

                for (Equipment eq : availableEquipment) {
                    List<ScheduleTask> eqTasks = equipmentTasks.get(eq.getId());

                    LocalDateTime eqAvailableTime;
                    int setupTime = process.getSetupTime();

                    if (!eqTasks.isEmpty()) {
                        ScheduleTask lastTask = eqTasks.get(eqTasks.size() - 1);
                        eqAvailableTime = lastTask.getEndTime();

                        if (lastTask.getDishId().equals(order.getDishId())) {
                            setupTime = 0;
                        }
                    } else {
                        eqAvailableTime = LocalDateTime.now().withHour(6).withMinute(0).withSecond(0).withNano(0);
                    }

                    LocalDateTime startTime;
                    if (prevTaskEndTime != null && prevTaskEndTime.isAfter(eqAvailableTime)) {
                        startTime = prevTaskEndTime;
                        if (!eqTasks.isEmpty()) {
                            ScheduleTask lastTask = eqTasks.get(eqTasks.size() - 1);
                            if (!lastTask.getDishId().equals(order.getDishId())) {
                                setupTime = process.getSetupTime();
                            }
                        }
                    } else {
                        startTime = eqAvailableTime;
                    }

                    if (startTime.isBefore(bestStartTime) || bestStartTime == null) {
                        bestStartTime = startTime;
                        bestEquipment = eq;
                        minSetupTime = setupTime;
                    } else if (startTime.equals(bestStartTime) && setupTime < minSetupTime) {
                        bestEquipment = eq;
                        minSetupTime = setupTime;
                    }
                }

                if (bestEquipment == null) {
                    continue;
                }

                ScheduleTask task = new ScheduleTask();
                task.setOrderId(order.getId());
                task.setDishId(order.getDishId());
                task.setDishName(dishName);
                task.setDishProcessId(process.getId());
                task.setProcessName(process.getProcessName());
                task.setEquipmentId(bestEquipment.getId());
                task.setEquipmentName(bestEquipment.getName());
                task.setStartTime(bestStartTime);
                task.setSetupEndTime(bestStartTime.plusMinutes(minSetupTime));
                task.setEndTime(bestStartTime.plusMinutes(minSetupTime + process.getStandardTime()));
                task.setStatus(ScheduleTask.TaskStatus.PENDING);

                task = taskRepository.save(task);
                equipmentTasks.get(bestEquipment.getId()).add(task);
                prevTaskEndTime = task.getEndTime();
                orderLastTasks.put(order.getId(), task);
            }

            ScheduleTask lastTask = orderLastTasks.get(order.getId());
            if (lastTask != null && lastTask.getEndTime().isAfter(order.getDeliveryEndTime())) {
                orderDelayed = true;
                delayedOrderIds.add(order.getId());
            }

            order.setStatus(orderDelayed ? ProductionOrder.OrderStatus.DELAYED : ProductionOrder.OrderStatus.SCHEDULED);
            orderRepository.save(order);
        }

        ScheduleResultDTO result = new ScheduleResultDTO();
        result.setTasks(convertToDTO(taskRepository.findAll()));
        result.setTotalTasks(result.getTasks().size());
        result.setDelayedOrders(delayedOrderIds.size());
        result.setDelayedOrderIds(delayedOrderIds);

        return result;
    }

    public ScheduleResultDTO optimizeSchedule() {
        List<ScheduleTask> tasks = taskRepository.findAll();
        if (tasks.isEmpty()) {
            return getSchedule();
        }

        Map<Long, List<ScheduleTask>> equipmentTaskMap = tasks.stream()
            .collect(Collectors.groupingBy(ScheduleTask::getEquipmentId));

        boolean improved = true;
        int iterations = 0;
        int maxIterations = 100;

        while (improved && iterations < maxIterations) {
            improved = false;
            iterations++;

            for (Map.Entry<Long, List<ScheduleTask>> entry : equipmentTaskMap.entrySet()) {
                List<ScheduleTask> eqTasks = entry.getValue();
                eqTasks.sort(Comparator.comparing(ScheduleTask::getStartTime));

                for (int i = 0; i < eqTasks.size() - 1; i++) {
                    ScheduleTask task1 = eqTasks.get(i);
                    ScheduleTask task2 = eqTasks.get(i + 1);

                    if (canSwap(task1, task2, eqTasks, i)) {
                        int originalDelay = calculateTaskDelay(task1) + calculateTaskDelay(task2);

                        swapTasks(task1, task2, eqTasks, i);

                        int newDelay = calculateTaskDelay(task1) + calculateTaskDelay(task2);

                        if (newDelay < originalDelay) {
                            improved = true;
                            taskRepository.save(task1);
                            taskRepository.save(task2);
                        } else {
                            swapTasks(task1, task2, eqTasks, i);
                        }
                    }
                }
            }
        }

        return getSchedule();
    }

    private boolean canSwap(ScheduleTask task1, ScheduleTask task2, List<ScheduleTask> eqTasks, int index) {
        return task1.getDishId() != null && task2.getDishId() != null;
    }

    private void swapTasks(ScheduleTask task1, ScheduleTask task2, List<ScheduleTask> eqTasks, int index) {
        LocalDateTime tempStart = task1.getStartTime();
        LocalDateTime tempEnd = task1.getEndTime();
        LocalDateTime tempSetupEnd = task1.getSetupEndTime();
        String tempDishName = task1.getDishName();
        Long tempDishId = task1.getDishId();
        String tempProcessName = task1.getProcessName();
        Long tempDishProcessId = task1.getDishProcessId();
        Long tempOrderId = task1.getOrderId();

        task1.setStartTime(task2.getStartTime());
        task1.setEndTime(task2.getEndTime());
        task1.setSetupEndTime(task2.getSetupEndTime());
        task1.setDishName(task2.getDishName());
        task1.setDishId(task2.getDishId());
        task1.setProcessName(task2.getProcessName());
        task1.setDishProcessId(task2.getDishProcessId());
        task1.setOrderId(task2.getOrderId());

        task2.setStartTime(tempStart);
        task2.setEndTime(tempEnd);
        task2.setSetupEndTime(tempSetupEnd);
        task2.setDishName(tempDishName);
        task2.setDishId(tempDishId);
        task2.setProcessName(tempProcessName);
        task2.setDishProcessId(tempDishProcessId);
        task2.setOrderId(tempOrderId);

        eqTasks.set(index, task2);
        eqTasks.set(index + 1, task1);
    }

    private int calculateTaskDelay(ScheduleTask task) {
        ProductionOrder order = orderRepository.findById(task.getOrderId()).orElse(null);
        if (order == null) {
            return 0;
        }
        if (task.getEndTime().isAfter(order.getDeliveryEndTime())) {
            return (int) java.time.Duration.between(order.getDeliveryEndTime(), task.getEndTime()).toMinutes();
        }
        return 0;
    }

    @Transactional
    public ScheduleTaskDTO rescheduleTask(Long taskId, Long newEquipmentId, LocalDateTime newStartTime) {
        ScheduleTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        Equipment equipment = equipmentRepository.findById(newEquipmentId)
            .orElseThrow(() -> new RuntimeException("Equipment not found"));

        DishProcess process = dishProcessRepository.findById(task.getDishProcessId())
            .orElseThrow(() -> new RuntimeException("Dish process not found"));

        boolean hasConflict = taskRepository.existsConflictingTask(
            taskId, newEquipmentId, newStartTime,
            newStartTime.plusMinutes(process.getSetupTime() + process.getStandardTime())
        );

        if (hasConflict) {
            throw new RuntimeException("Time conflict with existing tasks");
        }

        task.setEquipmentId(newEquipmentId);
        task.setEquipmentName(equipment.getName());
        task.setStartTime(newStartTime);
        task.setSetupEndTime(newStartTime.plusMinutes(process.getSetupTime()));
        task.setEndTime(newStartTime.plusMinutes(process.getSetupTime() + process.getStandardTime()));

        task = taskRepository.save(task);

        updateOrderStatus(task.getOrderId());

        return convertToDTO(task);
    }

    private void updateOrderStatus(Long orderId) {
        ProductionOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }

        List<ScheduleTask> tasks = taskRepository.findByOrderIdOrderByStartTime(orderId);
        if (tasks.isEmpty()) {
            return;
        }

        ScheduleTask lastTask = tasks.get(tasks.size() - 1);
        if (lastTask.getEndTime().isAfter(order.getDeliveryEndTime())) {
            order.setStatus(ProductionOrder.OrderStatus.DELAYED);
        } else {
            order.setStatus(ProductionOrder.OrderStatus.SCHEDULED);
        }
        orderRepository.save(order);
    }

    public ScheduleResultDTO getSchedule() {
        List<ScheduleTask> tasks = taskRepository.findAll();
        tasks.sort(Comparator.comparing(ScheduleTask::getStartTime));

        List<Long> delayedOrderIds = new ArrayList<>();
        Map<Long, ScheduleTask> orderLastTask = new HashMap<>();

        for (ScheduleTask task : tasks) {
            ScheduleTask current = orderLastTask.get(task.getOrderId());
            if (current == null || task.getEndTime().isAfter(current.getEndTime())) {
                orderLastTask.put(task.getOrderId(), task);
            }
        }

        for (Map.Entry<Long, ScheduleTask> entry : orderLastTask.entrySet()) {
            ProductionOrder order = orderRepository.findById(entry.getKey()).orElse(null);
            if (order != null && entry.getValue().getEndTime().isAfter(order.getDeliveryEndTime())) {
                delayedOrderIds.add(entry.getKey());
            }
        }

        ScheduleResultDTO result = new ScheduleResultDTO();
        result.setTasks(convertToDTO(tasks));
        result.setTotalTasks(tasks.size());
        result.setDelayedOrders(delayedOrderIds.size());
        result.setDelayedOrderIds(delayedOrderIds);

        return result;
    }

    private List<ScheduleTaskDTO> convertToDTO(List<ScheduleTask> tasks) {
        return tasks.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ScheduleTaskDTO convertToDTO(ScheduleTask task) {
        ScheduleTaskDTO dto = new ScheduleTaskDTO();
        dto.setId(task.getId());
        dto.setOrderId(task.getOrderId());
        dto.setDishId(task.getDishId());
        dto.setDishName(task.getDishName());
        dto.setDishProcessId(task.getDishProcessId());
        dto.setProcessName(task.getProcessName());
        dto.setEquipmentId(task.getEquipmentId());
        dto.setEquipmentName(task.getEquipmentName());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setSetupEndTime(task.getSetupEndTime());
        dto.setStatus(task.getStatus().name());

        ProductionOrder order = orderRepository.findById(task.getOrderId()).orElse(null);
        if (order != null) {
            dto.setOrderNo(order.getOrderNo());
            dto.setIsDelayed(task.getEndTime().isAfter(order.getDeliveryEndTime()));
        }

        return dto;
    }
}
