package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CrawlRunForm {

    private LocalDate targetDate;
    private String sourceName;
    private String sourceUrl;
    private Integer maxArticles;
}
