package com.kitchen.repository;

import com.kitchen.entity.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {
    EquipmentType findByName(String name);
}
