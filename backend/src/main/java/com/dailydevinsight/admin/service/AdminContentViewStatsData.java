package com.dailydevinsight.admin.service;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminContentViewStatsData {

    private long totalViewCount;
    private long knowledgeViewCount;
    private long newsViewCount;
    private List<AdminTopContentMetricData> topKnowledgeList;
    private List<AdminTopContentMetricData> topNewsList;
}
