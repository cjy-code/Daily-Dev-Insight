package com.dailydevinsight.repository;

import com.dailydevinsight.entity.DailyTrendInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTrendInsightRepository extends JpaRepository<DailyTrendInsight, Long> {

    /**
     * @date 2026-08-13
     * @desc 기준일과 일치하는 일일 개발 트렌드를 조회합니다.
     */
    Optional<DailyTrendInsight> findByTrendDate(LocalDate trendDate);

    /**
     * @date 2026-08-13
     * @desc 사용자에게 노출 가능한 최신 일일 개발 트렌드를 조회합니다.
     */
    Optional<DailyTrendInsight> findTopByVisibleTrueOrderByTrendDateDescIdDesc();

    /**
     * @date 2026-08-13
     * @desc 관리자 화면에 표시할 최신 일일 개발 트렌드를 조회합니다.
     */
    Optional<DailyTrendInsight> findTopByOrderByTrendDateDescIdDesc();

    /**
     * @date 2026-08-13
     * @desc 관리자 화면 이력에 표시할 최근 일일 개발 트렌드 목록을 조회합니다.
     */
    List<DailyTrendInsight> findTop5ByOrderByTrendDateDescIdDesc();
}
