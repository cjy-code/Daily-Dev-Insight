package com.dailydevinsight.repository;

import com.dailydevinsight.entity.WeeklyAiInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyAiInsightRepository extends JpaRepository<WeeklyAiInsight, Long> {

    /**
     * @date 2026-05-08
     * @desc 분석 시작일과 종료일이 일치하는 주간 AI 인사이트를 조회합니다.
     */
    Optional<WeeklyAiInsight> findByWeekStartDateAndWeekEndDate(LocalDate weekStartDate, LocalDate weekEndDate);

    /**
     * @date 2026-05-08
     * @desc 사용자에게 노출 가능한 최신 주간 AI 인사이트를 조회합니다.
     */
    Optional<WeeklyAiInsight> findTopByVisibleTrueOrderByWeekEndDateDescIdDesc();

    /**
     * @date 2026-05-08
     * @desc 관리자 화면에 표시할 최신 주간 AI 인사이트를 조회합니다.
     */
    Optional<WeeklyAiInsight> findTopByOrderByWeekEndDateDescIdDesc();

    /**
     * @date 2026-05-08
     * @desc 관리자 화면 이력에 표시할 최근 주간 AI 인사이트 목록을 조회합니다.
     */
    List<WeeklyAiInsight> findTop5ByOrderByWeekEndDateDescIdDesc();
}
