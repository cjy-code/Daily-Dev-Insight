package com.dailydevinsight.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledGenerationExecutor {

    private final GenerationScheduleService generationScheduleService;
    private final DailyKnowledgeGenerationService dailyKnowledgeGenerationService;

    /**
     * @date 2026-04-15
     * @desc 매 분마다 예약 조건을 확인하고 일일 개발 지식 생성을 실행합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeScheduledGeneration() {
        LocalDateTime now = LocalDateTime.now();
        if (!generationScheduleService.isExecutionDue(now)) {
            return;
        }

        dailyKnowledgeGenerationService.executeScheduledGeneration(LocalDate.now());
        generationScheduleService.markExecuted(now);
        log.info("Reserved generation executed at {}", now);
    }
}
