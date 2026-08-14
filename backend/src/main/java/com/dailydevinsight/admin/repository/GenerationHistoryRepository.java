package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.GenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

    Optional<GenerationHistory> findTopByOrderByIdDesc();

    List<GenerationHistory> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(String status);

    /**
     * @date 2026-08-12
     * @desc 삭제되는 일일 지식 게시물을 참조하는 이력의 참조를 해제합니다(이력 자체는 보존).
     */
    @Modifying
    @Query("UPDATE GenerationHistory g SET g.createdKnowledgeId = NULL WHERE g.createdKnowledgeId = :knowledgeId")
    void clearCreatedKnowledgeId(Long knowledgeId);
}
