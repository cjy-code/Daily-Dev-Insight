package com.dailydevinsight.admin.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTopContentMetricData {

    private String contentType;
    private Long contentId;
    private String title;
    private Long metricValue;
}
