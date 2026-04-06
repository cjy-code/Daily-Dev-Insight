package com.dailydevinsight.service;

import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyInsightService {

    /**
     * Return fixed sample data for MVP wiring.
     */
    public DailyInsightResponseDTO getInsightsByDate(LocalDate targetDate) {
        List<DailyInsightDTO> allItems = List.of(
                DailyInsightDTO.builder()
                        .id(1L)
                        .title("Spring Boot 3.2 Release Notes")
                        .url("https://spring.io/blog/2024/01/01/spring-boot-3-2")
                        .source("Spring Blog")
                        .summary("Spring Boot 3.2 major changes and migration notes.")
                        .publishedAt(targetDate)
                        .build(),
                DailyInsightDTO.builder()
                        .id(2L)
                        .title("Java 21 Virtual Threads in Practice")
                        .url("https://example.com/java21-vthreads")
                        .source("Dev Weekly")
                        .summary("Virtual thread usage patterns and performance trade-offs.")
                        .publishedAt(targetDate)
                        .build(),
                DailyInsightDTO.builder()
                        .id(3L)
                        .title("REST API Design Best Practices")
                        .url("https://example.com/rest-best-practices")
                        .source("API Design")
                        .summary("Resource naming and error response design basics.")
                        .publishedAt(targetDate)
                        .build()
        );

        DailyInsightDTO todayKnowledge = allItems.get(0);
        List<DailyInsightDTO> newsList = allItems.subList(1, allItems.size());

        return DailyInsightResponseDTO.builder()
                .date(targetDate)
                .todayKnowledge(todayKnowledge)
                .newsList(newsList)
                .build();
    }
}
