package com.dailydevinsight.service;

import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InsightCacheInvalidationRiskTest.TestConfig.class)
class InsightCacheInvalidationRiskTest {

    @Autowired
    private DailyInsightService dailyInsightService;

    @Autowired
    private TestConfig.FakeDailyKnowledgeService fakeDailyKnowledgeService;

    @Autowired
    private TestConfig.FakeTechNewsService fakeTechNewsService;

    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private DailyKnowledgeRepository dailyKnowledgeRepository;

    /**
     * @date 2026-04-22
     * @desc 동일 날짜 재조회 시 캐시된 응답이 우선되어 데이터 변경이 즉시 반영되지 않는지 검증합니다.
     */
    @Test
    void getInsightsByDate_ShouldReturnCachedResponseAfterSourceDataChanges() {
        fakeDailyKnowledgeService.resetState();
        fakeTechNewsService.resetState();
        LocalDate targetDate = LocalDate.of(2026, 4, 20);
        DailyKnowledge firstKnowledge = createKnowledge(1L, targetDate, "first-title");
        DailyKnowledge changedKnowledge = createKnowledge(1L, targetDate, "changed-title");

        fakeDailyKnowledgeService.setTodayKnowledge(firstKnowledge);
        fakeDailyKnowledgeService.setTop10KnowledgeList(Collections.emptyList());
        fakeDailyKnowledgeService.setTop5KnowledgeList(Collections.emptyList());
        fakeTechNewsService.setNewsList(Collections.emptyList());

        DailyInsightResponseDTO firstResponse = dailyInsightService.getInsightsByDate(targetDate);
        fakeDailyKnowledgeService.setTodayKnowledge(changedKnowledge);
        DailyInsightResponseDTO secondResponse = dailyInsightService.getInsightsByDate(targetDate);

        assertEquals("first-title", firstResponse.getTodayKnowledge().getTitle());
        assertEquals("first-title", secondResponse.getTodayKnowledge().getTitle());
        assertEquals(1, fakeDailyKnowledgeService.getFindTodayKnowledgeCallCount());
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 게시물 수정 이후에도 인사이트 캐시가 비워지지 않아 사용자 조회 결과가 즉시 갱신되지 않는지 검증합니다.
     */
    @Test
    void adminUpdateKnowledgePost_ShouldInvalidateInsightCache() {
        fakeDailyKnowledgeService.resetState();
        fakeTechNewsService.resetState();
        LocalDate targetDate = LocalDate.of(2026, 4, 22);
        DailyKnowledge originalKnowledge = createKnowledge(10L, targetDate, "old-title");
        DailyKnowledge updatedKnowledge = createKnowledge(10L, targetDate, "new-title");

        fakeDailyKnowledgeService.setTodayKnowledge(originalKnowledge);
        fakeDailyKnowledgeService.setTop10KnowledgeList(List.of(originalKnowledge));
        fakeDailyKnowledgeService.setTop5KnowledgeList(List.of(originalKnowledge));
        fakeTechNewsService.setNewsList(Collections.emptyList());
        when(dailyKnowledgeRepository.findById(10L)).thenReturn(Optional.of(originalKnowledge));
        when(dailyKnowledgeRepository.save(any(DailyKnowledge.class))).thenReturn(updatedKnowledge);

        DailyInsightResponseDTO cachedResponse = dailyInsightService.getInsightsByDate(targetDate);
        adminManagementService.updateKnowledgePost(10L, "Backend", "new-title");
        fakeDailyKnowledgeService.setTodayKnowledge(updatedKnowledge);
        DailyInsightResponseDTO responseAfterAdminUpdate = dailyInsightService.getInsightsByDate(targetDate);

        assertEquals("old-title", cachedResponse.getTodayKnowledge().getTitle());
        assertEquals("new-title", responseAfterAdminUpdate.getTodayKnowledge().getTitle());
        assertEquals(2, fakeDailyKnowledgeService.getFindTodayKnowledgeCallCount());
        verify(dailyKnowledgeRepository, times(1)).save(any(DailyKnowledge.class));
    }

    /**
     * @date 2026-04-22
     * @desc 테스트용 DailyKnowledge 엔티티를 생성합니다.
     */
    private DailyKnowledge createKnowledge(Long id, LocalDate knowledgeDate, String title) {
        return DailyKnowledge.builder()
                .id(id)
                .knowledgeDate(knowledgeDate)
                .category("Backend")
                .title(title)
                .summary("summary")
                .detail("detail")
                .attachmentImagePath(null)
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestConfig {

        /**
         * @date 2026-04-22
         * @desc 테스트에서 사용하는 메모리 캐시 매니저를 구성합니다.
         */
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    RedisCacheConfig.CACHE_INSIGHTS_BY_DATE,
                    RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE,
                    RedisCacheConfig.CACHE_WEEKLY_TOP10,
                    RedisCacheConfig.CACHE_WEEKLY_TOP5
            );
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 DailyInsightService 빈을 생성합니다.
         */
        @Bean
        DailyInsightService dailyInsightService(DailyKnowledgeService dailyKnowledgeService, TechNewsService techNewsService) {
            return new DailyInsightService(dailyKnowledgeService, techNewsService);
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 AdminManagementService 빈을 생성합니다.
         */
        @Bean
        AdminManagementService adminManagementService(
                DailyKnowledgeRepository dailyKnowledgeRepository,
                TechNewsRepository techNewsRepository,
                UserRepository userRepository,
                GenerationHistoryRepository generationHistoryRepository,
                InsightBookmarkRepository insightBookmarkRepository
        ) {
            return new AdminManagementService(
                    dailyKnowledgeRepository,
                    techNewsRepository,
                    userRepository,
                    generationHistoryRepository,
                    insightBookmarkRepository
            );
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 DailyKnowledgeService 목 객체를 제공합니다.
         */
        @Bean
        FakeDailyKnowledgeService fakeDailyKnowledgeService() {
            return new FakeDailyKnowledgeService();
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 DailyKnowledgeService 빈으로 Fake 구현체를 제공합니다.
         */
        @Bean
        DailyKnowledgeService dailyKnowledgeService(FakeDailyKnowledgeService fakeDailyKnowledgeService) {
            return fakeDailyKnowledgeService;
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 TechNewsService 목 객체를 제공합니다.
         */
        @Bean
        FakeTechNewsService fakeTechNewsService() {
            return new FakeTechNewsService();
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 TechNewsService 빈으로 Fake 구현체를 제공합니다.
         */
        @Bean
        TechNewsService techNewsService(FakeTechNewsService fakeTechNewsService) {
            return fakeTechNewsService;
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 DailyKnowledgeRepository 목 객체를 제공합니다.
         */
        @Bean
        DailyKnowledgeRepository dailyKnowledgeRepository() {
            return Mockito.mock(DailyKnowledgeRepository.class);
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 TechNewsRepository 목 객체를 제공합니다.
         */
        @Bean
        TechNewsRepository techNewsRepository() {
            return Mockito.mock(TechNewsRepository.class);
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 UserRepository 목 객체를 제공합니다.
         */
        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 GenerationHistoryRepository 목 객체를 제공합니다.
         */
        @Bean
        GenerationHistoryRepository generationHistoryRepository() {
            return Mockito.mock(GenerationHistoryRepository.class);
        }

        /**
         * @date 2026-04-22
         * @desc 테스트용 InsightBookmarkRepository 목 객체를 제공합니다.
         */
        @Bean
        InsightBookmarkRepository insightBookmarkRepository() {
            return Mockito.mock(InsightBookmarkRepository.class);
        }

        /**
         * @date 2026-04-22
         * @desc DailyKnowledgeService 호출값과 호출 횟수를 제어하기 위한 테스트 전용 스텁입니다.
         */
        static class FakeDailyKnowledgeService extends DailyKnowledgeService {

            private final AtomicReference<DailyKnowledge> todayKnowledgeReference = new AtomicReference<>();
            private final AtomicReference<List<DailyKnowledge>> top10KnowledgeListReference = new AtomicReference<>(Collections.emptyList());
            private final AtomicReference<List<DailyKnowledge>> top5KnowledgeListReference = new AtomicReference<>(Collections.emptyList());
            private final AtomicInteger findTodayKnowledgeCallCount = new AtomicInteger(0);

            FakeDailyKnowledgeService() {
                super(Mockito.mock(DailyKnowledgeRepository.class));
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 조회 시 반환할 오늘의 지식 데이터를 설정합니다.
             */
            void setTodayKnowledge(DailyKnowledge knowledge) {
                todayKnowledgeReference.set(knowledge);
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 조회 시 반환할 TOP10 목록 데이터를 설정합니다.
             */
            void setTop10KnowledgeList(List<DailyKnowledge> knowledgeList) {
                top10KnowledgeListReference.set(knowledgeList);
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 조회 시 반환할 TOP5 목록 데이터를 설정합니다.
             */
            void setTop5KnowledgeList(List<DailyKnowledge> knowledgeList) {
                top5KnowledgeListReference.set(knowledgeList);
            }

            /**
             * @date 2026-04-22
             * @desc 오늘의 지식 조회 호출 횟수를 반환합니다.
             */
            int getFindTodayKnowledgeCallCount() {
                return findTodayKnowledgeCallCount.get();
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 간 간섭을 방지하기 위해 스텁 내부 상태를 초기화합니다.
             */
            void resetState() {
                todayKnowledgeReference.set(null);
                top10KnowledgeListReference.set(Collections.emptyList());
                top5KnowledgeListReference.set(Collections.emptyList());
                findTodayKnowledgeCallCount.set(0);
            }

            /**
             * @date 2026-04-22
             * @desc 설정된 오늘의 지식 데이터를 반환합니다.
             */
            @Override
            public Optional<DailyKnowledge> findTodayKnowledge(LocalDate targetDate) {
                findTodayKnowledgeCallCount.incrementAndGet();
                return Optional.ofNullable(todayKnowledgeReference.get());
            }

            /**
             * @date 2026-04-22
             * @desc 설정된 TOP10 목록을 반환합니다.
             */
            @Override
            public List<DailyKnowledge> findWeeklyHotKnowledgeTop10(LocalDate referenceDate) {
                return top10KnowledgeListReference.get();
            }

            /**
             * @date 2026-04-22
             * @desc 설정된 TOP5 목록을 반환합니다.
             */
            @Override
            public List<DailyKnowledge> findWeeklyHotKnowledgeTop5(LocalDate referenceDate) {
                return top5KnowledgeListReference.get();
            }
        }

        /**
         * @date 2026-04-22
         * @desc TechNewsService 반환값을 제어하기 위한 테스트 전용 스텁입니다.
         */
        static class FakeTechNewsService extends TechNewsService {

            private final AtomicReference<List<TechNews>> newsListReference = new AtomicReference<>(Collections.emptyList());

            FakeTechNewsService() {
                super(Mockito.mock(TechNewsRepository.class));
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 조회 시 반환할 뉴스 목록 데이터를 설정합니다.
             */
            void setNewsList(List<TechNews> newsList) {
                newsListReference.set(newsList);
            }

            /**
             * @date 2026-04-22
             * @desc 테스트 간 간섭을 방지하기 위해 스텁 내부 상태를 초기화합니다.
             */
            void resetState() {
                newsListReference.set(Collections.emptyList());
            }

            /**
             * @date 2026-04-22
             * @desc 설정된 뉴스 목록 데이터를 반환합니다.
             */
            @Override
            public List<TechNews> findNewsByDate(LocalDate targetDate) {
                return newsListReference.get();
            }
        }
    }
}
