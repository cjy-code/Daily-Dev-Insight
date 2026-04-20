package com.dailydevinsight.admin.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsArticleData {

    private final String sourceName;
    private final String title;
    private final String url;
    private final String summary;
    private final String content;
    private final String imageUrl;
}
