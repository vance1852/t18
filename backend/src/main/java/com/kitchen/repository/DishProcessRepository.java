package com.kitchen.repository;

import com.kitchen.entity.DishProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishProcessRepository extends JpaRepository<DishProcess, Long> {
    List<DishProcess> findByDishIdOrderBySequence(Long dishId);
}
