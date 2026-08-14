package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlExecutionResult;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledCrawlingExecutorTest {

    /**
     * @date 2026-08-10
     * @desc 예약 크롤링 성공 시 실행 시각을 기록하는 기존 동작을 검증합니다.
     */
    @Test
    void executeScheduledCrawling_ShouldMarkExecutedWhenSuccessful() {
        CrawlScheduleService crawlScheduleService = mock(CrawlScheduleService.class);
        TechNewsCrawlingService techNewsCrawlingService = mock(TechNewsCrawlingService.class);
        ScheduledCrawlingExecutor executor = new ScheduledCrawlingExecutor(
                crawlScheduleService,
                techNewsCrawlingService
        );
        CrawlSchedule schedule = createSchedule();
        when(crawlScheduleService.isExecutionDue(any(LocalDateTime.class))).thenReturn(true);
        when(crawlScheduleService.getOrCreateSchedule()).thenReturn(schedule);
        when(techNewsCrawlingService.executeScheduledCrawling(any(LocalDate.class), eq(schedule)))
                .thenReturn(createResult(true, null));

        executor.executeScheduledCrawling();

        verify(crawlScheduleService).markExecuted(any(LocalDateTime.class));
    }

    /**
     * @date 2026-08-10
     * @desc 예약 크롤링 실패 또는 스킵 결과에도 실행 시각을 기록하는지 검증합니다.
     */
    @Test
    void executeScheduledCrawling_ShouldMarkExecutedWhenFailedOrSkipped() {
        CrawlScheduleService crawlScheduleService = mock(CrawlScheduleService.class);
        TechNewsCrawlingService techNewsCrawlingService = mock(TechNewsCrawlingService.class);
        ScheduledCrawlingExecutor executor = new ScheduledCrawlingExecutor(
                crawlScheduleService,
                techNewsCrawlingService
        );
        CrawlSchedule schedule = createSchedule();
        when(crawlScheduleService.isExecutionDue(any(LocalDateTime.class))).thenReturn(true);
        when(crawlScheduleService.getOrCreateSchedule()).thenReturn(schedule);
        when(techNewsCrawlingService.executeScheduledCrawling(any(LocalDate.class), eq(schedule)))
                .thenReturn(createResult(false, "crawl_in_progress"));

        executor.executeScheduledCrawling();

        verify(crawlScheduleService).markExecuted(any(LocalDateTime.class));
    }

    /**
     * @date 2026-08-10
     * @desc 예약 실행 테스트용 스케줄 엔티티를 생성합니다.
     */
    private CrawlSchedule createSchedule() {
        return CrawlSchedule.builder()
                .id(1L)
                .enabled(true)
                .allowDuplicate(false)
                .cronExpression("0 0 8 * * *")
                .sourceName("테스트 소스")
                .sourceUrl("https://example.com/rss")
                .maxArticles(10)
                .keywordMatchType("OR")
                .connectTimeoutSeconds(10)
                .readTimeoutSeconds(10)
                .retryCount(1)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 8, 0))
                .build();
    }

    /**
     * @date 2026-08-10
     * @desc 예약 실행 테스트용 크롤링 결과를 생성합니다.
     */
    private CrawlExecutionResult createResult(boolean success, String errorCode) {
        return CrawlExecutionResult.builder()
                .success(success)
                .errorCode(errorCode)
                .message("테스트 결과")
                .collectedCount(0)
                .insertedCount(0)
                .build();
    }
}
