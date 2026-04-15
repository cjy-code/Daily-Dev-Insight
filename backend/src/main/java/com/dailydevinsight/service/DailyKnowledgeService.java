package com.dailydevinsight.service;

import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyKnowledgeService {

    private final DailyKnowledgeRepository dailyKnowledgeRepository;

    /**
     * @date 2026-04-13
     * @desc 기준일의 개발 인사이트 1건을 조회합니다.
     */
    public Optional<DailyKnowledge> findTodayKnowledge(LocalDate targetDate) {
        return dailyKnowledgeRepository.findTopByKnowledgeDateOrderByIdDesc(targetDate);
    }

    /**
     * @date 2026-04-13
     * @desc 시작일~종료일 범위의 개발 인사이트 목록을 조회합니다.
     */
    public List<DailyKnowledge> findKnowledgeByDateRange(LocalDate startDate, LocalDate endDate) {
        return dailyKnowledgeRepository.findByKnowledgeDateBetweenOrderByKnowledgeDateDescIdDesc(startDate, endDate);
    }

    /**
     * @date 2026-04-13
     * @desc 기준일이 속한 주(월~일)에서 조회수 기준 TOP10을 조회합니다.
     */
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, key = "#referenceDate.toString()")
    public List<DailyKnowledge> findWeeklyHotKnowledgeTop10(LocalDate referenceDate) {
        LocalDate weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        return dailyKnowledgeRepository.findTop10ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, weekEnd);
    }

    /**
     * @date 2026-04-13
     * @desc 기준일이 속한 주(월~일)에서 조회수 기준 TOP5를 조회합니다.
     */
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, key = "#referenceDate.toString()")
    public List<DailyKnowledge> findWeeklyHotKnowledgeTop5(LocalDate referenceDate) {
        LocalDate weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        return dailyKnowledgeRepository.findTop5ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, weekEnd);
    }
}
