package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeneratedWeeklyInsightResult {

    private final String summary;
    private final String trendAnalysis;
    private final String developerView;
}
