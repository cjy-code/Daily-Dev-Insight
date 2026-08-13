package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GeneratedDailyTrendResult {

    private final List<String> keywords;
    private final String summary;
}
