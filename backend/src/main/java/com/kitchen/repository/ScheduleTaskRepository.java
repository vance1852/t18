package com.kitchen.repository;

import com.kitchen.entity.ScheduleTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleTaskRepository extends JpaRepository<ScheduleTask, Long> {
    List<ScheduleTask> findByOrderIdOrderByStartTime(Long orderId);
    List<ScheduleTask> findByEquipmentIdOrderByStartTime(Long equipmentId);

    @Query("SELECT st FROM ScheduleTask st WHERE st.startTime >= :start AND st.endTime <= :end ORDER BY st.startTime")
    List<ScheduleTask> findByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT st FROM ScheduleTask st WHERE st.equipmentId = :equipmentId AND st.startTime < :endTime AND st.endTime > :startTime ORDER BY st.startTime")
    List<ScheduleTask> findConflictingTasks(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime);

    @Modifying
    @Transactional
    void deleteByOrderId(Long orderId);

    @Query("SELECT COUNT(st) > 0 FROM ScheduleTask st WHERE st.equipmentId = :equipmentId AND st.startTime < :endTime AND st.endTime > :startTime AND st.id <> :taskId")
    boolean existsConflictingTask(Long taskId, Long equipmentId, LocalDateTime startTime, LocalDateTime endTime);
}
