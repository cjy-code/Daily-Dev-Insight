package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.dto.CrawlExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledCrawlingExecutor {

    private final CrawlScheduleService crawlScheduleService;
    private final TechNewsCrawlingService techNewsCrawlingService;

    /**
     * @date 2026-04-17
     * @desc 매 분 예약 조건을 확인하고 필요 시 뉴스 크롤링을 실행합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeScheduledCrawling() {
        LocalDateTime now = LocalDateTime.now();
        if (!crawlScheduleService.isExecutionDue(now)) {
            return;
        }

        CrawlSchedule schedule = crawlScheduleService.getOrCreateSchedule();
        CrawlExecutionResult executionResult = techNewsCrawlingService.executeScheduledCrawling(LocalDate.now(), schedule);
        if (!"crawl_in_progress".equals(executionResult.getErrorCode())) {
            crawlScheduleService.markExecuted(now);
        }
        log.info("Reserved crawling executed at {} (success={}, errorCode={})", now, executionResult.isSuccess(), executionResult.getErrorCode());
    }
}
