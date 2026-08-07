package com.dailydevinsight.admin.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentViewStatsData {

    private long totalViewCount;
    private long knowledgeViewCount;
    private long newsViewCount;
    private List<AdminTopContentMetricData> topKnowledgeList;
    private List<AdminTopContentMetricData> topNewsList;
}
