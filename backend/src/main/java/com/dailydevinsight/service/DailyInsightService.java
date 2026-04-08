package com.dailydevinsight.service;

import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.TechNews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyInsightService {

    private final DailyKnowledgeService dailyKnowledgeService;
    private final TechNewsService techNewsService;

    public DailyInsightResponseDTO getInsightsByDate(LocalDate targetDate) {
        Optional<DailyKnowledge> todayKnowledgeEntity = dailyKnowledgeService.findTodayKnowledge(targetDate);
        List<TechNews> newsEntities = techNewsService.findNewsByDate(targetDate);

        DailyInsightDTO todayKnowledge = todayKnowledgeEntity
                .map(this::toKnowledgeDto)
                .orElse(null);

        List<DailyInsightDTO> newsList = newsEntities.stream()
                .map(this::toNewsDto)
                .toList();

        return DailyInsightResponseDTO.builder()
                .date(targetDate)
                .todayKnowledge(todayKnowledge)
                .newsList(newsList)
                .build();
    }

    private DailyInsightDTO toKnowledgeDto(DailyKnowledge knowledge) {
        return DailyInsightDTO.builder()
                .id(knowledge.getId())
                .title(knowledge.getTitle())
                .url(null)
                .source(knowledge.getCategory())
                .summary(knowledge.getSummary())
                .publishedAt(knowledge.getKnowledgeDate())
                .build();
    }

    private DailyInsightDTO toNewsDto(TechNews news) {
        return DailyInsightDTO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .url(news.getUrl())
                .source(news.getSource())
                .summary(news.getSummary())
                .publishedAt(news.getNewsDate())
                .build();
    }
}
