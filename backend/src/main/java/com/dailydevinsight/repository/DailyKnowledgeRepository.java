package com.dailydevinsight.repository;

import com.dailydevinsight.entity.DailyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyKnowledgeRepository extends JpaRepository<DailyKnowledge, Long> {

    Optional<DailyKnowledge> findTopByKnowledgeDateOrderByIdDesc(LocalDate knowledgeDate);
}
