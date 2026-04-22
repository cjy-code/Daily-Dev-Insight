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

    Optional<DailyKnowledge> findTopByOrderByIdDesc();

    Optional<DailyKnowledge> findTopByKnowledgeDateOrderByIdDesc(LocalDate knowledgeDate);

    List<DailyKnowledge> findByKnowledgeDateBetweenOrderByKnowledgeDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop10ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop5ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(LocalDate startDate, LocalDate endDate);

    List<DailyKnowledge> findTop30ByOrderByKnowledgeDateDescIdDesc();

    List<DailyKnowledge> findTop5ByOrderByViewCountDescIdDesc();

    long countByKnowledgeDate(LocalDate knowledgeDate);

    @Query("select coalesce(sum(d.viewCount), 0) from DailyKnowledge d")
    long sumViewCount();

    @Modifying
    @Query("update DailyKnowledge d set d.viewCount = coalesce(d.viewCount, 0) + 1 where d.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
