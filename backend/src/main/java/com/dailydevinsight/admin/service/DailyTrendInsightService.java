package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedDailyTrendResult;
import com.dailydevinsight.admin.entity.DailyTrendGenerationHistory;
import com.dailydevinsight.admin.repository.DailyTrendGenerationHistoryRepository;
import com.dailydevinsight.dto.DailyTrendInsightViewDTO;
import com.dailydevinsight.entity.DailyTrendInsight;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.DailyTrendInsightRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyTrendInsightService {

    private static final int TREND_FALLBACK_DAYS = 3;
    private static final int TREND_FALLBACK_MIN_NEWS_COUNT = 3;
    private static final int MAX_PROMPT_NEWS_COUNT = 40;
    private static final int MAX_SUMMARY_LENGTH = 350;
    private static final int MIN_KEYWORD_COUNT = 3;
    private static final int MAX_KEYWORD_COUNT = 5;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final String TRIGGER_TYPE_MANUAL = "MANUAL";
    private static final String TRIGGER_TYPE_SCHEDULED = "SCHEDULED";

    private final TechNewsRepository techNewsRepository;
    private final DailyTrendInsightRepository dailyTrendInsightRepository;
    private final DailyTrendGenerationHistoryRepository dailyTrendGenerationHistoryRepository;
    private final LlmGenerationClient llmGenerationClient;

    private final Set<LocalDate> inProgressTrendDates = ConcurrentHashMap.newKeySet();

    /**
     * @date 2026-08-13
     * @desc 관리자 요청으로 기준일의 일일 개발 트렌드를 생성하거나 갱신합니다.
     */
    public DailyTrendInsightViewDTO generateDailyTrend(LocalDate referenceDate) {
        return generateDailyTrend(referenceDate, TRIGGER_TYPE_MANUAL);
    }

    /**
     * @date 2026-08-13
     * @desc 예약 실행으로 기준일의 일일 개발 트렌드를 생성하거나 갱신합니다.
     */
    public DailyTrendInsightViewDTO generateScheduledDailyTrend(LocalDate referenceDate) {
        return generateDailyTrend(referenceDate, TRIGGER_TYPE_SCHEDULED);
    }

    /**
     * @date 2026-08-13
     * @desc 기준일에 저장된 일일 개발 트렌드를 관리자 화면용 DTO로 조회합니다.
     */
    @Transactional(readOnly = true)
    public DailyTrendInsightViewDTO findByTrendDate(LocalDate trendDate) {
        if (trendDate == null) {
            return null;
        }
        return dailyTrendInsightRepository.findByTrendDate(trendDate)
                .map(DailyTrendInsightViewDTO::from)
                .orElse(null);
    }

    /**
     * @date 2026-08-13
     * @desc 사용자 화면에 표시할 최신 공개 일일 개발 트렌드를 조회합니다.
     */
    @Transactional(readOnly = true)
    public DailyTrendInsightViewDTO findLatestVisibleTrend() {
        return dailyTrendInsightRepository.findTopByVisibleTrueOrderByTrendDateDescIdDesc()
                .map(DailyTrendInsightViewDTO::from)
                .orElse(null);
    }

    /**
     * @date 2026-08-13
     * @desc 관리자 화면에 표시할 최신 일일 개발 트렌드를 조회합니다.
     */
    @Transactional(readOnly = true)
    public DailyTrendInsightViewDTO findLatestTrendForAdmin() {
        return dailyTrendInsightRepository.findTopByOrderByTrendDateDescIdDesc()
                .map(DailyTrendInsightViewDTO::from)
                .orElse(null);
    }

    /**
     * @date 2026-08-13
     * @desc 관리자 화면에 표시할 최근 일일 개발 트렌드 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<DailyTrendInsightViewDTO> findRecentTrendsForAdmin() {
        return dailyTrendInsightRepository.findTop5ByOrderByTrendDateDescIdDesc()
                .stream()
                .map(DailyTrendInsightViewDTO::from)
                .toList();
    }

    /**
     * @date 2026-08-13
     * @desc 관리자 화면에 표시할 최근 트렌드 생성 시도 이력(성공/실패 포함) 20건을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<DailyTrendGenerationHistory> findRecentGenerationHistoryForAdmin() {
        return dailyTrendGenerationHistoryRepository.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * @date 2026-08-13
     * @desc 일일 개발 트렌드의 사용자 노출 여부를 반전합니다.
     */
    public DailyTrendInsightViewDTO toggleVisible(Long trendId) {
        DailyTrendInsight dailyTrendInsight = dailyTrendInsightRepository.findById(trendId)
                .orElseThrow(() -> new IllegalArgumentException("일일 개발 트렌드를 찾을 수 없습니다."));
        dailyTrendInsight.changeVisible(!Boolean.TRUE.equals(dailyTrendInsight.getVisible()));
        return DailyTrendInsightViewDTO.from(dailyTrendInsightRepository.save(dailyTrendInsight));
    }

    /**
     * @date 2026-08-13
     * @desc 트리거 유형에 맞춰 뉴스 조회, LLM 분석, 결과와 실행 이력 저장을 처리합니다.
     */
    private DailyTrendInsightViewDTO generateDailyTrend(LocalDate referenceDate, String triggerType) {
        LocalDate targetDate = referenceDate == null ? LocalDate.now() : referenceDate;
        if (!inProgressTrendDates.add(targetDate)) {
            throw new IllegalStateException("기준일 " + targetDate + "의 트렌드 생성이 이미 진행 중입니다. 잠시 후 다시 시도해 주세요.");
        }

        try {
            List<TechNews> sourceNewsList = findSourceNews(targetDate);
            try {
                if (sourceNewsList.isEmpty()) {
                    throw new IllegalStateException("분석할 테크 뉴스가 없습니다.");
                }

                GeneratedDailyTrendResult generatedResult = llmGenerationClient.generateDailyTrend(
                        buildDailyTrendPrompt(sourceNewsList),
                        targetDate
                );
                List<String> normalizedKeywords = normalizeKeywords(generatedResult.getKeywords());
                String summary = validateSummary(generatedResult.getSummary());
                DailyTrendInsight dailyTrendInsight = dailyTrendInsightRepository.findByTrendDate(targetDate)
                        .orElseGet(() -> createDailyTrendInsight(targetDate, sourceNewsList.size()));

                dailyTrendInsight.updateAnalysis(String.join(",", normalizedKeywords), summary, sourceNewsList.size());
                DailyTrendInsight savedTrend = dailyTrendInsightRepository.save(dailyTrendInsight);
                saveGenerationHistory(
                        triggerType,
                        targetDate,
                        "SUCCESS",
                        sourceNewsList.size(),
                        savedTrend.getId(),
                        null
                );
                return DailyTrendInsightViewDTO.from(savedTrend);
            } catch (Exception exception) {
                saveGenerationHistory(
                        triggerType,
                        targetDate,
                        "FAILED",
                        sourceNewsList.size(),
                        null,
                        exception.getMessage()
                );
                throw exception;
            }
        } finally {
            inProgressTrendDates.remove(targetDate);
        }
    }

    /**
     * @date 2026-08-13
     * @desc 당일 뉴스가 3건 미만이면 기준일을 포함한 최근 3일 뉴스로 폴백합니다.
     */
    private List<TechNews> findSourceNews(LocalDate targetDate) {
        List<TechNews> sourceNewsList = techNewsRepository.findByNewsDateOrderByIdDesc(targetDate);
        if (sourceNewsList.size() >= TREND_FALLBACK_MIN_NEWS_COUNT) {
            return sourceNewsList;
        }
        return techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(
                targetDate.minusDays(TREND_FALLBACK_DAYS - 1L),
                targetDate
        );
    }

    /**
     * @date 2026-08-13
     * @desc 저장 전 기본값을 포함한 일일 개발 트렌드 엔티티를 생성합니다.
     */
    private DailyTrendInsight createDailyTrendInsight(LocalDate targetDate, int sourceNewsCount) {
        LocalDateTime now = LocalDateTime.now();
        return DailyTrendInsight.builder()
                .trendDate(targetDate)
                .keywords("")
                .summary("")
                .sourceNewsCount(sourceNewsCount)
                .visible(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * @date 2026-08-13
     * @desc LLM에 전달할 일일 뉴스 목록과 응답 형식 예시를 구성합니다.
     */
    private String buildDailyTrendPrompt(List<TechNews> sourceNewsList) {
        String newsPrompt = sourceNewsList.stream()
                .limit(MAX_PROMPT_NEWS_COUNT)
                .map(this::formatNewsForPrompt)
                .collect(Collectors.joining("\n\n"));
        return newsPrompt + "\n\n응답 JSON 예시:\n"
                + "{\"keywords\":[\"...\",\"...\",\"...\"],\"summary\":\"...\"}";
    }

    /**
     * @date 2026-08-13
     * @desc 단일 뉴스 항목을 일일 트렌드 LLM 입력용 텍스트로 변환합니다.
     */
    private String formatNewsForPrompt(TechNews techNews) {
        return "- 날짜: " + techNews.getNewsDate() + "\n"
                + "  출처: " + techNews.getSource() + "\n"
                + "  제목: " + techNews.getTitle() + "\n"
                + "  요약: " + truncate(techNews.getSummary(), MAX_SUMMARY_LENGTH) + "\n"
                + "  URL: " + techNews.getUrl();
    }

    /**
     * @date 2026-08-13
     * @desc 키워드 공백과 내부 쉼표, 중복을 제거하고 3~5개 범위로 검증합니다.
     */
    private List<String> normalizeKeywords(List<String> keywords) {
        LinkedHashSet<String> normalizedKeywords = new LinkedHashSet<>();
        Optional.ofNullable(keywords).orElseGet(List::of).forEach(keyword -> {
            String normalizedKeyword = Optional.ofNullable(keyword).orElse("").trim().replace(",", "");
            if (!normalizedKeyword.isBlank()) {
                normalizedKeywords.add(normalizedKeyword);
            }
        });
        if (normalizedKeywords.size() < MIN_KEYWORD_COUNT) {
            throw new LlmClientException(
                    "LLM",
                    "invalid_daily_trend_keywords",
                    200,
                    "LLM 응답의 트렌드 키워드가 부족합니다.",
                    "일일 트렌드 응답의 유효 키워드가 3개 미만입니다."
            );
        }
        return normalizedKeywords.stream().limit(MAX_KEYWORD_COUNT).toList();
    }

    /**
     * @date 2026-08-13
     * @desc LLM 일일 트렌드 요약이 비어 있지 않은지 검증합니다.
     */
    private String validateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            throw new LlmClientException(
                    "LLM",
                    "missing_required_field",
                    200,
                    "LLM 응답에 트렌드 요약이 누락되었습니다.",
                    "일일 트렌드 응답 필수 필드 누락: summary"
            );
        }
        return summary.trim();
    }

    /**
     * @date 2026-08-13
     * @desc 일일 트렌드 생성 성공 또는 실패 이력을 별도 이력 테이블에 저장합니다.
     */
    private void saveGenerationHistory(
            String triggerType,
            LocalDate targetDate,
            String status,
            Integer sourceNewsCount,
            Long createdTrendId,
            String errorMessage
    ) {
        dailyTrendGenerationHistoryRepository.save(DailyTrendGenerationHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status(status)
                .sourceNewsCount(sourceNewsCount)
                .createdTrendId(createdTrendId)
                .errorMessage(errorMessage == null ? null : truncate(errorMessage, MAX_ERROR_MESSAGE_LENGTH))
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * @date 2026-08-13
     * @desc LLM 프롬프트와 이력 문자열이 지정 길이를 넘지 않도록 제한합니다.
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }
}
