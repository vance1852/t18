package com.kitchen.repository;

import com.kitchen.entity.DishBom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishBomRepository extends JpaRepository<DishBom, Long> {
    List<DishBom> findByDishId(Long dishId);
}
