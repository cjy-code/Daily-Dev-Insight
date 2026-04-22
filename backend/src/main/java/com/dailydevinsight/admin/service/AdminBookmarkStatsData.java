package com.dailydevinsight.admin.service;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminBookmarkStatsData {

    private long totalBookmarkCount;
    private long bookmarkedUserCount;
    private List<AdminTopContentMetricData> topBookmarkedContentList;
}
