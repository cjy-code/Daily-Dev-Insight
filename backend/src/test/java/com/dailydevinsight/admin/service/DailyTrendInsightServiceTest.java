package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedDailyTrendResult;
import com.dailydevinsight.admin.entity.DailyTrendGenerationHistory;
import com.dailydevinsight.admin.repository.DailyTrendGenerationHistoryRepository;
import com.dailydevinsight.dto.DailyTrendInsightViewDTO;
import com.dailydevinsight.entity.DailyTrendInsight;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.DailyTrendInsightRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyTrendInsightServiceTest {

    /**
     * @date 2026-08-13
     * @desc 당일 뉴스가 3건 이상이면 당일 뉴스만 사용해 트렌드를 생성하는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldUseOnlyTargetDateNewsWhenAtLeastThreeExist() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        List<TechNews> newsList = createNewsList(targetDate, 3);
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate)).thenReturn(newsList);
        stubSuccessfulGeneration(fixture, targetDate, List.of("Java", "AI", "Cloud"));

        DailyTrendInsightViewDTO result = fixture.service.generateDailyTrend(targetDate);

        assertEquals(targetDate, result.getTrendDate());
        assertEquals(3, result.getSourceNewsCount());
        verify(fixture.techNewsRepository, never())
                .findByNewsDateBetweenOrderByNewsDateDescIdDesc(any(), any());
    }

    /**
     * @date 2026-08-13
     * @desc 당일 뉴스가 2건 이하이면 기준일 포함 최근 3일 뉴스로 폴백하는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldFallbackToThreeDaysWhenTargetDateNewsIsInsufficient() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        LocalDate startDate = targetDate.minusDays(2);
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate))
                .thenReturn(createNewsList(targetDate, 2));
        when(fixture.techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(startDate, targetDate))
                .thenReturn(createNewsList(targetDate, 5));
        stubSuccessfulGeneration(fixture, targetDate, List.of("Java", "AI", "Cloud"));

        DailyTrendInsightViewDTO result = fixture.service.generateDailyTrend(targetDate);

        assertEquals(5, result.getSourceNewsCount());
        verify(fixture.techNewsRepository)
                .findByNewsDateBetweenOrderByNewsDateDescIdDesc(startDate, targetDate);
    }

    /**
     * @date 2026-08-13
     * @desc 최근 3일에도 뉴스가 없으면 생성 실패와 실패 이력을 함께 기록하는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldSaveFailureHistoryWhenFallbackHasNoNews() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate)).thenReturn(List.of());
        when(fixture.techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(targetDate.minusDays(2), targetDate))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> fixture.service.generateDailyTrend(targetDate));

        ArgumentCaptor<DailyTrendGenerationHistory> historyCaptor =
                ArgumentCaptor.forClass(DailyTrendGenerationHistory.class);
        verify(fixture.historyRepository).save(historyCaptor.capture());
        assertEquals("FAILED", historyCaptor.getValue().getStatus());
        assertEquals(targetDate, historyCaptor.getValue().getTargetDate());
        assertTrue(historyCaptor.getValue().getErrorMessage().contains("테크 뉴스"));
    }

    /**
     * @date 2026-08-13
     * @desc 같은 기준일 재생성 시 기존 트렌드 행을 갱신하고 신규 엔티티를 만들지 않는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldUpdateExistingTrendDate() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        DailyTrendInsight existingTrend = createTrend(11L, targetDate, "Old,Trend,Data");
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate))
                .thenReturn(createNewsList(targetDate, 3));
        when(fixture.llmGenerationClient.generateDailyTrend(any(), eq(targetDate)))
                .thenReturn(createGeneratedResult(List.of("Java", "AI", "Cloud")));
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate))
                .thenReturn(Optional.of(existingTrend));
        when(fixture.dailyTrendInsightRepository.save(any(DailyTrendInsight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyTrendInsightViewDTO result = fixture.service.generateDailyTrend(targetDate);

        assertEquals(11L, result.getId());
        assertEquals(List.of("Java", "AI", "Cloud"), result.getKeywords());
    }

    /**
     * @date 2026-08-13
     * @desc LLM 키워드가 5개를 초과하면 앞의 5개만 저장하는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldKeepFirstFiveKeywords() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate))
                .thenReturn(createNewsList(targetDate, 3));
        stubSuccessfulGeneration(
                fixture,
                targetDate,
                List.of("Java", "AI", "Cloud", "Spring", "Oracle", "Redis")
        );

        DailyTrendInsightViewDTO result = fixture.service.generateDailyTrend(targetDate);

        assertEquals(List.of("Java", "AI", "Cloud", "Spring", "Oracle"), result.getKeywords());
    }

    /**
     * @date 2026-08-13
     * @desc LLM 유효 키워드가 2개 이하이면 생성 실패와 실패 이력을 기록하는지 검증합니다.
     */
    @Test
    void generateDailyTrend_ShouldFailWhenKeywordsAreFewerThanThree() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        when(fixture.techNewsRepository.findByNewsDateOrderByIdDesc(targetDate))
                .thenReturn(createNewsList(targetDate, 3));
        when(fixture.llmGenerationClient.generateDailyTrend(any(), eq(targetDate)))
                .thenReturn(createGeneratedResult(List.of("Java", "AI")));

        assertThrows(LlmClientException.class, () -> fixture.service.generateDailyTrend(targetDate));

        ArgumentCaptor<DailyTrendGenerationHistory> historyCaptor =
                ArgumentCaptor.forClass(DailyTrendGenerationHistory.class);
        verify(fixture.historyRepository).save(historyCaptor.capture());
        assertEquals("FAILED", historyCaptor.getValue().getStatus());
    }

    /**
     * @date 2026-08-13
     * @desc 성공 생성에 필요한 공통 저장소와 LLM 응답을 설정합니다.
     */
    private void stubSuccessfulGeneration(TestFixture fixture, LocalDate targetDate, List<String> keywords) {
        when(fixture.llmGenerationClient.generateDailyTrend(any(), eq(targetDate)))
                .thenReturn(createGeneratedResult(keywords));
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate)).thenReturn(Optional.empty());
        when(fixture.dailyTrendInsightRepository.save(any(DailyTrendInsight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * @date 2026-08-13
     * @desc 테스트에 사용할 트렌드 생성 서비스와 Mock 의존성을 구성합니다.
     */
    private TestFixture createFixture() {
        TechNewsRepository techNewsRepository = mock(TechNewsRepository.class);
        DailyTrendInsightRepository dailyTrendInsightRepository = mock(DailyTrendInsightRepository.class);
        DailyTrendGenerationHistoryRepository historyRepository =
                mock(DailyTrendGenerationHistoryRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);
        DailyTrendInsightService service = new DailyTrendInsightService(
                techNewsRepository,
                dailyTrendInsightRepository,
                historyRepository,
                llmGenerationClient
        );
        return new TestFixture(
                service,
                techNewsRepository,
                dailyTrendInsightRepository,
                historyRepository,
                llmGenerationClient
        );
    }

    /**
     * @date 2026-08-13
     * @desc 지정 기준일과 개수로 테스트용 테크 뉴스 목록을 생성합니다.
     */
    private List<TechNews> createNewsList(LocalDate targetDate, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> TechNews.builder()
                        .id((long) index + 1L)
                        .newsDate(targetDate.minusDays(index % 3))
                        .source("source")
                        .title("news-" + index)
                        .url("https://example.com/" + index)
                        .summary("summary")
                        .viewCount(0L)
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    /**
     * @date 2026-08-13
     * @desc 테스트용 일일 개발 트렌드 엔티티를 생성합니다.
     */
    private DailyTrendInsight createTrend(Long id, LocalDate targetDate, String keywords) {
        return DailyTrendInsight.builder()
                .id(id)
                .trendDate(targetDate)
                .keywords(keywords)
                .summary("기존 요약")
                .sourceNewsCount(3)
                .visible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * @date 2026-08-13
     * @desc 지정 키워드를 포함한 테스트용 LLM 트렌드 결과를 생성합니다.
     */
    private GeneratedDailyTrendResult createGeneratedResult(List<String> keywords) {
        return GeneratedDailyTrendResult.builder()
                .keywords(keywords)
                .summary("오늘의 개발 트렌드 요약")
                .build();
    }

    private record TestFixture(
            DailyTrendInsightService service,
            TechNewsRepository techNewsRepository,
            DailyTrendInsightRepository dailyTrendInsightRepository,
            DailyTrendGenerationHistoryRepository historyRepository,
            LlmGenerationClient llmGenerationClient
    ) {
    }
}
