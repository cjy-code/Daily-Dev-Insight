package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawlPreviewItem {

    private String title;
    private String url;
}
