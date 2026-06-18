package com.kitchen.controller;

import com.kitchen.dto.Result;
import com.kitchen.entity.Dish;
import com.kitchen.entity.DishProcess;
import com.kitchen.repository.DishProcessRepository;
import com.kitchen.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishRepository dishRepository;
    private final DishProcessRepository dishProcessRepository;

    @GetMapping
    public Result<List<Dish>> list() {
        return Result.success(dishRepository.findAll());
    }

    @GetMapping("/{id}")
    public Result<Dish> getById(@PathVariable Long id) {
        return dishRepository.findById(id)
            .map(Result::success)
            .orElse(Result.error("菜品不存在"));
    }

    @GetMapping("/{id}/processes")
    public Result<List<DishProcess>> getProcesses(@PathVariable Long id) {
        return Result.success(dishProcessRepository.findByDishIdOrderBySequence(id));
    }
}
