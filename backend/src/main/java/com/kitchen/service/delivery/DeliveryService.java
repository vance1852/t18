package com.kitchen.service.delivery;

import com.kitchen.entity.DeliveryBatch;
import com.kitchen.entity.ProductionOrder;
import com.kitchen.entity.ScheduleTask;
import com.kitchen.repository.DeliveryBatchRepository;
import com.kitchen.repository.ProductionOrderRepository;
import com.kitchen.repository.ScheduleTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryBatchRepository batchRepository;
    private final ProductionOrderRepository orderRepository;
    private final ScheduleTaskRepository taskRepository;

    private static final int TRANSPORT_TIME_MINUTES = 60;
    private static final int MAX_BATCH_QUANTITY = 500;
    private static final String[] VEHICLES = {"京A12345", "京A67890", "京B11111", "京B22222", "京C33333"};

    @Transactional
    public List<DeliveryBatch> generateDeliveryPlan() {
        batchRepository.deleteAll();

        List<ProductionOrder> orders = orderRepository.findByStatusIn(
            Arrays.asList(ProductionOrder.OrderStatus.SCHEDULED, ProductionOrder.OrderStatus.PRODUCING)
        );

        Map<String, List<ProductionOrder>> areaGroups = orders.stream()
            .collect(Collectors.groupingBy(order -> 
                order.getDeliveryArea() != null ? order.getDeliveryArea() : "默认区域"
            ));

        List<DeliveryBatch> allBatches = new ArrayList<>();
        int vehicleIndex = 0;

        for (Map.Entry<String, List<ProductionOrder>> entry : areaGroups.entrySet()) {
            String area = entry.getKey();
            List<ProductionOrder> areaOrders = entry.getValue();

            areaOrders.sort(Comparator.comparing(ProductionOrder::getDeliveryStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

            List<List<ProductionOrder>> batches = splitIntoBatches(areaOrders);

            for (int i = 0; i < batches.size(); i++) {
                List<ProductionOrder> batchOrders = batches.get(i);

                LocalDateTime productionEndTime = getProductionEndTime(batchOrders);

                LocalDateTime deliveryStartTime = batchOrders.stream()
                    .map(ProductionOrder::getDeliveryStartTime)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().plusHours(2));

                LocalDateTime departureTime = deliveryStartTime.minusMinutes(TRANSPORT_TIME_MINUTES);

                if (departureTime.isBefore(productionEndTime)) {
                    departureTime = productionEndTime;
                }

                LocalDateTime arrivalTime = departureTime.plusMinutes(TRANSPORT_TIME_MINUTES);

                int totalQuantity = batchOrders.stream()
                    .mapToInt(ProductionOrder::getQuantity)
                    .sum();

                String orderIds = batchOrders.stream()
                    .map(ProductionOrder::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

                DeliveryBatch batch = new DeliveryBatch();
                batch.setBatchNo("BATCH-" + System.currentTimeMillis() + "-" + i);
                batch.setDeliveryArea(area);
                batch.setDepartureTime(departureTime);
                batch.setArrivalTime(arrivalTime);
                batch.setVehicleNo(VEHICLES[vehicleIndex % VEHICLES.length]);
                batch.setOrderIds(orderIds);
                batch.setTotalQuantity(totalQuantity);

                batch = batchRepository.save(batch);
                allBatches.add(batch);

                vehicleIndex++;
            }
        }

        allBatches.sort(Comparator.comparing(DeliveryBatch::getDepartureTime, Comparator.nullsLast(Comparator.naturalOrder())));

        return allBatches;
    }

    private List<List<ProductionOrder>> splitIntoBatches(List<ProductionOrder> orders) {
        List<List<ProductionOrder>> batches = new ArrayList<>();
        List<ProductionOrder> currentBatch = new ArrayList<>();
        int currentQuantity = 0;

        for (ProductionOrder order : orders) {
            if (currentQuantity + order.getQuantity() > MAX_BATCH_QUANTITY && !currentBatch.isEmpty()) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
                currentQuantity = 0;
            }
            currentBatch.add(order);
            currentQuantity += order.getQuantity();
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        return batches;
    }

    private LocalDateTime getProductionEndTime(List<ProductionOrder> orders) {
        LocalDateTime latestEndTime = LocalDateTime.now();

        for (ProductionOrder order : orders) {
            List<ScheduleTask> tasks = taskRepository.findByOrderIdOrderByStartTime(order.getId());
            if (!tasks.isEmpty()) {
                LocalDateTime orderEndTime = tasks.get(tasks.size() - 1).getEndTime();
                if (orderEndTime != null && orderEndTime.isAfter(latestEndTime)) {
                    latestEndTime = orderEndTime;
                }
            }
        }

        return latestEndTime;
    }

    public List<DeliveryBatch> getDeliveryPlan() {
        List<DeliveryBatch> batches = batchRepository.findAll();
        if (batches.isEmpty()) {
            return generateDeliveryPlan();
        }
        batches.sort(Comparator.comparing(DeliveryBatch::getDepartureTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return batches;
    }
}
