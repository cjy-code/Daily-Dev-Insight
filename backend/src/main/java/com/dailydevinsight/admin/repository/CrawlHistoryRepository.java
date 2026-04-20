package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {

    List<CrawlHistory> findTop20ByOrderByCreatedAtDesc();
}
