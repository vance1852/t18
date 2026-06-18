package com.kitchen.repository;

import com.kitchen.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByEquipmentTypeId(Long equipmentTypeId);
    List<Equipment> findByStatus(Equipment.EquipmentStatus status);
    List<Equipment> findByEquipmentTypeIdAndStatus(Long equipmentTypeId, Equipment.EquipmentStatus status);
}
