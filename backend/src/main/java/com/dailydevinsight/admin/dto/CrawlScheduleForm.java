package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrawlScheduleForm {

    private Boolean enabled;
    private String cronExpression;
    private String sourceName;
    private String sourceUrl;
    private Integer maxArticles;
}
