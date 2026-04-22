package com.dailydevinsight.service;

import com.dailydevinsight.repository.DailyKnowledgeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyKnowledgeServiceTest {

    /**
     * @date 2026-04-22
     * @desc TOP10 조회 시 주간 종료일이 오늘 이후면 오늘 날짜로 보정되는지 검증합니다.
     */
    @Test
    void findWeeklyHotKnowledgeTop10_ShouldClampWeekEndToToday() {
        DailyKnowledgeRepository repository = mock(DailyKnowledgeRepository.class);
        DailyKnowledgeService service = new DailyKnowledgeService(repository);

        LocalDate today = LocalDate.now();
        LocalDate futureReferenceDate = today.plusDays(5);
        LocalDate weekStart = futureReferenceDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        when(repository.findTop10ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, today))
                .thenReturn(Collections.emptyList());

        service.findWeeklyHotKnowledgeTop10(futureReferenceDate);

        verify(repository).findTop10ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, today);
    }

    /**
     * @date 2026-04-22
     * @desc TOP5 조회 시 주간 종료일이 오늘 이후면 오늘 날짜로 보정되는지 검증합니다.
     */
    @Test
    void findWeeklyHotKnowledgeTop5_ShouldClampWeekEndToToday() {
        DailyKnowledgeRepository repository = mock(DailyKnowledgeRepository.class);
        DailyKnowledgeService service = new DailyKnowledgeService(repository);

        LocalDate today = LocalDate.now();
        LocalDate futureReferenceDate = today.plusDays(5);
        LocalDate weekStart = futureReferenceDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        when(repository.findTop5ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, today))
                .thenReturn(Collections.emptyList());

        service.findWeeklyHotKnowledgeTop5(futureReferenceDate);

        verify(repository).findTop5ByKnowledgeDateBetweenOrderByViewCountDescIdDesc(weekStart, today);
    }
}
