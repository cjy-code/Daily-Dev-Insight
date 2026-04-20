package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawlExecutionResult {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final int collectedCount;
    private final int insertedCount;
}
