package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlScheduleForm;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.repository.CrawlScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlScheduleService {

    private static final Long DEFAULT_SCHEDULE_ID = 1L;
    private static final String DEFAULT_CRON_EXPRESSION = "0 0 8 * * *";
    private static final String DEFAULT_SOURCE_NAME = "Hacker News";
    private static final String DEFAULT_SOURCE_URL = "https://hnrss.org/frontpage";
    private static final int DEFAULT_MAX_ARTICLES = 20;
    private static final String DEFAULT_KEYWORD_MATCH_TYPE = "OR";
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_RETRY_COUNT = 1;
    private static final int MIN_MAX_ARTICLES = 1;
    private static final int MAX_MAX_ARTICLES = 100;
    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;
    private static final int MIN_RETRY_COUNT = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int MAX_FILTER_KEYWORD_COUNT = 5;

    private final CrawlScheduleRepository crawlScheduleRepository;

    /**
     * @date 2026-04-20
     * @desc 크롤링 예약 설정을 조회하고 없으면 기본 설정을 생성합니다.
     */
    @Transactional
    public CrawlSchedule getOrCreateSchedule() {
        return crawlScheduleRepository.findById(DEFAULT_SCHEDULE_ID)
                .orElseGet(this::createDefaultSchedule);
    }

    /**
     * @date 2026-04-20
     * @desc 관리자 입력값으로 크롤링 예약 설정을 갱신합니다.
     */
    @Transactional
    public CrawlSchedule updateSchedule(CrawlScheduleForm form) {
        validateScheduleForm(form);
        CrawlSchedule currentSchedule = getOrCreateSchedule();
        CrawlSchedule updatedSchedule = CrawlSchedule.builder()
                .id(currentSchedule.getId())
                .enabled(Boolean.TRUE.equals(form.getEnabled()))
                .cronExpression(form.getCronExpression().trim())
                .sourceName(form.getSourceName().trim())
                .sourceUrl(form.getSourceUrl().trim())
                .maxArticles(form.getMaxArticles())
                .keywordMatchType(normalizeKeywordMatchType(form.getKeywordMatchType()))
                .includeKeywords(joinAsCsv(normalizeStringList(form.getIncludeKeywords())))
                .includeKeywordOperators(joinAsCsv(normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords())))
                .excludeKeywords(joinAsCsv(normalizeStringList(form.getExcludeKeywords())))
                .targetDomains(joinAsCsv(normalizeStringList(form.getTargetDomains())))
                .connectTimeoutSeconds(form.getConnectTimeoutSeconds())
                .readTimeoutSeconds(form.getReadTimeoutSeconds())
                .retryCount(form.getRetryCount())
                .lastExecutedAt(currentSchedule.getLastExecutedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        return crawlScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-20
     * @desc 현재 시각 기준으로 크롤링 예약 실행 필요 여부를 판단합니다.
     */
    @Transactional
    public boolean isExecutionDue(LocalDateTime now) {
        CrawlSchedule schedule = getOrCreateSchedule();
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            return false;
        }

        CronExpression cronExpression = CronExpression.parse(schedule.getCronExpression());
        LocalDateTime referenceTime = schedule.getLastExecutedAt() == null
                ? now.minusMinutes(1)
                : schedule.getLastExecutedAt();

        LocalDateTime nextExecutionTime = cronExpression.next(referenceTime);
        return nextExecutionTime != null && !nextExecutionTime.isAfter(now);
    }

    /**
     * @date 2026-04-20
     * @desc 크롤링 예약 실행 완료 시 마지막 실행 시각을 갱신합니다.
     */
    @Transactional
    public void markExecuted(LocalDateTime executedAt) {
        CrawlSchedule schedule = getOrCreateSchedule();
        CrawlSchedule updatedSchedule = CrawlSchedule.builder()
                .id(schedule.getId())
                .enabled(schedule.getEnabled())
                .cronExpression(schedule.getCronExpression())
                .sourceName(schedule.getSourceName())
                .sourceUrl(schedule.getSourceUrl())
                .maxArticles(schedule.getMaxArticles())
                .keywordMatchType(schedule.getKeywordMatchType())
                .includeKeywords(schedule.getIncludeKeywords())
                .includeKeywordOperators(schedule.getIncludeKeywordOperators())
                .excludeKeywords(schedule.getExcludeKeywords())
                .targetDomains(schedule.getTargetDomains())
                .connectTimeoutSeconds(schedule.getConnectTimeoutSeconds())
                .readTimeoutSeconds(schedule.getReadTimeoutSeconds())
                .retryCount(schedule.getRetryCount())
                .lastExecutedAt(executedAt)
                .updatedAt(LocalDateTime.now())
                .build();
        crawlScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-20
     * @desc 예약 설정 입력값의 필수 항목과 범위를 검증합니다.
     */
    private void validateScheduleForm(CrawlScheduleForm form) {
        if (form.getCronExpression() == null || form.getCronExpression().isBlank()) {
            throw new IllegalArgumentException("Cron expression is required.");
        }
        if (form.getSourceName() == null || form.getSourceName().isBlank()) {
            throw new IllegalArgumentException("Source name is required.");
        }
        if (form.getSourceUrl() == null || form.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("Source URL is required.");
        }
        if (form.getMaxArticles() == null) {
            throw new IllegalArgumentException("Max articles is required.");
        }
        if (form.getMaxArticles() < MIN_MAX_ARTICLES || form.getMaxArticles() > MAX_MAX_ARTICLES) {
            throw new IllegalArgumentException("Max articles must be between " + MIN_MAX_ARTICLES + " and " + MAX_MAX_ARTICLES + ".");
        }

        validateKeywordCount(form.getIncludeKeywords(), "Include keywords");
        validateKeywordCount(form.getExcludeKeywords(), "Exclude keywords");
        normalizeKeywordMatchType(form.getKeywordMatchType());
        normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords());
        validateTimeoutAndRetry(form.getConnectTimeoutSeconds(), form.getReadTimeoutSeconds(), form.getRetryCount());
        CronExpression.parse(form.getCronExpression().trim());
    }

    /**
     * @date 2026-04-20
     * @desc 키워드 매칭 타입을 AND/OR 범위로 정규화합니다.
     */
    private String normalizeKeywordMatchType(String keywordMatchType) {
        String normalizedKeywordMatchType = keywordMatchType == null ? "OR" : keywordMatchType.trim().toUpperCase();
        if (!"AND".equals(normalizedKeywordMatchType) && !"OR".equals(normalizedKeywordMatchType)) {
            throw new IllegalArgumentException("Keyword match type only supports AND or OR.");
        }
        return normalizedKeywordMatchType;
    }

    /**
     * @date 2026-04-20
     * @desc 포함 키워드 수에 맞춰 연산자 목록을 정규화합니다.
     */
    private List<String> normalizeIncludeKeywordOperators(List<String> operators, List<String> includeKeywords) {
        List<String> normalizedKeywords = normalizeStringList(includeKeywords);
        if (normalizedKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedOperators = normalizeStringList(operators).stream()
                .map(value -> value.toUpperCase())
                .collect(Collectors.toList());

        int requiredOperatorCount = normalizedKeywords.size();
        if (normalizedOperators.size() == requiredOperatorCount - 1) {
            normalizedOperators.add(0, "OR");
        }
        if (normalizedOperators.size() > requiredOperatorCount) {
            normalizedOperators = normalizedOperators.subList(0, requiredOperatorCount);
        }
        while (normalizedOperators.size() < requiredOperatorCount) {
            normalizedOperators.add("OR");
        }

        for (String operator : normalizedOperators) {
            if (!"AND".equals(operator) && !"OR".equals(operator)) {
                throw new IllegalArgumentException("Include keyword operators only support AND or OR.");
            }
        }
        return normalizedOperators;
    }

    /**
     * @date 2026-04-20
     * @desc 포함/제외 키워드는 성능 보호를 위해 최대 5개까지 허용합니다.
     */
    private void validateKeywordCount(List<String> keywords, String fieldName) {
        int keywordCount = normalizeStringList(keywords).size();
        if (keywordCount > MAX_FILTER_KEYWORD_COUNT) {
            throw new IllegalArgumentException(fieldName + " can contain at most " + MAX_FILTER_KEYWORD_COUNT + " items.");
        }
    }

    /**
     * @date 2026-04-20
     * @desc 타임아웃/재시도 옵션 범위를 검증합니다.
     */
    private void validateTimeoutAndRetry(Integer connectTimeoutSeconds, Integer readTimeoutSeconds, Integer retryCount) {
        if (connectTimeoutSeconds == null
                || connectTimeoutSeconds < MIN_TIMEOUT_SECONDS
                || connectTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("Connect timeout must be between " + MIN_TIMEOUT_SECONDS + " and " + MAX_TIMEOUT_SECONDS + " seconds.");
        }
        if (readTimeoutSeconds == null
                || readTimeoutSeconds < MIN_TIMEOUT_SECONDS
                || readTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("Read timeout must be between " + MIN_TIMEOUT_SECONDS + " and " + MAX_TIMEOUT_SECONDS + " seconds.");
        }
        if (retryCount == null || retryCount < MIN_RETRY_COUNT || retryCount > MAX_RETRY_COUNT) {
            throw new IllegalArgumentException("Retry count must be between " + MIN_RETRY_COUNT + " and " + MAX_RETRY_COUNT + ".");
        }
    }

    /**
     * @date 2026-04-20
     * @desc 문자열 목록에서 공백/빈 값을 제거하고 정규화합니다.
     */
    private List<String> normalizeStringList(List<String> sourceList) {
        if (sourceList == null) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-04-20
     * @desc 문자열 목록을 CSV 문자열로 결합합니다.
     */
    private String joinAsCsv(List<String> sourceList) {
        if (sourceList.isEmpty()) {
            return null;
        }
        return String.join(",", sourceList);
    }

    /**
     * @date 2026-04-20
     * @desc CSV 문자열을 문자열 목록으로 분리합니다.
     */
    @Transactional(readOnly = true)
    public List<String> splitCsv(String csvText) {
        if (csvText == null || csvText.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csvText.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-04-20
     * @desc 기본 크롤링 예약 설정을 생성합니다.
     */
    private CrawlSchedule createDefaultSchedule() {
        LocalDateTime now = LocalDateTime.now();
        CrawlSchedule defaultSchedule = CrawlSchedule.builder()
                .id(DEFAULT_SCHEDULE_ID)
                .enabled(false)
                .cronExpression(DEFAULT_CRON_EXPRESSION)
                .sourceName(DEFAULT_SOURCE_NAME)
                .sourceUrl(DEFAULT_SOURCE_URL)
                .maxArticles(DEFAULT_MAX_ARTICLES)
                .keywordMatchType(DEFAULT_KEYWORD_MATCH_TYPE)
                .includeKeywords(null)
                .includeKeywordOperators(null)
                .excludeKeywords(null)
                .targetDomains(null)
                .connectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS)
                .readTimeoutSeconds(DEFAULT_READ_TIMEOUT_SECONDS)
                .retryCount(DEFAULT_RETRY_COUNT)
                .lastExecutedAt(null)
                .updatedAt(now)
                .build();
        return crawlScheduleRepository.save(defaultSchedule);
    }
}
