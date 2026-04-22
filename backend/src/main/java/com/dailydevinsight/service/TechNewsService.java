package com.dailydevinsight.service;

import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechNewsService {

    private static final int RECENT_NEWS_DEFAULT_DAYS = 2;
    private static final int RECENT_NEWS_FALLBACK_DAYS = 3;
    private static final int RECENT_NEWS_MIN_COUNT = 6;

    private final TechNewsRepository techNewsRepository;

    /**
     * @date 2026-04-22
     * @desc 기준일 기준 최근 2일 뉴스를 우선 조회하고 부족하면 3일까지 확장하여 중복 없이 반환합니다.
     */
    public List<TechNews> findNewsByDate(LocalDate targetDate) {
        List<TechNews> recentTwoDaysNews = findNewsByRecentDays(targetDate, RECENT_NEWS_DEFAULT_DAYS);
        if (recentTwoDaysNews.size() >= RECENT_NEWS_MIN_COUNT) {
            return recentTwoDaysNews;
        }
        return findNewsByRecentDays(targetDate, RECENT_NEWS_FALLBACK_DAYS);
    }

    /**
     * @date 2026-04-22
     * @desc 최근 N일 범위 뉴스를 날짜 내림차순으로 조회하고 URL 기준 중복을 제거합니다.
     */
    private List<TechNews> findNewsByRecentDays(LocalDate targetDate, int days) {
        LocalDate startDate = targetDate.minusDays(days - 1L);
        List<TechNews> newsList = techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(startDate, targetDate);
        return deduplicateNewsByUrl(newsList);
    }

    /**
     * @date 2026-04-22
     * @desc URL 기준으로 뉴스 중복을 제거하되 URL이 비어 있으면 ID 기준으로 유지합니다.
     */
    private List<TechNews> deduplicateNewsByUrl(List<TechNews> newsList) {
        Set<String> seenKeySet = new LinkedHashSet<>();
        List<TechNews> deduplicatedList = new ArrayList<>();
        for (TechNews news : newsList) {
            String deduplicationKey = resolveNewsDeduplicationKey(news);
            if (!seenKeySet.add(deduplicationKey)) {
                continue;
            }
            deduplicatedList.add(news);
        }
        return deduplicatedList;
    }

    /**
     * @date 2026-04-22
     * @desc 뉴스 중복 제거 키를 URL 우선으로 생성하고 URL 미존재 시 ID 키를 생성합니다.
     */
    private String resolveNewsDeduplicationKey(TechNews news) {
        String normalizedUrl = normalizeOptionalText(news.getUrl());
        if (!normalizedUrl.isBlank()) {
            return "url:" + normalizedUrl;
        }
        return "id:" + news.getId();
    }

    /**
     * @date 2026-04-22
     * @desc 선택 문자열을 trim 처리하고 null은 빈 문자열로 변환합니다.
     */
    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
