package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlExecutionResult;
import com.dailydevinsight.admin.dto.CrawlRunForm;
import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.entity.TechNews;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechNewsCrawlingServiceTest {

    /**
     * @date 2026-08-10
     * @desc 정상 크롤링에서 수집과 썸네일 처리 후 저장을 위임하고 SUCCESS 이력을 기록하는지 검증합니다.
     */
    @Test
    void executeManualCrawling_ShouldCollectEnrichPersistAndRecordSuccess() {
        TechNewsPersistenceService persistenceService = mock(TechNewsPersistenceService.class);
        CrawlHistoryService crawlHistoryService = mock(CrawlHistoryService.class);
        NewsCrawlerClient newsCrawlerClient = mock(NewsCrawlerClient.class);
        NewsThumbnailStorageService thumbnailStorageService = mock(NewsThumbnailStorageService.class);
        TechNewsCrawlingService service = createService(
                persistenceService,
                crawlHistoryService,
                newsCrawlerClient,
                thumbnailStorageService
        );
        CrawlHistory runningHistory = createRunningHistory();
        NewsArticleData article = createArticle();
        TechNews savedArticle = TechNews.builder().id(11L).url(article.getUrl()).build();
        when(crawlHistoryService.recordRunning(anyString(), any(), anyString(), anyInt())).thenReturn(runningHistory);
        when(newsCrawlerClient.crawlArticles(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(article));
        when(thumbnailStorageService.downloadAndStoreThumbnail(article.getImageUrl(), LocalDate.of(2026, 8, 10)))
                .thenReturn("/images/thumbnail.jpg");
        when(persistenceService.persistArticles(any(), anyString(), anyList(), anyBoolean()))
                .thenReturn(List.of(savedArticle));

        CrawlExecutionResult result = service.executeManualCrawling(createRunForm());

        assertTrue(result.isSuccess());
        assertNull(result.getErrorCode());
        assertEquals(1, result.getCollectedCount());
        assertEquals(1, result.getInsertedCount());

        ArgumentCaptor<List<EnrichedArticle>> enrichedArticlesCaptor = ArgumentCaptor.forClass(List.class);
        InOrder executionOrder = inOrder(newsCrawlerClient, thumbnailStorageService, persistenceService, crawlHistoryService);
        executionOrder.verify(crawlHistoryService).recordRunning("MANUAL", LocalDate.of(2026, 8, 10), "테스트 소스", 10);
        executionOrder.verify(newsCrawlerClient).crawlArticles("테스트 소스", "https://example.com/rss", 10, 10, 20, 1);
        executionOrder.verify(thumbnailStorageService).downloadAndStoreThumbnail(article.getImageUrl(), LocalDate.of(2026, 8, 10));
        executionOrder.verify(persistenceService).persistArticles(
                eq(LocalDate.of(2026, 8, 10)),
                eq("테스트 소스"),
                enrichedArticlesCaptor.capture(),
                eq(false)
        );
        executionOrder.verify(crawlHistoryService).recordSuccess(runningHistory, 1, 1);
        assertEquals("/images/thumbnail.jpg", enrichedArticlesCaptor.getValue().get(0).thumbnailPath());
    }

    /**
     * @date 2026-08-10
     * @desc 수집 단계 예외 시 저장과 썸네일 처리를 시도하지 않고 FAILED 이력을 기록하는지 검증합니다.
     */
    @Test
    void executeManualCrawling_ShouldRecordFailureWhenCollectionFails() {
        TechNewsPersistenceService persistenceService = mock(TechNewsPersistenceService.class);
        CrawlHistoryService crawlHistoryService = mock(CrawlHistoryService.class);
        NewsCrawlerClient newsCrawlerClient = mock(NewsCrawlerClient.class);
        NewsThumbnailStorageService thumbnailStorageService = mock(NewsThumbnailStorageService.class);
        TechNewsCrawlingService service = createService(
                persistenceService,
                crawlHistoryService,
                newsCrawlerClient,
                thumbnailStorageService
        );
        CrawlHistory runningHistory = createRunningHistory();
        IllegalStateException collectionException = new IllegalStateException("RSS 수집 실패");
        when(crawlHistoryService.recordRunning(anyString(), any(), anyString(), anyInt())).thenReturn(runningHistory);
        when(newsCrawlerClient.crawlArticles(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(collectionException);

        CrawlExecutionResult result = service.executeManualCrawling(createRunForm());

        assertFalse(result.isSuccess());
        assertEquals("crawl_failed", result.getErrorCode());
        verify(crawlHistoryService).recordFailure(runningHistory, collectionException);
        verify(thumbnailStorageService, never()).downloadAndStoreThumbnail(any(), any());
        verify(persistenceService, never()).persistArticles(any(), anyString(), anyList(), anyBoolean());
    }

    /**
     * @date 2026-08-10
     * @desc 이미 크롤링 중이면 SKIPPED 이력을 기록하고 수집과 저장을 실행하지 않는지 검증합니다.
     */
    @Test
    void executeManualCrawling_ShouldRecordSkippedWhenCrawlingIsInProgress() throws Exception {
        TechNewsPersistenceService persistenceService = mock(TechNewsPersistenceService.class);
        CrawlHistoryService crawlHistoryService = mock(CrawlHistoryService.class);
        NewsCrawlerClient newsCrawlerClient = mock(NewsCrawlerClient.class);
        NewsThumbnailStorageService thumbnailStorageService = mock(NewsThumbnailStorageService.class);
        TechNewsCrawlingService service = createService(
                persistenceService,
                crawlHistoryService,
                newsCrawlerClient,
                thumbnailStorageService
        );
        setCrawlingInProgress(service, true);

        CrawlExecutionResult result = service.executeManualCrawling(createRunForm());

        assertFalse(result.isSuccess());
        assertEquals("crawl_in_progress", result.getErrorCode());
        verify(crawlHistoryService).recordSkipped(
                "MANUAL",
                LocalDate.of(2026, 8, 10),
                "테스트 소스",
                10,
                "이미 다른 크롤링 작업이 실행 중입니다."
        );
        verify(crawlHistoryService, never()).recordRunning(anyString(), any(), anyString(), anyInt());
        verify(newsCrawlerClient, never()).crawlArticles(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(persistenceService, never()).persistArticles(any(), anyString(), anyList(), anyBoolean());
    }

    /**
     * @date 2026-08-10
     * @desc 수동 및 예약 실행 진입 메서드에 트랜잭션 애노테이션이 없는지 검증합니다.
     */
    @Test
    void crawlingEntryMethods_ShouldNotDeclareTransactional() throws Exception {
        Method manualMethod = TechNewsCrawlingService.class.getMethod("executeManualCrawling", CrawlRunForm.class);
        Method scheduledMethod = TechNewsCrawlingService.class.getMethod(
                "executeScheduledCrawling",
                LocalDate.class,
                com.dailydevinsight.admin.entity.CrawlSchedule.class
        );

        assertNull(manualMethod.getAnnotation(Transactional.class));
        assertNull(scheduledMethod.getAnnotation(Transactional.class));
    }

    /**
     * @date 2026-08-10
     * @desc 크롤링 서비스 테스트용 의존성을 조합합니다.
     */
    private TechNewsCrawlingService createService(
            TechNewsPersistenceService persistenceService,
            CrawlHistoryService crawlHistoryService,
            NewsCrawlerClient newsCrawlerClient,
            NewsThumbnailStorageService thumbnailStorageService
    ) {
        return new TechNewsCrawlingService(
                persistenceService,
                crawlHistoryService,
                newsCrawlerClient,
                thumbnailStorageService
        );
    }

    /**
     * @date 2026-08-10
     * @desc 크롤링 테스트용 관리자 실행 폼을 생성합니다.
     */
    private CrawlRunForm createRunForm() {
        CrawlRunForm form = new CrawlRunForm();
        form.setTargetDate(LocalDate.of(2026, 8, 10));
        form.setSourceName("테스트 소스");
        form.setSourceUrl("https://example.com/rss");
        form.setMaxArticles(10);
        form.setKeywordMatchType("OR");
        form.setIncludeKeywords(List.of());
        form.setIncludeKeywordOperators(List.of());
        form.setExcludeKeywords(List.of());
        form.setTargetDomains(List.of());
        form.setConnectTimeoutSeconds(10);
        form.setReadTimeoutSeconds(20);
        form.setRetryCount(1);
        return form;
    }

    /**
     * @date 2026-08-10
     * @desc 크롤링 테스트용 기사 데이터를 생성합니다.
     */
    private NewsArticleData createArticle() {
        return NewsArticleData.builder()
                .sourceName("테스트 소스")
                .title("신규 기사")
                .url("https://example.com/news")
                .summary("테스트 요약")
                .content("테스트 본문")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }

    /**
     * @date 2026-08-10
     * @desc 크롤링 테스트용 RUNNING 이력을 생성합니다.
     */
    private CrawlHistory createRunningHistory() {
        return CrawlHistory.builder()
                .id(1L)
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

    /**
     * @date 2026-08-10
     * @desc 동시 실행 방지 상태를 테스트 목적에 맞게 설정합니다.
     */
    private void setCrawlingInProgress(TechNewsCrawlingService service, boolean inProgress) throws Exception {
        Field field = TechNewsCrawlingService.class.getDeclaredField("crawlingInProgress");
        field.setAccessible(true);
        AtomicBoolean crawlingInProgress = (AtomicBoolean) field.get(service);
        crawlingInProgress.set(inProgress);
    }
}
