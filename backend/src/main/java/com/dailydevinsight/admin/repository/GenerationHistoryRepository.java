package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.GenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

    Optional<GenerationHistory> findTopByOrderByIdDesc();

    List<GenerationHistory> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
