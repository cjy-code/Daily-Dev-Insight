package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedWeeklyInsightResult;
import com.dailydevinsight.dto.WeeklyAiInsightViewDTO;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.entity.WeeklyAiInsight;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.WeeklyAiInsightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyAiInsightServiceTest {

    /**
     * @date 2026-05-08
     * @desc 최근 7일 테크 뉴스 기반 주간 AI 인사이트를 새로 생성하는지 검증합니다.
     */
    @Test
    void generateWeeklyInsight_ShouldCreateFromRecentTechNews() {
        TechNewsRepository techNewsRepository = mock(TechNewsRepository.class);
        WeeklyAiInsightRepository weeklyAiInsightRepository = mock(WeeklyAiInsightRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);
        WeeklyAiInsightService service = new WeeklyAiInsightService(
                techNewsRepository,
                weeklyAiInsightRepository,
                llmGenerationClient
        );

        LocalDate referenceDate = LocalDate.of(2026, 5, 8);
        LocalDate startDate = LocalDate.of(2026, 5, 2);
        List<TechNews> newsList = List.of(createNews(1L, referenceDate, "AI Tool"), createNews(2L, startDate, "Spring"));
        when(techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(startDate, referenceDate))
                .thenReturn(newsList);
        when(llmGenerationClient.generateWeeklyInsight(any(), eq(startDate), eq(referenceDate)))
                .thenReturn(GeneratedWeeklyInsightResult.builder()
                        .summary("요약")
                        .trendAnalysis("트렌드")
                        .developerView("개발자 관점")
                        .build());
        when(weeklyAiInsightRepository.findByWeekStartDateAndWeekEndDate(startDate, referenceDate))
                .thenReturn(Optional.empty());
        when(weeklyAiInsightRepository.save(any(WeeklyAiInsight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyAiInsightViewDTO result = service.generateWeeklyInsight(referenceDate);

        assertEquals(startDate, result.getWeekStartDate());
        assertEquals(referenceDate, result.getWeekEndDate());
        assertEquals("요약", result.getSummary());
        assertEquals(2, result.getSourceNewsCount());
        assertTrue(result.getVisible());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmGenerationClient).generateWeeklyInsight(promptCaptor.capture(), eq(startDate), eq(referenceDate));
        assertTrue(promptCaptor.getValue().contains("AI Tool"));
    }

    /**
     * @date 2026-05-08
     * @desc 기존 주간 AI 인사이트가 있으면 같은 기간 데이터를 갱신하는지 검증합니다.
     */
    @Test
    void generateWeeklyInsight_ShouldUpdateExistingPeriod() {
        TechNewsRepository techNewsRepository = mock(TechNewsRepository.class);
        WeeklyAiInsightRepository weeklyAiInsightRepository = mock(WeeklyAiInsightRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);
        WeeklyAiInsightService service = new WeeklyAiInsightService(
                techNewsRepository,
                weeklyAiInsightRepository,
                llmGenerationClient
        );

        LocalDate referenceDate = LocalDate.of(2026, 5, 8);
        LocalDate startDate = LocalDate.of(2026, 5, 2);
        WeeklyAiInsight existingInsight = createWeeklyInsight(10L, startDate, referenceDate, true);
        when(techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(startDate, referenceDate))
                .thenReturn(List.of(createNews(1L, referenceDate, "Redis")));
        when(llmGenerationClient.generateWeeklyInsight(any(), eq(startDate), eq(referenceDate)))
                .thenReturn(GeneratedWeeklyInsightResult.builder()
                        .summary("새 요약")
                        .trendAnalysis("새 트렌드")
                        .developerView("새 관점")
                        .build());
        when(weeklyAiInsightRepository.findByWeekStartDateAndWeekEndDate(startDate, referenceDate))
                .thenReturn(Optional.of(existingInsight));
        when(weeklyAiInsightRepository.save(any(WeeklyAiInsight.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyAiInsightViewDTO result = service.generateWeeklyInsight(referenceDate);

        assertEquals(10L, result.getId());
        assertEquals("새 요약", result.getSummary());
        assertEquals("새 트렌드", result.getTrendAnalysis());
        assertEquals("새 관점", result.getDeveloperView());
    }

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트 사용자 노출 여부를 반전하는지 검증합니다.
     */
    @Test
    void toggleVisible_ShouldReverseVisibleState() {
        WeeklyAiInsightRepository weeklyAiInsightRepository = mock(WeeklyAiInsightRepository.class);
        WeeklyAiInsightService service = new WeeklyAiInsightService(
                mock(TechNewsRepository.class),
                weeklyAiInsightRepository,
                mock(LlmGenerationClient.class)
        );
        WeeklyAiInsight weeklyAiInsight = createWeeklyInsight(5L, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 8), true);
        when(weeklyAiInsightRepository.findById(5L)).thenReturn(Optional.of(weeklyAiInsight));

        WeeklyAiInsightViewDTO result = service.toggleVisible(5L);

        assertEquals(false, result.getVisible());
    }

    /**
     * @date 2026-05-08
     * @desc 테스트용 테크 뉴스 엔티티를 생성합니다.
     */
    private TechNews createNews(Long id, LocalDate newsDate, String title) {
        return TechNews.builder()
                .id(id)
                .newsDate(newsDate)
                .source("source")
                .title(title)
                .url("https://example.com/" + id)
                .summary("summary")
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * @date 2026-05-08
     * @desc 테스트용 주간 AI 인사이트 엔티티를 생성합니다.
     */
    private WeeklyAiInsight createWeeklyInsight(Long id, LocalDate startDate, LocalDate endDate, boolean visible) {
        return WeeklyAiInsight.builder()
                .id(id)
                .weekStartDate(startDate)
                .weekEndDate(endDate)
                .summary("기존 요약")
                .trendAnalysis("기존 트렌드")
                .developerView("기존 관점")
                .sourceNewsCount(1)
                .visible(visible)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
