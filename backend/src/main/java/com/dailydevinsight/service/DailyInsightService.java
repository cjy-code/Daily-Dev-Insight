package com.dailydevinsight.service;

import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.TechNews;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyInsightService {

    private final DailyKnowledgeService dailyKnowledgeService;
    private final TechNewsService techNewsService;

    /**
     * @date 2026-04-13
     * @desc 단일 기준일의 인사이트/뉴스/TOP10 정보를 조회합니다.
     */
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, key = "#targetDate.toString()")
    public DailyInsightResponseDTO getInsightsByDate(LocalDate targetDate) {
        Optional<DailyKnowledge> todayKnowledgeEntity = dailyKnowledgeService.findTodayKnowledge(targetDate);
        List<TechNews> newsEntities = techNewsService.findNewsByDate(targetDate);

        DailyInsightDTO todayKnowledge = todayKnowledgeEntity
                .map(this::toKnowledgeDto)
                .orElse(null);

        List<DailyInsightDTO> newsList = newsEntities.stream()
                .map(this::toNewsDto)
                .toList();

        List<DailyInsightDTO> dailyKnowledgeList = todayKnowledge != null
                ? List.of(todayKnowledge)
                : Collections.emptyList();
        List<DailyInsightDTO> techNewsList = newsList;
        List<DailyInsightDTO> top10List = dailyKnowledgeService.findWeeklyHotKnowledgeTop10(targetDate).stream()
                .map(this::toKnowledgeDto)
                .toList();
        List<DailyInsightDTO> top5List = dailyKnowledgeService.findWeeklyHotKnowledgeTop5(targetDate).stream()
                .map(this::toKnowledgeDto)
                .toList();

        return DailyInsightResponseDTO.builder()
                .date(targetDate)
                .dailyKnowledgeList(dailyKnowledgeList)
                .techNewsList(techNewsList)
                .top10List(top10List)
                .top5List(top5List)
                .weeklyHotList(top10List)
                .todayKnowledge(todayKnowledge)
                .newsList(newsList)
                .build();
    }

    /**
     * @date 2026-04-13
     * @desc 기간 조회 결과와 기준 주차 TOP10을 함께 반환합니다.
     */
    @Cacheable(
            cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE,
            key = "(#startDate == null ? '' : #startDate.toString())"
                    + " + ':' + "
                    + "(#endDate == null ? '' : #endDate.toString())"
                    + " + ':' + "
                    + "(#keyword == null ? '' : #keyword.trim().toLowerCase())"
                    + " + ':' + "
                    + "(#searchType == null ? '' : #searchType.trim().toLowerCase())"
    )
    public DailyInsightResponseDTO getInsightsByRange(LocalDate startDate, LocalDate endDate, String keyword, String searchType) {
        LocalDate normalizedStart = startDate;
        LocalDate normalizedEnd = endDate;
        if (normalizedStart.isAfter(normalizedEnd)) {
            normalizedStart = endDate;
            normalizedEnd = startDate;
        }

        List<DailyInsightDTO> knowledgeDtos = dailyKnowledgeService.findKnowledgeByDateRange(normalizedStart, normalizedEnd).stream()
                .map(this::toKnowledgeDto)
                .toList();

        List<DailyInsightDTO> filteredKnowledge = applyKnowledgeFilter(knowledgeDtos, keyword, searchType);
        DailyInsightDTO todayKnowledge = dailyKnowledgeService.findTodayKnowledge(LocalDate.now())
                .map(this::toKnowledgeDto)
                .orElse(null);
        List<DailyInsightDTO> top10List = dailyKnowledgeService.findWeeklyHotKnowledgeTop10(normalizedEnd).stream()
                .map(this::toKnowledgeDto)
                .toList();
        List<DailyInsightDTO> top5List = dailyKnowledgeService.findWeeklyHotKnowledgeTop5(normalizedEnd).stream()
                .map(this::toKnowledgeDto)
                .toList();

        // 뉴스는 기존 동작 유지: 종료일 기준 조회.
        List<DailyInsightDTO> techNewsList = techNewsService.findNewsByDate(normalizedEnd).stream()
                .map(this::toNewsDto)
                .toList();

        return DailyInsightResponseDTO.builder()
                .date(normalizedEnd)
                .dailyKnowledgeList(filteredKnowledge)
                .techNewsList(techNewsList)
                .top10List(top10List)
                .top5List(top5List)
                .weeklyHotList(top10List)
                .todayKnowledge(todayKnowledge)
                .newsList(techNewsList)
                .build();
    }

    /**
     * @date 2026-04-13
     * @desc 검색 조건에 맞는 개발 인사이트 목록을 필터링합니다.
     */
    private List<DailyInsightDTO> applyKnowledgeFilter(List<DailyInsightDTO> knowledgeList, String keyword, String searchType) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalizedKeyword.isEmpty()) {
            return knowledgeList;
        }

        String normalizedSearchType = (searchType == null || searchType.isBlank()) ? "title_content" : searchType;
        return knowledgeList.stream()
                .filter(item -> matchesByType(item, normalizedKeyword, normalizedSearchType))
                .toList();
    }

    /**
     * @date 2026-04-13
     * @desc 검색 타입별 제목/요약 매칭 여부를 판별합니다.
     */
    private boolean matchesByType(DailyInsightDTO item, String keyword, String searchType) {
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase();
        String summary = item.getSummary() == null ? "" : item.getSummary().toLowerCase();

        return switch (searchType) {
            case "title" -> title.contains(keyword);
            case "content" -> summary.contains(keyword);
            default -> title.contains(keyword) || summary.contains(keyword);
        };
    }

    /**
     * @date 2026-04-13
     * @desc DailyKnowledge 엔티티를 응답 DTO로 변환합니다.
     */
    private DailyInsightDTO toKnowledgeDto(DailyKnowledge knowledge) {
        return DailyInsightDTO.builder()
                .id(knowledge.getId())
                .title(knowledge.getTitle())
                .url(null)
                .thumbnailUrl(knowledge.getAttachmentImagePath())
                .source(knowledge.getCategory())
                .summary(knowledge.getSummary())
                .publishedAt(knowledge.getKnowledgeDate())
                .build();
    }

    /**
     * @date 2026-04-13
     * @desc TechNews 엔티티를 응답 DTO로 변환합니다.
     */
    private DailyInsightDTO toNewsDto(TechNews news) {
        return DailyInsightDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .url(news.getUrl())
                .thumbnailUrl(news.getAttachmentImagePath())
                .source(news.getSource())
                .summary(news.getSummary())
                .publishedAt(news.getNewsDate())
                .build();
    }
}
