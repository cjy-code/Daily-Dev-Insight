package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.DailyTrendGenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyTrendGenerationHistoryRepository extends JpaRepository<DailyTrendGenerationHistory, Long> {

    List<DailyTrendGenerationHistory> findTop20ByOrderByCreatedAtDesc();
}
