package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.GenerationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationScheduleRepository extends JpaRepository<GenerationSchedule, Long> {
}
