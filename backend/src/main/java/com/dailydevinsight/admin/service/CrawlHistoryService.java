package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.admin.repository.CrawlHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlHistoryService {

    private final CrawlHistoryRepository crawlHistoryRepository;

    /**
     * @date 2026-04-17
     * @desc 최근 크롤링 이력 20건을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrawlHistory> findRecentHistory() {
        return crawlHistoryRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
