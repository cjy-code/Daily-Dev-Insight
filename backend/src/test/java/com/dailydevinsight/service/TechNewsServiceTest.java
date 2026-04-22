package com.dailydevinsight.service;

import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechNewsServiceTest {

    /**
     * @date 2026-04-22
     * @desc 최근 2일 데이터가 6건 이상이면 3일 fallback 없이 2일 조회 결과를 반환하는지 검증합니다.
     */
    @Test
    void findNewsByDate_ShouldReturnRecentTwoDaysWhenEnoughData() {
        TechNewsRepository repository = mock(TechNewsRepository.class);
        TechNewsService service = new TechNewsService(repository);

        LocalDate targetDate = LocalDate.of(2026, 4, 22);
        LocalDate twoDaysStartDate = targetDate.minusDays(1L);
        LocalDate threeDaysStartDate = targetDate.minusDays(2L);
        List<TechNews> twoDaysNewsList = List.of(
                createNews(1L, targetDate, "https://a.com/1", "A"),
                createNews(2L, targetDate, "https://a.com/2", "B"),
                createNews(3L, targetDate, "https://a.com/3", "C"),
                createNews(4L, targetDate.minusDays(1L), "https://a.com/4", "D"),
                createNews(5L, targetDate.minusDays(1L), "https://a.com/5", "E"),
                createNews(6L, targetDate.minusDays(1L), "https://a.com/6", "F")
        );
        when(repository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(twoDaysStartDate, targetDate))
                .thenReturn(twoDaysNewsList);

        List<TechNews> result = service.findNewsByDate(targetDate);

        assertEquals(6, result.size());
        verify(repository).findByNewsDateBetweenOrderByNewsDateDescIdDesc(twoDaysStartDate, targetDate);
        verify(repository, never()).findByNewsDateBetweenOrderByNewsDateDescIdDesc(threeDaysStartDate, targetDate);
    }

    /**
     * @date 2026-04-22
     * @desc 최근 2일 데이터가 부족하면 3일 fallback을 수행하고 URL 중복을 제거하는지 검증합니다.
     */
    @Test
    void findNewsByDate_ShouldFallbackToThreeDaysAndDeduplicateByUrl() {
        TechNewsRepository repository = mock(TechNewsRepository.class);
        TechNewsService service = new TechNewsService(repository);

        LocalDate targetDate = LocalDate.of(2026, 4, 22);
        LocalDate twoDaysStartDate = targetDate.minusDays(1L);
        LocalDate threeDaysStartDate = targetDate.minusDays(2L);

        List<TechNews> twoDaysNewsList = List.of(
                createNews(1L, targetDate, "https://a.com/1", "A"),
                createNews(2L, targetDate, "https://a.com/1", "A-duplicate"),
                createNews(3L, targetDate.minusDays(1L), "https://a.com/2", "B")
        );
        List<TechNews> threeDaysNewsList = List.of(
                createNews(4L, targetDate, "https://a.com/1", "A-new"),
                createNews(5L, targetDate.minusDays(1L), "https://a.com/2", "B-new"),
                createNews(6L, targetDate.minusDays(2L), "https://a.com/2", "B-duplicate"),
                createNews(7L, targetDate.minusDays(2L), "", "NoUrl-1"),
                createNews(8L, targetDate.minusDays(2L), "", "NoUrl-2")
        );

        when(repository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(twoDaysStartDate, targetDate))
                .thenReturn(twoDaysNewsList);
        when(repository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(threeDaysStartDate, targetDate))
                .thenReturn(threeDaysNewsList);

        List<TechNews> result = service.findNewsByDate(targetDate);

        assertEquals(4, result.size());
        assertEquals("A-new", result.get(0).getTitle());
        assertEquals("B-new", result.get(1).getTitle());
        assertEquals("NoUrl-1", result.get(2).getTitle());
        assertEquals("NoUrl-2", result.get(3).getTitle());
        verify(repository).findByNewsDateBetweenOrderByNewsDateDescIdDesc(twoDaysStartDate, targetDate);
        verify(repository).findByNewsDateBetweenOrderByNewsDateDescIdDesc(threeDaysStartDate, targetDate);
    }

    /**
     * @date 2026-04-22
     * @desc 테스트용 TechNews 엔티티를 생성합니다.
     */
    private TechNews createNews(Long id, LocalDate newsDate, String url, String title) {
        return TechNews.builder()
                .id(id)
                .newsDate(newsDate)
                .source("source")
                .title(title)
                .url(url)
                .attachmentImagePath(null)
                .summary("summary")
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
