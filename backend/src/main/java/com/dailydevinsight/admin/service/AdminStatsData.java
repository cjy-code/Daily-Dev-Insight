package com.dailydevinsight.admin.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsData {

    private long totalUsers;
    private long activeUsers;
    private long totalPosts;
    private long todayPosts;
    private long generationSuccessCount;
    private long generationFailedCount;
    private long totalViewCount;
    private long totalBookmarkCount;
}
