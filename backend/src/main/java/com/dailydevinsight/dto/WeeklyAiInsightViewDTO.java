package com.dailydevinsight.dto;

import com.dailydevinsight.entity.WeeklyAiInsight;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WeeklyAiInsightViewDTO {

    private final Long id;
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private final String summary;
    private final String trendAnalysis;
    private final String developerView;
    private final Integer sourceNewsCount;
    private final Boolean visible;

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트 엔티티를 화면 표시용 DTO로 변환합니다.
     */
    public static WeeklyAiInsightViewDTO from(WeeklyAiInsight weeklyAiInsight) {
        if (weeklyAiInsight == null) {
            return null;
        }

        return WeeklyAiInsightViewDTO.builder()
                .id(weeklyAiInsight.getId())
                .weekStartDate(weeklyAiInsight.getWeekStartDate())
                .weekEndDate(weeklyAiInsight.getWeekEndDate())
                .summary(weeklyAiInsight.getSummary())
                .trendAnalysis(weeklyAiInsight.getTrendAnalysis())
                .developerView(weeklyAiInsight.getDeveloperView())
                .sourceNewsCount(weeklyAiInsight.getSourceNewsCount())
                .visible(weeklyAiInsight.getVisible())
                .build();
    }
}
