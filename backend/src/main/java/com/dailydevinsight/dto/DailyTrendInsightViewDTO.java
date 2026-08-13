package com.dailydevinsight.dto;

import com.dailydevinsight.entity.DailyTrendInsight;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class DailyTrendInsightViewDTO {

    private final Long id;
    private final LocalDate trendDate;
    private final List<String> keywords;
    private final String summary;
    private final Integer sourceNewsCount;
    private final Boolean visible;

    /**
     * @date 2026-08-13
     * @desc 일일 개발 트렌드 엔티티를 화면 표시용 DTO로 변환합니다.
     */
    public static DailyTrendInsightViewDTO from(DailyTrendInsight dailyTrendInsight) {
        if (dailyTrendInsight == null) {
            return null;
        }

        return DailyTrendInsightViewDTO.builder()
                .id(dailyTrendInsight.getId())
                .trendDate(dailyTrendInsight.getTrendDate())
                .keywords(splitKeywords(dailyTrendInsight.getKeywords()))
                .summary(dailyTrendInsight.getSummary())
                .sourceNewsCount(dailyTrendInsight.getSourceNewsCount())
                .visible(dailyTrendInsight.getVisible())
                .build();
    }

    /**
     * @date 2026-08-13
     * @desc 쉼표로 저장된 키워드를 공백과 빈 값을 제거한 목록으로 변환합니다.
     */
    private static List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .toList();
    }
}
