package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.CrawlSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlScheduleRepository extends JpaRepository<CrawlSchedule, Long> {
}
