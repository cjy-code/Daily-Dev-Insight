package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.admin.repository.CrawlHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlHistoryServiceTest {

    /**
     * @date 2026-08-10
     * @desc RUNNING과 SKIPPED 이력이 기존 필드값 그대로 저장되는지 검증합니다.
     */
    @Test
    void recordRunningAndSkipped_ShouldSaveExpectedHistories() {
        CrawlHistoryRepository crawlHistoryRepository = mock(CrawlHistoryRepository.class);
        CrawlHistoryService service = new CrawlHistoryService(crawlHistoryRepository);
        when(crawlHistoryRepository.save(any(CrawlHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate targetDate = LocalDate.of(2026, 8, 10);

        CrawlHistory runningHistory = service.recordRunning("MANUAL", targetDate, "테스트 소스", 10);
        service.recordSkipped("SCHEDULED", targetDate, "예약 소스", 20, "이미 실행 중");

        assertEquals("RUNNING", runningHistory.getStatus());
        assertEquals("MANUAL", runningHistory.getTriggerType());
        assertEquals(10, runningHistory.getRequestedCount());
        assertEquals(0, runningHistory.getCollectedCount());
        assertEquals(0, runningHistory.getInsertedCount());
        assertNull(runningHistory.getErrorMessage());

        ArgumentCaptor<CrawlHistory> historyCaptor = ArgumentCaptor.forClass(CrawlHistory.class);
        verify(crawlHistoryRepository, times(2)).save(historyCaptor.capture());
        CrawlHistory skippedHistory = historyCaptor.getAllValues().get(1);
        assertEquals("SKIPPED", skippedHistory.getStatus());
        assertEquals("SCHEDULED", skippedHistory.getTriggerType());
        assertEquals("이미 실행 중", skippedHistory.getErrorMessage());
    }

    /**
     * @date 2026-08-10
     * @desc RUNNING 이력을 SUCCESS 상태와 수집·저장 건수로 갱신하는지 검증합니다.
     */
    @Test
    void recordSuccess_ShouldSaveSuccessHistory() {
        CrawlHistoryRepository crawlHistoryRepository = mock(CrawlHistoryRepository.class);
        CrawlHistoryService service = new CrawlHistoryService(crawlHistoryRepository);
        CrawlHistory runningHistory = createRunningHistory();

        service.recordSuccess(runningHistory, 7, 5);

        ArgumentCaptor<CrawlHistory> historyCaptor = ArgumentCaptor.forClass(CrawlHistory.class);
        verify(crawlHistoryRepository).save(historyCaptor.capture());
        CrawlHistory successHistory = historyCaptor.getValue();
        assertEquals(runningHistory.getId(), successHistory.getId());
        assertEquals("SUCCESS", successHistory.getStatus());
        assertEquals(7, successHistory.getCollectedCount());
        assertEquals(5, successHistory.getInsertedCount());
        assertNull(successHistory.getErrorMessage());
    }

    /**
     * @date 2026-08-10
     * @desc RUNNING 이력을 FAILED 상태와 예외 메시지로 갱신하는지 검증합니다.
     */
    @Test
    void recordFailure_ShouldSaveFailureHistory() {
        CrawlHistoryRepository crawlHistoryRepository = mock(CrawlHistoryRepository.class);
        CrawlHistoryService service = new CrawlHistoryService(crawlHistoryRepository);
        CrawlHistory runningHistory = createRunningHistory();

        service.recordFailure(runningHistory, new IllegalStateException("수집 실패"));

        ArgumentCaptor<CrawlHistory> historyCaptor = ArgumentCaptor.forClass(CrawlHistory.class);
        verify(crawlHistoryRepository).save(historyCaptor.capture());
        CrawlHistory failureHistory = historyCaptor.getValue();
        assertEquals(runningHistory.getId(), failureHistory.getId());
        assertEquals("FAILED", failureHistory.getStatus());
        assertEquals(0, failureHistory.getCollectedCount());
        assertEquals(0, failureHistory.getInsertedCount());
        assertEquals("수집 실패", failureHistory.getErrorMessage());
    }

    /**
     * @date 2026-08-10
     * @desc 이력 갱신 테스트용 RUNNING 엔티티를 생성합니다.
     */
    private CrawlHistory createRunningHistory() {
        return CrawlHistory.builder()
                .id(10L)
                .triggerType("MANUAL")
                .targetDate(LocalDate.of(2026, 8, 10))
                .status("RUNNING")
                .sourceName("테스트 소스")
                .requestedCount(10)
                .collectedCount(0)
                .insertedCount(0)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .build();
    }
}
