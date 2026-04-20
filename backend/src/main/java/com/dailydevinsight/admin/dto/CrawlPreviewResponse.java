package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CrawlPreviewResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private int collectedCount;
    private int filteredCount;
    private List<CrawlPreviewItem> previewItems;
}
