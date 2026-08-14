package com.dailydevinsight.admin.service;

import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechNewsPersistenceService {

    private static final int MAX_SOURCE_TEXT_LENGTH = 100;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 500;

    private final TechNewsRepository techNewsRepository;

    /**
     * @date 2026-08-10
     * @desc 수집과 썸네일 처리가 끝난 기사에서 중복을 제거하고 ID를 할당하여 저장합니다.
     */
    @Transactional
    public List<TechNews> persistArticles(
            LocalDate targetDate,
            String sourceName,
            List<EnrichedArticle> enrichedArticles,
            boolean allowDuplicate
    ) {
        List<TechNews> insertTargets = new ArrayList<>();
        Set<String> existingUrls = allowDuplicate ? Collections.emptySet() : findExistingUrls(enrichedArticles);
        Set<String> queuedUrls = new HashSet<>();
        long nextId = resolveNextNewsId();
        for (EnrichedArticle enrichedArticle : enrichedArticles) {
            NewsArticleData article = enrichedArticle.article();
            if (article.getUrl() == null || article.getUrl().isBlank()) {
                continue;
            }
            String normalizedUrl = article.getUrl().trim();
            if (!allowDuplicate) {
                if (existingUrls.contains(normalizedUrl) || queuedUrls.contains(normalizedUrl)) {
                    continue;
                }
                queuedUrls.add(normalizedUrl);
            }

            TechNews techNews = TechNews.builder()
                    .id(nextId)
                    .newsDate(targetDate)
                    .source(limitLength(defaultIfBlank(article.getSourceName(), sourceName), MAX_SOURCE_TEXT_LENGTH))
                    .title(limitLength(defaultIfBlank(article.getTitle(), "제목 없음"), MAX_TITLE_LENGTH))
                    .url(limitLength(normalizedUrl, MAX_URL_LENGTH))
                    .attachmentImagePath(normalizeOptionalText(enrichedArticle.thumbnailPath()))
                    .summary(defaultIfBlank(article.getSummary(), "요약 정보가 없습니다."))
                    .viewCount(0L)
                    .createdAt(LocalDateTime.now())
                    .build();
            insertTargets.add(techNews);
            nextId++;
        }
        if (!insertTargets.isEmpty()) {
            techNewsRepository.saveAll(insertTargets);
        }
        return insertTargets;
    }

    /**
     * @date 2026-08-10
     * @desc 수집 기사 URL 목록으로 기존 저장 URL 집합을 조회합니다.
     */
    private Set<String> findExistingUrls(List<EnrichedArticle> enrichedArticles) {
        List<String> candidateUrls = enrichedArticles.stream()
                .map(EnrichedArticle::article)
                .map(NewsArticleData::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (candidateUrls.isEmpty()) {
            return Collections.emptySet();
        }
        return techNewsRepository.findByUrlIn(candidateUrls).stream()
                .map(TechNews::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * @date 2026-08-10
     * @desc 다음 tech_news ID 값을 계산합니다.
     */
    private long resolveNextNewsId() {
        return techNewsRepository.findTopByOrderByIdDesc()
                .map(TechNews::getId)
                .orElse(0L) + 1L;
    }

    /**
     * @date 2026-08-10
     * @desc 공백 문자열이면 기본값을 반환하고 아니면 trim 결과를 반환합니다.
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * @date 2026-08-10
     * @desc 선택 입력 문자열을 trim 처리하고 비어 있으면 null로 변환합니다.
     */
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * @date 2026-08-10
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

record EnrichedArticle(NewsArticleData article, String thumbnailPath) {
}
