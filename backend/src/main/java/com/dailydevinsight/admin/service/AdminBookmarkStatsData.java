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
public class AdminBookmarkStatsData {

    private long totalBookmarkCount;
    private long bookmarkedUserCount;
    private List<AdminTopContentMetricData> topBookmarkedContentList;
}
