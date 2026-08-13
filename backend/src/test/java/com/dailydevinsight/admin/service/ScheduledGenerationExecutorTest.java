package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledGenerationExecutorTest {

    /**
     * @date 2026-08-13
     * @desc 트렌드 생성 예외가 발생해도 지식 생성과 성공 시각 기록을 계속 수행하는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldContinueKnowledgeGenerationWhenTrendFails() {
        GenerationScheduleService scheduleService = mock(GenerationScheduleService.class);
        DailyTrendInsightService trendService = mock(DailyTrendInsightService.class);
        DailyKnowledgeGenerationService knowledgeService = mock(DailyKnowledgeGenerationService.class);
        ScheduledGenerationExecutor executor = new ScheduledGenerationExecutor(
                scheduleService,
                trendService,
                knowledgeService
        );
        when(scheduleService.isExecutionDue(any(LocalDateTime.class))).thenReturn(true);
        when(trendService.generateScheduledDailyTrend(any(LocalDate.class)))
                .thenThrow(new IllegalStateException("트렌드 실패"));
        when(knowledgeService.executeScheduledGeneration(any(LocalDate.class)))
                .thenReturn(createExecutionResult(true));

        executor.executeScheduledGeneration();

        verify(knowledgeService).executeScheduledGeneration(any(LocalDate.class));
        verify(scheduleService).markExecuted(any(LocalDateTime.class));
    }

    /**
     * @date 2026-08-13
     * @desc 트렌드 생성 성공과 무관하게 지식 생성 실패 시 실행 시각을 기록하지 않는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldNotMarkExecutedWhenKnowledgeFailsAfterTrendSuccess() {
        GenerationScheduleService scheduleService = mock(GenerationScheduleService.class);
        DailyTrendInsightService trendService = mock(DailyTrendInsightService.class);
        DailyKnowledgeGenerationService knowledgeService = mock(DailyKnowledgeGenerationService.class);
        ScheduledGenerationExecutor executor = new ScheduledGenerationExecutor(
                scheduleService,
                trendService,
                knowledgeService
        );
        when(scheduleService.isExecutionDue(any(LocalDateTime.class))).thenReturn(true);
        when(knowledgeService.executeScheduledGeneration(any(LocalDate.class)))
                .thenReturn(createExecutionResult(false));

        executor.executeScheduledGeneration();

        verify(trendService).generateScheduledDailyTrend(any(LocalDate.class));
        verify(scheduleService, never()).markExecuted(any(LocalDateTime.class));
    }

    /**
     * @date 2026-08-13
     * @desc 트렌드와 지식 생성 모두 실패해도 실행 시각 판단은 지식 성공 값만 따르는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldNotMarkExecutedWhenTrendAndKnowledgeBothFail() {
        GenerationScheduleService scheduleService = mock(GenerationScheduleService.class);
        DailyTrendInsightService trendService = mock(DailyTrendInsightService.class);
        DailyKnowledgeGenerationService knowledgeService = mock(DailyKnowledgeGenerationService.class);
        ScheduledGenerationExecutor executor = new ScheduledGenerationExecutor(
                scheduleService,
                trendService,
                knowledgeService
        );
        when(scheduleService.isExecutionDue(any(LocalDateTime.class))).thenReturn(true);
        when(trendService.generateScheduledDailyTrend(any(LocalDate.class)))
                .thenThrow(new IllegalStateException("트렌드 실패"));
        when(knowledgeService.executeScheduledGeneration(any(LocalDate.class)))
                .thenReturn(createExecutionResult(false));

        executor.executeScheduledGeneration();

        verify(knowledgeService).executeScheduledGeneration(any(LocalDate.class));
        verify(scheduleService, never()).markExecuted(any(LocalDateTime.class));
    }

    /**
     * @date 2026-08-13
     * @desc 지정 성공 상태를 가진 예약 지식 생성 결과를 생성합니다.
     */
    private GenerationExecutionResult createExecutionResult(boolean success) {
        return GenerationExecutionResult.builder()
                .success(success)
                .errorCode(success ? null : "failed")
                .message(success ? "성공" : "실패")
                .build();
    }
}
