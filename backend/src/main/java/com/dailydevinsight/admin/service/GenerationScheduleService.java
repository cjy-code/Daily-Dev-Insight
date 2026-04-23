package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.ScheduleForm;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.repository.GenerationScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GenerationScheduleService {

    private static final Long DEFAULT_SCHEDULE_ID = 1L;
    private static final String DEFAULT_CRON_EXPRESSION = "0 0 9 * * *";

    private final GenerationScheduleRepository generationScheduleRepository;

    /**
     * @date 2026-04-15
     * @desc 생성 스케줄 설정을 조회하고 없으면 기본 설정을 생성합니다.
     */
    @Transactional
    public GenerationSchedule getOrCreateSchedule() {
        return generationScheduleRepository.findById(DEFAULT_SCHEDULE_ID)
                .orElseGet(this::createDefaultSchedule);
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 입력값으로 생성 스케줄을 갱신합니다.
     */
    @Transactional
    public GenerationSchedule updateSchedule(ScheduleForm form) {
        validateScheduleForm(form);

        GenerationSchedule currentSchedule = getOrCreateSchedule();
        GenerationSchedule updatedSchedule = GenerationSchedule.builder()
                .id(currentSchedule.getId())
                .enabled(Boolean.TRUE.equals(form.getEnabled()))
                .allowDuplicate(Boolean.TRUE.equals(form.getAllowDuplicate()))
                .cronExpression(form.getCronExpression().trim())
                .category(form.getCategory().trim())
                .tone(form.getTone().trim())
                .difficulty(form.getDifficulty().trim())
                .lastExecutedAt(currentSchedule.getLastExecutedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        return generationScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-15
     * @desc 현재 시각 기준으로 예약 실행 필요 여부를 판단합니다.
     */
    @Transactional
    public boolean isExecutionDue(LocalDateTime now) {
        GenerationSchedule schedule = getOrCreateSchedule();
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
     * @date 2026-04-15
     * @desc 예약 작업 실행 완료 시 마지막 실행 시각을 기록합니다.
     */
    @Transactional
    public void markExecuted(LocalDateTime executedAt) {
        GenerationSchedule schedule = getOrCreateSchedule();
        GenerationSchedule updatedSchedule = GenerationSchedule.builder()
                .id(schedule.getId())
                .enabled(schedule.getEnabled())
                .allowDuplicate(schedule.getAllowDuplicate())
                .cronExpression(schedule.getCronExpression())
                .category(schedule.getCategory())
                .tone(schedule.getTone())
                .difficulty(schedule.getDifficulty())
                .lastExecutedAt(executedAt)
                .updatedAt(LocalDateTime.now())
                .build();
        generationScheduleRepository.save(updatedSchedule);
    }

    /**
     * @date 2026-04-15
     * @desc 입력된 스케줄 폼의 필수값과 cron 형식을 검증합니다.
     */
    private void validateScheduleForm(ScheduleForm form) {
        if (form.getCronExpression() == null || form.getCronExpression().isBlank()) {
            throw new IllegalArgumentException("Cron 표현식은 필수입니다.");
        }
        if (form.getCategory() == null || form.getCategory().isBlank()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        if (form.getTone() == null || form.getTone().isBlank()) {
            throw new IllegalArgumentException("톤은 필수입니다.");
        }
        if (form.getDifficulty() == null || form.getDifficulty().isBlank()) {
            throw new IllegalArgumentException("난이도는 필수입니다.");
        }
        CronExpression.parse(form.getCronExpression().trim());
    }

    /**
     * @date 2026-04-15
     * @desc 기본 예약 생성 설정을 생성합니다.
     */
    private GenerationSchedule createDefaultSchedule() {
        LocalDateTime now = LocalDateTime.now();
        GenerationSchedule defaultSchedule = GenerationSchedule.builder()
                .id(DEFAULT_SCHEDULE_ID)
                .enabled(false)
                .allowDuplicate(false)
                .cronExpression(DEFAULT_CRON_EXPRESSION)
                .category("Backend")
                .tone("실무형")
                .difficulty("중급")
                .lastExecutedAt(null)
                .updatedAt(now)
                .build();
        return generationScheduleRepository.save(defaultSchedule);
    }
}
