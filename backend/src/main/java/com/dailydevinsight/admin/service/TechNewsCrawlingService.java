package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlExecutionResult;
import com.dailydevinsight.admin.dto.CrawlRunForm;
import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.repository.CrawlHistoryRepository;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechNewsCrawlingService {

    private static final int MAX_SOURCE_TEXT_LENGTH = 100;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final int MIN_MAX_ARTICLES = 1;
    private static final int MAX_MAX_ARTICLES = 100;

    private final TechNewsRepository techNewsRepository;
    private final CrawlHistoryRepository crawlHistoryRepository;
    private final NewsCrawlerClient newsCrawlerClient;
    private final NewsThumbnailStorageService newsThumbnailStorageService;

    /**
     * @date 2026-04-17
     * @desc 관리자 입력값으로 뉴스 크롤링을 실행하고 저장 결과를 반환합니다.
     */
    @Transactional
    public CrawlExecutionResult executeManualCrawling(CrawlRunForm form) {
        validateRunForm(form);
        return executeCrawling(
                form.getTargetDate(),
                form.getSourceName().trim(),
                form.getSourceUrl().trim(),
                form.getMaxArticles(),
                "MANUAL"
        );
    }

    /**
     * @date 2026-04-17
     * @desc 예약 설정값으로 뉴스 크롤링을 실행하고 저장 결과를 반환합니다.
     */
    @Transactional
    public CrawlExecutionResult executeScheduledCrawling(LocalDate targetDate, CrawlSchedule schedule) {
        return executeCrawling(
                targetDate,
                schedule.getSourceName(),
                schedule.getSourceUrl(),
                schedule.getMaxArticles(),
                "SCHEDULED"
        );
    }

    /**
     * @date 2026-04-17
     * @desc 수집/중복제거/저장/이력기록을 하나의 흐름으로 처리합니다.
     */
    private CrawlExecutionResult executeCrawling(
            LocalDate targetDate,
            String sourceName,
            String sourceUrl,
            Integer maxArticles,
            String triggerType
    ) {
        int normalizedMaxArticles = normalizeMaxArticles(maxArticles);
        try {
            List<NewsArticleData> collectedArticles = newsCrawlerClient.crawlArticles(sourceName, sourceUrl, normalizedMaxArticles);
            List<TechNews> insertTargets = buildInsertTargets(targetDate, sourceName, collectedArticles);
            if (!insertTargets.isEmpty()) {
                techNewsRepository.saveAll(insertTargets);
            }

            saveSuccessHistory(triggerType, targetDate, sourceName, normalizedMaxArticles, collectedArticles.size(), insertTargets.size());
            return CrawlExecutionResult.builder()
                    .success(true)
                    .errorCode(null)
                    .message("크롤링을 완료했습니다.")
                    .collectedCount(collectedArticles.size())
                    .insertedCount(insertTargets.size())
                    .build();
        } catch (Exception exception) {
            saveFailureHistory(triggerType, targetDate, sourceName, normalizedMaxArticles, exception);
            return CrawlExecutionResult.builder()
                    .success(false)
                    .errorCode("crawl_failed")
                    .message("크롤링에 실패했습니다: " + exception.getMessage())
                    .collectedCount(0)
                    .insertedCount(0)
                    .build();
        }
    }

    /**
     * @date 2026-04-17
     * @desc 대상 날짜 기준 신규 뉴스만 필터링하여 저장 대상 목록으로 변환합니다.
     */
    private List<TechNews> buildInsertTargets(LocalDate targetDate, String sourceName, List<NewsArticleData> collectedArticles) {
        List<TechNews> insertTargets = new ArrayList<>();
        long nextId = resolveNextNewsId();
        for (NewsArticleData article : collectedArticles) {
            if (article.getUrl() == null || article.getUrl().isBlank()) {
                continue;
            }
            if (techNewsRepository.existsByUrl(article.getUrl().trim())) {
                continue;
            }

            String thumbnailPath = resolveThumbnailPath(article, targetDate);
            TechNews techNews = TechNews.builder()
                    .id(nextId)
                    .newsDate(targetDate)
                    .source(limitLength(defaultIfBlank(article.getSourceName(), sourceName), MAX_SOURCE_TEXT_LENGTH))
                    .title(limitLength(defaultIfBlank(article.getTitle(), "제목 없음"), MAX_TITLE_LENGTH))
                    .url(limitLength(article.getUrl().trim(), MAX_URL_LENGTH))
                    .attachmentImagePath(normalizeOptionalText(thumbnailPath))
                    .summary(defaultIfBlank(article.getSummary(), "요약 정보가 없습니다."))
                    .viewCount(0L)
                    .createdAt(LocalDateTime.now())
                    .build();
            insertTargets.add(techNews);
            nextId++;
        }
        return insertTargets;
    }

    /**
     * @date 2026-04-17
     * @desc 기사 이미지 URL이 있으면 썸네일을 저장하고 공개 경로를 반환합니다.
     */
    private String resolveThumbnailPath(NewsArticleData article, LocalDate targetDate) {
        return newsThumbnailStorageService.downloadAndStoreThumbnail(article.getImageUrl(), targetDate);
    }

    /**
     * @date 2026-04-17
     * @desc 저장할 다음 tech_news ID 값을 계산합니다.
     */
    private long resolveNextNewsId() {
        return techNewsRepository.findTopByOrderByIdDesc()
                .map(TechNews::getId)
                .orElse(0L) + 1L;
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 실행 입력값의 필수값과 범위를 검증합니다.
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
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 최대 수집 건수를 최소/최대 범위로 보정합니다.
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
     * @desc 성공 이력을 crawl_history 테이블에 저장합니다.
     */
    private void saveSuccessHistory(
            String triggerType,
            LocalDate targetDate,
            String sourceName,
            int requestedCount,
            int collectedCount,
            int insertedCount
    ) {
        CrawlHistory history = CrawlHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("SUCCESS")
                .sourceName(limitLength(sourceName, MAX_SOURCE_TEXT_LENGTH))
                .requestedCount(requestedCount)
                .collectedCount(collectedCount)
                .insertedCount(insertedCount)
                .errorMessage(null)
                .createdAt(LocalDateTime.now())
                .build();
        crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-17
     * @desc 실패 이력을 crawl_history 테이블에 저장합니다.
     */
    private void saveFailureHistory(
            String triggerType,
            LocalDate targetDate,
            String sourceName,
            int requestedCount,
            Exception exception
    ) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "원인을 확인할 수 없는 오류"
                : exception.getMessage().trim();
        CrawlHistory history = CrawlHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("FAILED")
                .sourceName(limitLength(sourceName, MAX_SOURCE_TEXT_LENGTH))
                .requestedCount(requestedCount)
                .collectedCount(0)
                .insertedCount(0)
                .errorMessage(limitLength(message, MAX_ERROR_MESSAGE_LENGTH))
                .createdAt(LocalDateTime.now())
                .build();
        crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-17
     * @desc 공백 문자열이면 기본값을 반환하고, 값이 있으면 trim 결과를 반환합니다.
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * @date 2026-04-17
     * @desc 선택 입력 문자열을 trim 처리한 뒤 비어 있으면 null로 변환합니다.
     */
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * @date 2026-04-17
     * @desc 문자열을 지정한 최대 길이 이하로 잘라 DB 컬럼 길이를 보호합니다.
     */
    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() <= maxLength) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, maxLength);
    }
}
