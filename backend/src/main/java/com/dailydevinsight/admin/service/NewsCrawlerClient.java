package com.dailydevinsight.admin.service;

import java.util.List;

public interface NewsCrawlerClient {

    /**
     * @date 2026-04-17
     * @desc 지정한 소스 URL에서 뉴스 목록을 수집하여 반환합니다.
     */
    List<NewsArticleData> crawlArticles(String sourceName, String sourceUrl, int maxArticles);
}
