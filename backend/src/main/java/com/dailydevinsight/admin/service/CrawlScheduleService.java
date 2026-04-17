package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlScheduleForm;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.repository.CrawlScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CrawlScheduleService {

    private static final Long DEFAULT_SCHEDULE_ID = 1L;
    private static final String DEFAULT_CRON_EXPRESSION = "0 0 8 * * *";
    private static final String DEFAULT_SOURCE_NAME = "Hacker News";
    private static final String DEFAULT_SOURCE_URL = "https://hnrss.org/frontpage";
    private static final int DEFAULT_MAX_ARTICLES = 20;
    private static final int MIN_MAX_ARTICLES = 1;
    private static final int MAX_MAX_ARTICLES = 100;

    private final CrawlScheduleRepository crawlScheduleRepository;

    /**
     * @date 2026-04-17
     * @desc 크롤링 예약 설정을 조회하고 없으면 기본 설정을 생성합니다.
     */
    @Transactional
    public CrawlSchedule getOrCreateSchedule() {
        return crawlScheduleRepository.findById(DEFAULT_SCHEDULE_ID)
                .orElseGet(this::createDefaultSchedule);
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 입력값으로 크롤링 예약 설정을 갱신합니다.
     */
    @Transactional
    public CrawlSchedule updateSchedule(CrawlScheduleForm form) {
        validateScheduleForm(form);
        CrawlSchedule currentSchedule = getOrCreateSchedule();
        CrawlSchedule updatedSchedule = CrawlSchedule.builder()
                .id(currentSchedule.getId())
                .enabled(Boolean.TRUE.equals(form.getEnabled()))
                .cronExpression(form.getCronExpression().trim())
                .sourceName(form.getSourceName().trim())
                .sourceUrl(form.getSourceUrl().trim())
                .maxArticles(form.getMaxArticles())
                .lastExecutedAt(currentSchedule.getLastExecutedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        return crawlScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-17
     * @desc 현재 시각 기준으로 크롤링 예약 실행 필요 여부를 판단합니다.
     */
    @Transactional
    public boolean isExecutionDue(LocalDateTime now) {
        CrawlSchedule schedule = getOrCreateSchedule();
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            return false;
        }

        CronExpression cronExpression = CronExpression.parse(schedule.getCronExpression());
        LocalDateTime referenceTime = schedule.getLastExecutedAt() == null
                ? now.minusMinutes(1)
                : schedule.getLastExecutedAt();

        LocalDateTime nextExecutionTime = cronExpression.next(referenceTime);
        return nextExecutionTime != null && !nextExecutionTime.isAfter(now);
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 예약 실행 완료 시 마지막 실행 시각을 갱신합니다.
     */
    @Transactional
    public void markExecuted(LocalDateTime executedAt) {
        CrawlSchedule schedule = getOrCreateSchedule();
        CrawlSchedule updatedSchedule = CrawlSchedule.builder()
                .id(schedule.getId())
                .enabled(schedule.getEnabled())
                .cronExpression(schedule.getCronExpression())
                .sourceName(schedule.getSourceName())
                .sourceUrl(schedule.getSourceUrl())
                .maxArticles(schedule.getMaxArticles())
                .lastExecutedAt(executedAt)
                .updatedAt(LocalDateTime.now())
                .build();
        crawlScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-17
     * @desc 예약 설정 입력값의 필수 항목과 범위를 검증합니다.
     */
    private void validateScheduleForm(CrawlScheduleForm form) {
        if (form.getCronExpression() == null || form.getCronExpression().isBlank()) {
            throw new IllegalArgumentException("Cron 표현식은 필수입니다.");
        }
        if (form.getSourceName() == null || form.getSourceName().isBlank()) {
            throw new IllegalArgumentException("소스 이름은 필수입니다.");
        }
        if (form.getSourceUrl() == null || form.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("소스 URL은 필수입니다.");
        }
        if (form.getMaxArticles() == null) {
            throw new IllegalArgumentException("최대 수집 건수는 필수입니다.");
        }
        if (form.getMaxArticles() < MIN_MAX_ARTICLES || form.getMaxArticles() > MAX_MAX_ARTICLES) {
            throw new IllegalArgumentException("최대 수집 건수는 " + MIN_MAX_ARTICLES + " ~ " + MAX_MAX_ARTICLES + " 사이여야 합니다.");
        }
        CronExpression.parse(form.getCronExpression().trim());
    }

    /**
     * @date 2026-04-17
     * @desc 기본 크롤링 예약 설정을 생성합니다.
     */
    private CrawlSchedule createDefaultSchedule() {
        LocalDateTime now = LocalDateTime.now();
        CrawlSchedule defaultSchedule = CrawlSchedule.builder()
                .id(DEFAULT_SCHEDULE_ID)
                .enabled(false)
                .cronExpression(DEFAULT_CRON_EXPRESSION)
                .sourceName(DEFAULT_SOURCE_NAME)
                .sourceUrl(DEFAULT_SOURCE_URL)
                .maxArticles(DEFAULT_MAX_ARTICLES)
                .lastExecutedAt(null)
                .updatedAt(now)
                .build();
        return crawlScheduleRepository.save(defaultSchedule);
    }
}
