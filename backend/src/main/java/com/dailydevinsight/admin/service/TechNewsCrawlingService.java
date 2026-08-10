package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlExecutionResult;
import com.dailydevinsight.admin.dto.CrawlPreviewItem;
import com.dailydevinsight.admin.dto.CrawlPreviewResponse;
import com.dailydevinsight.admin.dto.CrawlRunForm;
import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.entity.TechNews;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechNewsCrawlingService {

    private static final int MIN_MAX_ARTICLES = 1;
    private static final int MAX_MAX_ARTICLES = 100;
    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;
    private static final int MIN_RETRY_COUNT = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int MAX_FILTER_KEYWORD_COUNT = 5;

    private final AtomicBoolean crawlingInProgress = new AtomicBoolean(false);

    private final TechNewsPersistenceService techNewsPersistenceService;
    private final CrawlHistoryService crawlHistoryService;
    private final NewsCrawlerClient newsCrawlerClient;
    private final NewsThumbnailStorageService newsThumbnailStorageService;

    /**
     * @date 2026-04-17
     * @desc 관리자 입력값으로 즉시 수동 크롤링을 실행합니다.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true)
    })
    public CrawlExecutionResult executeManualCrawling(CrawlRunForm form) {
        validateRunForm(form);
        return executeCrawling(
                form.getTargetDate(),
                form.getSourceName().trim(),
                form.getSourceUrl().trim(),
                form.getMaxArticles(),
                normalizeKeywordMatchType(form.getKeywordMatchType()),
                normalizeStringList(form.getIncludeKeywords()),
                normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords(), form.getKeywordMatchType()),
                normalizeStringList(form.getExcludeKeywords()),
                normalizeStringList(form.getTargetDomains()),
                normalizeTimeoutSeconds(form.getConnectTimeoutSeconds()),
                normalizeTimeoutSeconds(form.getReadTimeoutSeconds()),
                normalizeRetryCount(form.getRetryCount()),
                false,
                "MANUAL"
        );
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 입력값으로 미리보기(제목/URL) 목록을 생성합니다.
     */
    @Transactional(readOnly = true)
    public CrawlPreviewResponse previewManualCrawling(CrawlRunForm form) {
        try {
            validateRunForm(form);

            List<String> includeKeywords = normalizeStringList(form.getIncludeKeywords());
            List<String> includeKeywordOperators = normalizeIncludeKeywordOperators(
                    form.getIncludeKeywordOperators(),
                    form.getIncludeKeywords(),
                    form.getKeywordMatchType()
            );
            List<String> excludeKeywords = normalizeStringList(form.getExcludeKeywords());
            List<String> targetDomains = normalizeStringList(form.getTargetDomains());

            List<NewsArticleData> collectedArticles = newsCrawlerClient.crawlArticles(
                    form.getSourceName().trim(),
                    form.getSourceUrl().trim(),
                    normalizeMaxArticles(form.getMaxArticles()),
                    normalizeTimeoutSeconds(form.getConnectTimeoutSeconds()),
                    normalizeTimeoutSeconds(form.getReadTimeoutSeconds()),
                    normalizeRetryCount(form.getRetryCount())
            );

            List<NewsArticleData> filteredArticles = filterArticles(
                    collectedArticles,
                    includeKeywords,
                    includeKeywordOperators,
                    excludeKeywords,
                    normalizeKeywordMatchType(form.getKeywordMatchType()),
                    targetDomains
            );

            List<CrawlPreviewItem> previewItems = filteredArticles.stream()
                    .map(article -> CrawlPreviewItem.builder()
                            .title(defaultIfBlank(article.getTitle(), "제목 없음"))
                            .url(defaultIfBlank(article.getUrl(), ""))
                            .build())
                    .collect(Collectors.toList());

            return CrawlPreviewResponse.builder()
                    .success(true)
                    .errorCode(null)
                    .message("미리보기 생성이 완료되었습니다.")
                    .collectedCount(collectedArticles.size())
                    .filteredCount(previewItems.size())
                    .previewItems(previewItems)
                    .build();
        } catch (Exception exception) {
            return CrawlPreviewResponse.builder()
                    .success(false)
                    .errorCode("crawl_preview_failed")
                    .message("미리보기 생성에 실패했습니다: " + exception.getMessage())
                    .collectedCount(0)
                    .filteredCount(0)
                    .previewItems(Collections.emptyList())
                    .build();
        }
    }

    /**
     * @date 2026-04-17
     * @desc 예약 설정값으로 스케줄 크롤링을 실행합니다.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true)
    })
    public CrawlExecutionResult executeScheduledCrawling(LocalDate targetDate, CrawlSchedule schedule) {
        return executeCrawling(
                targetDate,
                schedule.getSourceName(),
                schedule.getSourceUrl(),
                schedule.getMaxArticles(),
                normalizeKeywordMatchType(schedule.getKeywordMatchType()),
                splitCsv(schedule.getIncludeKeywords()),
                normalizeIncludeKeywordOperators(splitCsv(schedule.getIncludeKeywordOperators()), splitCsv(schedule.getIncludeKeywords()), schedule.getKeywordMatchType()),
                splitCsv(schedule.getExcludeKeywords()),
                splitCsv(schedule.getTargetDomains()),
                normalizeTimeoutSeconds(schedule.getConnectTimeoutSeconds()),
                normalizeTimeoutSeconds(schedule.getReadTimeoutSeconds()),
                normalizeRetryCount(schedule.getRetryCount()),
                Boolean.TRUE.equals(schedule.getAllowDuplicate()),
                "SCHEDULED"
        );
    }

    /**
     * @date 2026-04-17
     * @desc 트랜잭션 없이 수집/필터/썸네일 처리를 수행하고 저장과 이력 기록을 위임합니다.
     */
    private CrawlExecutionResult executeCrawling(
            LocalDate targetDate,
            String sourceName,
            String sourceUrl,
            Integer maxArticles,
            String keywordMatchType,
            List<String> includeKeywords,
            List<String> includeKeywordOperators,
            List<String> excludeKeywords,
            List<String> targetDomains,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int retryCount,
            boolean allowDuplicate,
            String triggerType
    ) {
        int normalizedMaxArticles = normalizeMaxArticles(maxArticles);

        if (!crawlingInProgress.compareAndSet(false, true)) {
            crawlHistoryService.recordSkipped(
                    triggerType,
                    targetDate,
                    sourceName,
                    normalizedMaxArticles,
                    "이미 다른 크롤링 작업이 실행 중입니다."
            );
            return CrawlExecutionResult.builder()
                    .success(false)
                    .errorCode("crawl_in_progress")
                    .message("이미 실행 중인 크롤링이 있어 이번 요청은 건너뜁니다.")
                    .collectedCount(0)
                    .insertedCount(0)
                    .build();
        }

        CrawlHistory runningHistory = crawlHistoryService.recordRunning(
                triggerType,
                targetDate,
                sourceName,
                normalizedMaxArticles
        );
        try {
            List<NewsArticleData> collectedArticles = newsCrawlerClient.crawlArticles(
                    sourceName,
                    sourceUrl,
                    normalizedMaxArticles,
                    connectTimeoutSeconds,
                    readTimeoutSeconds,
                    retryCount
            );
            List<NewsArticleData> filteredArticles = filterArticles(
                    collectedArticles,
                    includeKeywords,
                    includeKeywordOperators,
                    excludeKeywords,
                    keywordMatchType,
                    targetDomains
            );
            List<EnrichedArticle> enrichedArticles = filteredArticles.stream()
                    .map(article -> new EnrichedArticle(article, resolveThumbnailPath(article, targetDate)))
                    .collect(Collectors.toList());
            List<TechNews> savedArticles = techNewsPersistenceService.persistArticles(
                    targetDate,
                    sourceName,
                    enrichedArticles,
                    allowDuplicate
            );

            crawlHistoryService.recordSuccess(runningHistory, collectedArticles.size(), savedArticles.size());
            return CrawlExecutionResult.builder()
                    .success(true)
                    .errorCode(null)
                    .message("크롤링이 완료되었습니다.")
                    .collectedCount(collectedArticles.size())
                    .insertedCount(savedArticles.size())
                    .build();
        } catch (Exception exception) {
            crawlHistoryService.recordFailure(runningHistory, exception);
            return CrawlExecutionResult.builder()
                    .success(false)
                    .errorCode("crawl_failed")
                    .message("크롤링에 실패했습니다: " + exception.getMessage())
                    .collectedCount(0)
                    .insertedCount(0)
                    .build();
        } finally {
            crawlingInProgress.set(false);
        }
    }

    /**
     * @date 2026-04-17
     * @desc 키워드/도메인 조건으로 수집 기사 목록을 필터링합니다.
     */
    private List<NewsArticleData> filterArticles(
            List<NewsArticleData> collectedArticles,
            List<String> includeKeywords,
            List<String> includeKeywordOperators,
            List<String> excludeKeywords,
            String keywordMatchType,
            List<String> targetDomains
    ) {
        return collectedArticles.stream()
                .filter(article -> matchesKeywordCondition(article, includeKeywords, includeKeywordOperators, keywordMatchType))
                .filter(article -> matchesExcludeKeywordCondition(article, excludeKeywords))
                .filter(article -> matchesTargetDomains(article, targetDomains))
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-04-17
     * @desc 기사 제목/요약/본문/URL 기준으로 포함 키워드 조건(AND/OR)을 평가합니다.
     */
    private boolean matchesKeywordCondition(
            NewsArticleData article,
            List<String> includeKeywords,
            List<String> includeKeywordOperators,
            String keywordMatchType
    ) {
        if (includeKeywords.isEmpty()) {
            return true;
        }

        String searchText = (
                defaultIfBlank(article.getTitle(), "") + " "
                        + defaultIfBlank(article.getSummary(), "") + " "
                        + defaultIfBlank(article.getContent(), "") + " "
                        + defaultIfBlank(article.getUrl(), "")
        ).toLowerCase(Locale.ROOT);

        List<String> normalizedOperators = normalizeIncludeKeywordOperators(includeKeywordOperators, includeKeywords, keywordMatchType);
        List<Boolean> keywordMatched = includeKeywords.stream()
                .map(keyword -> searchText.contains(keyword.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        boolean evaluationResult = keywordMatched.get(0);
        for (int index = 1; index < keywordMatched.size(); index++) {
            String operator = normalizedOperators.get(index);
            if ("AND".equals(operator)) {
                evaluationResult = evaluationResult && keywordMatched.get(index);
            } else {
                evaluationResult = evaluationResult || keywordMatched.get(index);
            }
        }
        return evaluationResult;
    }

    /**
     * @date 2026-04-17
     * @desc 기사 제목/요약/본문/URL에 제외 키워드가 포함되면 제외합니다.
     */
    private boolean matchesExcludeKeywordCondition(NewsArticleData article, List<String> excludeKeywords) {
        if (excludeKeywords.isEmpty()) {
            return true;
        }

        String searchText = (
                defaultIfBlank(article.getTitle(), "") + " "
                        + defaultIfBlank(article.getSummary(), "") + " "
                        + defaultIfBlank(article.getContent(), "") + " "
                        + defaultIfBlank(article.getUrl(), "")
        ).toLowerCase(Locale.ROOT);

        return excludeKeywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .noneMatch(searchText::contains);
    }

    /**
     * @date 2026-04-17
     * @desc 기사 URL 호스트가 대상 도메인 목록에 포함되는지 검사합니다.
     */
    private boolean matchesTargetDomains(NewsArticleData article, List<String> targetDomains) {
        if (targetDomains.isEmpty()) {
            return true;
        }
        if (article.getUrl() == null || article.getUrl().isBlank()) {
            return false;
        }

        String host = resolveHost(article.getUrl());
        if (host.isBlank()) {
            return false;
        }

        for (String targetDomain : targetDomains) {
            String normalizedTargetDomain = normalizeDomain(targetDomain);
            if (normalizedTargetDomain.isBlank()) {
                continue;
            }
            if (host.equals(normalizedTargetDomain) || host.endsWith("." + normalizedTargetDomain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @date 2026-04-17
     * @desc URL 문자열에서 host 값을 추출합니다.
     */
    private String resolveHost(String urlText) {
        try {
            URI uri = URI.create(urlText.trim());
            if (uri.getHost() == null) {
                return "";
            }
            return normalizeDomain(uri.getHost());
        } catch (Exception exception) {
            return "";
        }
    }

    /**
     * @date 2026-04-17
     * @desc 도메인 문자열을 소문자/프로토콜 제거/경로 제거 형태로 정규화합니다.
     */
    private String normalizeDomain(String domainText) {
        if (domainText == null || domainText.isBlank()) {
            return "";
        }
        String normalizedDomain = domainText.trim().toLowerCase(Locale.ROOT);
        if (normalizedDomain.startsWith("http://")) {
            normalizedDomain = normalizedDomain.substring("http://".length());
        } else if (normalizedDomain.startsWith("https://")) {
            normalizedDomain = normalizedDomain.substring("https://".length());
        }
        if (normalizedDomain.startsWith("www.")) {
            normalizedDomain = normalizedDomain.substring("www.".length());
        }
        int slashIndex = normalizedDomain.indexOf('/');
        if (slashIndex >= 0) {
            normalizedDomain = normalizedDomain.substring(0, slashIndex);
        }
        return normalizedDomain;
    }

    /**
     * @date 2026-04-17
     * @desc 기사 대표 이미지 URL 기반으로 썸네일을 저장하고 공개 경로를 반환합니다.
     */
    private String resolveThumbnailPath(NewsArticleData article, LocalDate targetDate) {
        return newsThumbnailStorageService.downloadAndStoreThumbnail(article.getImageUrl(), targetDate);
    }

    /**
     * @date 2026-04-17
     * @desc 수동 실행 요청값의 필수값과 범위를 검증합니다.
     */
    private void validateRunForm(CrawlRunForm form) {
        if (form == null) {
            throw new IllegalArgumentException("크롤링 요청 값이 없습니다.");
        }
        if (form.getTargetDate() == null) {
            throw new IllegalArgumentException("대상 날짜는 필수입니다.");
        }
        if (form.getSourceName() == null || form.getSourceName().isBlank()) {
            throw new IllegalArgumentException("소스 이름은 필수입니다.");
        }
        if (form.getSourceUrl() == null || form.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("소스 URL은 필수입니다.");
        }
        if (form.getMaxArticles() == null) {
            throw new IllegalArgumentException("최대 수집 건수는 필수입니다.");
        }
        if (form.getMaxArticles() < MIN_MAX_ARTICLES || form.getMaxArticles() > MAX_MAX_ARTICLES) {
            throw new IllegalArgumentException("최대 수집 건수는 " + MIN_MAX_ARTICLES + " ~ " + MAX_MAX_ARTICLES + " 사이여야 합니다.");
        }
        validateKeywordCount(form.getIncludeKeywords(), "포함 키워드");
        validateKeywordCount(form.getExcludeKeywords(), "제외 키워드");
        normalizeKeywordMatchType(form.getKeywordMatchType());
        normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords(), form.getKeywordMatchType());
        normalizeTimeoutSeconds(form.getConnectTimeoutSeconds());
        normalizeTimeoutSeconds(form.getReadTimeoutSeconds());
        normalizeRetryCount(form.getRetryCount());
    }

    /**
     * @date 2026-04-17
     * @desc 최대 수집 건수를 허용 범위로 보정합니다.
     */
    private int normalizeMaxArticles(Integer maxArticles) {
        if (maxArticles == null) {
            return MIN_MAX_ARTICLES;
        }
        if (maxArticles < MIN_MAX_ARTICLES) {
            return MIN_MAX_ARTICLES;
        }
        return Math.min(maxArticles, MAX_MAX_ARTICLES);
    }

    /**
     * @date 2026-04-17
     * @desc 키워드 매칭 타입을 AND/OR로 정규화합니다.
     */
    private String normalizeKeywordMatchType(String keywordMatchType) {
        String normalizedKeywordMatchType = keywordMatchType == null ? "OR" : keywordMatchType.trim().toUpperCase();
        if (!"AND".equals(normalizedKeywordMatchType) && !"OR".equals(normalizedKeywordMatchType)) {
            throw new IllegalArgumentException("키워드 조건은 AND 또는 OR만 허용됩니다.");
        }
        return normalizedKeywordMatchType;
    }

    /**
     * @date 2026-04-17
     * @desc 포함 키워드 개수에 맞춰 연산자 목록(AND/OR)을 보정합니다.
     */
    private List<String> normalizeIncludeKeywordOperators(
            List<String> includeKeywordOperators,
            List<String> includeKeywords,
            String fallbackKeywordMatchType
    ) {
        List<String> normalizedKeywords = normalizeStringList(includeKeywords);
        if (normalizedKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        String fallbackOperator = normalizeKeywordMatchType(fallbackKeywordMatchType);
        List<String> normalizedOperators = normalizeStringList(includeKeywordOperators).stream()
                .map(operator -> operator.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());

        int requiredOperatorCount = normalizedKeywords.size();
        if (normalizedOperators.size() == requiredOperatorCount - 1) {
            normalizedOperators.add(0, fallbackOperator);
        }
        if (normalizedOperators.size() > requiredOperatorCount) {
            normalizedOperators = normalizedOperators.subList(0, requiredOperatorCount);
        }
        while (normalizedOperators.size() < requiredOperatorCount) {
            normalizedOperators.add(fallbackOperator);
        }

        for (String operator : normalizedOperators) {
            if (!"AND".equals(operator) && !"OR".equals(operator)) {
                throw new IllegalArgumentException("포함 키워드 연산자는 AND 또는 OR만 허용됩니다.");
            }
        }
        return normalizedOperators;
    }

    /**
     * @date 2026-04-20
     * @desc 포함/제외 키워드는 최대 5개까지 허용합니다.
     */
    private void validateKeywordCount(List<String> keywords, String fieldName) {
        int keywordCount = normalizeStringList(keywords).size();
        if (keywordCount > MAX_FILTER_KEYWORD_COUNT) {
            throw new IllegalArgumentException(fieldName + "는 최대 " + MAX_FILTER_KEYWORD_COUNT + "개까지만 입력할 수 있습니다.");
        }
    }

    /**
     * @date 2026-04-17
     * @desc 타임아웃 값을 허용 범위로 보정합니다.
     */
    private int normalizeTimeoutSeconds(Integer timeoutSeconds) {
        if (timeoutSeconds == null) {
            return MIN_TIMEOUT_SECONDS;
        }
        if (timeoutSeconds < MIN_TIMEOUT_SECONDS) {
            return MIN_TIMEOUT_SECONDS;
        }
        return Math.min(timeoutSeconds, MAX_TIMEOUT_SECONDS);
    }

    /**
     * @date 2026-04-17
     * @desc 재시도 횟수를 허용 범위로 보정합니다.
     */
    private int normalizeRetryCount(Integer retryCount) {
        if (retryCount == null) {
            return MIN_RETRY_COUNT;
        }
        if (retryCount < MIN_RETRY_COUNT) {
            return MIN_RETRY_COUNT;
        }
        return Math.min(retryCount, MAX_RETRY_COUNT);
    }

    /**
     * @date 2026-04-17
     * @desc 문자열 목록에서 빈 값과 공백을 제거하고 중복을 제거합니다.
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
     * @date 2026-04-17
     * @desc CSV 문자열을 문자열 목록으로 분리합니다.
     */
    private List<String> splitCsv(String csvText) {
        if (csvText == null || csvText.isBlank()) {
            return Collections.emptyList();
        }
        String[] tokens = csvText.split(",");
        List<String> values = new ArrayList<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            values.add(token.trim());
        }
        return values;
    }

    /**
     * @date 2026-04-17
     * @desc 공백 문자열이면 기본값을 반환하고 아니면 trim 결과를 반환합니다.
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

}
