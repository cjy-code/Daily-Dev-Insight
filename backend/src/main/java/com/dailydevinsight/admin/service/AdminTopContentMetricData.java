package com.dailydevinsight.admin.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTopContentMetricData {

    private String contentType;
    private Long contentId;
    private String title;
    private Long metricValue;
}
