package com.kitchen.controller;

import com.kitchen.dto.OrderCreateRequest;
import com.kitchen.dto.Result;
import com.kitchen.entity.ProductionOrder;
import com.kitchen.repository.ProductionOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ProductionOrderRepository orderRepository;

    @GetMapping
    public Result<List<ProductionOrder>> list() {
        List<ProductionOrder> orders = orderRepository.findAll();
        orders.sort((a, b) -> b.getDeliveryEndTime().compareTo(a.getDeliveryEndTime()));
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    public Result<ProductionOrder> getById(@PathVariable Long id) {
        return orderRepository.findById(id)
            .map(Result::success)
            .orElse(Result.error("订单不存在"));
    }

    @PostMapping
    public Result<ProductionOrder> create(@RequestBody OrderCreateRequest request) {
        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(request.getOrderNo());
        order.setDishId(request.getDishId());
        order.setQuantity(request.getQuantity());
        order.setDeliveryStartTime(request.getDeliveryStartTime());
        order.setDeliveryEndTime(request.getDeliveryEndTime());
        order.setDeliveryArea(request.getDeliveryArea());
        order.setStatus(ProductionOrder.OrderStatus.PENDING);

        order = orderRepository.save(order);
        return Result.success(order);
    }

    @PutMapping("/{id}")
    public Result<ProductionOrder> update(@PathVariable Long id, @RequestBody OrderCreateRequest request) {
        return orderRepository.findById(id)
            .map(order -> {
                order.setOrderNo(request.getOrderNo());
                order.setDishId(request.getDishId());
                order.setQuantity(request.getQuantity());
                order.setDeliveryStartTime(request.getDeliveryStartTime());
                order.setDeliveryEndTime(request.getDeliveryEndTime());
                order.setDeliveryArea(request.getDeliveryArea());
                return Result.success(orderRepository.save(order));
            })
            .orElse(Result.error("订单不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return Result.success();
        }
        return Result.error("订单不存在");
    }
}
