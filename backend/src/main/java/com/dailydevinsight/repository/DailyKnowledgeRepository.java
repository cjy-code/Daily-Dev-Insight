package com.dailydevinsight.repository;

import com.dailydevinsight.entity.DailyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyKnowledgeRepository extends JpaRepository<DailyKnowledge, Long> {

    Optional<DailyKnowledge> findTopByKnowledgeDateOrderByIdDesc(LocalDate knowledgeDate);

    List<DailyKnowledge> findByKnowledgeDateBetweenOrderByKnowledgeDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop6ByOrderByViewCountDescIdDesc();
}
