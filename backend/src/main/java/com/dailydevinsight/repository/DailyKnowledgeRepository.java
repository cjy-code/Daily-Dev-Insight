package com.dailydevinsight.repository;

import com.dailydevinsight.entity.DailyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyKnowledgeRepository extends JpaRepository<DailyKnowledge, Long> {

    Optional<DailyKnowledge> findTopByKnowledgeDateOrderByIdDesc(LocalDate knowledgeDate);

    List<DailyKnowledge> findByKnowledgeDateBetweenOrderByKnowledgeDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop10ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop5ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("update DailyKnowledge d set d.viewCount = coalesce(d.viewCount, 0) + 1 where d.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
