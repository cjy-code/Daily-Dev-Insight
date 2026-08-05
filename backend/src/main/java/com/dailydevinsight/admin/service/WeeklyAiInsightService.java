package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedWeeklyInsightResult;
import com.dailydevinsight.dto.WeeklyAiInsightViewDTO;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.entity.WeeklyAiInsight;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.WeeklyAiInsightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyAiInsightService {

    private static final int WEEKLY_ANALYSIS_DAYS = 7;
    private static final int MAX_PROMPT_NEWS_COUNT = 40;
    private static final int MAX_SUMMARY_LENGTH = 350;

    private final TechNewsRepository techNewsRepository;
    private final WeeklyAiInsightRepository weeklyAiInsightRepository;
    private final LlmGenerationClient llmGenerationClient;

    /**
     * @date 2026-05-08
     * @desc 기준일을 포함한 최근 7일 뉴스 기반 주간 AI 인사이트를 생성하거나 갱신합니다.
     */
    @Transactional
    public WeeklyAiInsightViewDTO generateWeeklyInsight(LocalDate referenceDate) {
        LocalDate weekEndDate = referenceDate == null ? LocalDate.now() : referenceDate;
        LocalDate weekStartDate = weekEndDate.minusDays(WEEKLY_ANALYSIS_DAYS - 1L);
        List<TechNews> sourceNewsList = techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(
                weekStartDate,
                weekEndDate
        );

        if (sourceNewsList.isEmpty()) {
            throw new IllegalStateException("최근 7일 기준으로 분석할 테크 뉴스가 없습니다.");
        }

        String prompt = buildWeeklyInsightPrompt(sourceNewsList);
        GeneratedWeeklyInsightResult generatedResult = llmGenerationClient.generateWeeklyInsight(
                prompt,
                weekStartDate,
                weekEndDate
        );
        WeeklyAiInsight weeklyAiInsight = weeklyAiInsightRepository
                .findByWeekStartDateAndWeekEndDate(weekStartDate, weekEndDate)
                .orElseGet(() -> createWeeklyAiInsight(weekStartDate, weekEndDate, sourceNewsList.size()));

        weeklyAiInsight.updateAnalysis(
                generatedResult.getSummary(),
                generatedResult.getTrendAnalysis(),
                generatedResult.getDeveloperView(),
                sourceNewsList.size()
        );

        return WeeklyAiInsightViewDTO.from(weeklyAiInsightRepository.save(weeklyAiInsight));
    }

    /**
     * @date 2026-05-08
     * @desc 관리자 화면에 표시할 최신 주간 AI 인사이트를 조회합니다.
     */
    @Transactional(readOnly = true)
    public WeeklyAiInsightViewDTO findLatestInsightForAdmin() {
        return weeklyAiInsightRepository.findTopByOrderByWeekEndDateDescIdDesc()
                .map(WeeklyAiInsightViewDTO::from)
                .orElse(null);
    }

    /**
     * @date 2026-05-08
     * @desc 사용자 화면에 표시할 최신 공개 주간 AI 인사이트를 조회합니다.
     */
    @Transactional(readOnly = true)
    public WeeklyAiInsightViewDTO findLatestVisibleInsight() {
        return weeklyAiInsightRepository.findTopByVisibleTrueOrderByWeekEndDateDescIdDesc()
                .map(WeeklyAiInsightViewDTO::from)
                .orElse(null);
    }

    /**
     * @date 2026-05-08
     * @desc 관리자 화면에 표시할 최근 주간 AI 인사이트 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<WeeklyAiInsightViewDTO> findRecentInsightsForAdmin() {
        return weeklyAiInsightRepository.findTop5ByOrderByWeekEndDateDescIdDesc()
                .stream()
                .map(WeeklyAiInsightViewDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트의 사용자 노출 여부를 반전합니다.
     */
    @Transactional
    public WeeklyAiInsightViewDTO toggleVisible(Long insightId) {
        WeeklyAiInsight weeklyAiInsight = weeklyAiInsightRepository.findById(insightId)
                .orElseThrow(() -> new IllegalArgumentException("주간 AI 인사이트를 찾을 수 없습니다."));
        weeklyAiInsight.changeVisible(!Boolean.TRUE.equals(weeklyAiInsight.getVisible()));
        return WeeklyAiInsightViewDTO.from(weeklyAiInsight);
    }

    /**
     * @date 2026-05-08
     * @desc 기준일에 해당하는 최근 7일 분석 시작일을 계산합니다.
     */
    public LocalDate calculateWeekStartDate(LocalDate referenceDate) {
        LocalDate resolvedReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        return resolvedReferenceDate.minusDays(WEEKLY_ANALYSIS_DAYS - 1L);
    }

    /**
     * @date 2026-05-08
     * @desc 저장 전 기본값을 포함한 주간 AI 인사이트 엔티티를 생성합니다.
     */
    private WeeklyAiInsight createWeeklyAiInsight(LocalDate weekStartDate, LocalDate weekEndDate, int sourceNewsCount) {
        LocalDateTime now = LocalDateTime.now();
        return WeeklyAiInsight.builder()
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .summary("")
                .trendAnalysis("")
                .developerView("")
                .sourceNewsCount(sourceNewsCount)
                .visible(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * @date 2026-05-08
     * @desc LLM에 전달할 주간 뉴스 목록 프롬프트를 구성합니다.
     */
    private String buildWeeklyInsightPrompt(List<TechNews> sourceNewsList) {
        return sourceNewsList.stream()
                .limit(MAX_PROMPT_NEWS_COUNT)
                .map(this::formatNewsForPrompt)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * @date 2026-05-08
     * @desc 단일 뉴스 항목을 LLM 입력에 적합한 텍스트로 변환합니다.
     */
    private String formatNewsForPrompt(TechNews techNews) {
        return "- 날짜: " + techNews.getNewsDate() + "\n"
                + "  출처: " + techNews.getSource() + "\n"
                + "  제목: " + techNews.getTitle() + "\n"
                + "  요약: " + truncate(techNews.getSummary(), MAX_SUMMARY_LENGTH) + "\n"
                + "  URL: " + techNews.getUrl();
    }

    /**
     * @date 2026-05-08
     * @desc LLM 프롬프트가 과도하게 길어지지 않도록 문자열 길이를 제한합니다.
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return Optional.ofNullable(value).orElse("");
        }
        return value.substring(0, maxLength) + "...";
    }
}
