package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CrawlRunForm {

    private LocalDate targetDate;
    private String sourceName;
    private String sourceUrl;
    private Integer maxArticles;
    private String keywordMatchType;
    private List<String> includeKeywords;
    private List<String> includeKeywordOperators;
    private List<String> excludeKeywords;
    private List<String> targetDomains;
    private Integer connectTimeoutSeconds;
    private Integer readTimeoutSeconds;
    private Integer retryCount;
}
