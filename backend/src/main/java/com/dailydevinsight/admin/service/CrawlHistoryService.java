package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.entity.CrawlHistory;
import com.dailydevinsight.admin.repository.CrawlHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlHistoryService {

    private static final int MAX_SOURCE_TEXT_LENGTH = 100;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final CrawlHistoryRepository crawlHistoryRepository;

    /**
     * @date 2026-08-10
     * @desc 실행 시작 시 RUNNING 상태 히스토리를 저장합니다.
     */
    @Transactional
    public CrawlHistory recordRunning(String triggerType, LocalDate targetDate, String sourceName, int requestedCount) {
        CrawlHistory history = CrawlHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("RUNNING")
                .sourceName(limitLength(sourceName, MAX_SOURCE_TEXT_LENGTH))
                .requestedCount(requestedCount)
                .collectedCount(0)
                .insertedCount(0)
                .errorMessage(null)
                .createdAt(LocalDateTime.now())
                .build();
        return crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-08-10
     * @desc 중복 실행으로 스킵된 요청의 히스토리를 저장합니다.
     */
    @Transactional
    public void recordSkipped(String triggerType, LocalDate targetDate, String sourceName, int requestedCount, String reason) {
        CrawlHistory history = CrawlHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("SKIPPED")
                .sourceName(limitLength(sourceName, MAX_SOURCE_TEXT_LENGTH))
                .requestedCount(requestedCount)
                .collectedCount(0)
                .insertedCount(0)
                .errorMessage(limitLength(reason, MAX_ERROR_MESSAGE_LENGTH))
                .createdAt(LocalDateTime.now())
                .build();
        crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-08-10
     * @desc RUNNING 히스토리를 SUCCESS 상태로 갱신합니다.
     */
    @Transactional
    public void recordSuccess(CrawlHistory runningHistory, int collectedCount, int insertedCount) {
        CrawlHistory history = CrawlHistory.builder()
                .id(runningHistory.getId())
                .triggerType(runningHistory.getTriggerType())
                .targetDate(runningHistory.getTargetDate())
                .status("SUCCESS")
                .sourceName(runningHistory.getSourceName())
                .requestedCount(runningHistory.getRequestedCount())
                .collectedCount(collectedCount)
                .insertedCount(insertedCount)
                .errorMessage(null)
                .createdAt(runningHistory.getCreatedAt())
                .build();
        crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-08-10
     * @desc RUNNING 히스토리를 FAILED 상태로 갱신합니다.
     */
    @Transactional
    public void recordFailure(CrawlHistory runningHistory, Exception exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "원인을 확인할 수 없는 오류"
                : exception.getMessage().trim();

        CrawlHistory history = CrawlHistory.builder()
                .id(runningHistory.getId())
                .triggerType(runningHistory.getTriggerType())
                .targetDate(runningHistory.getTargetDate())
                .status("FAILED")
                .sourceName(runningHistory.getSourceName())
                .requestedCount(runningHistory.getRequestedCount())
                .collectedCount(0)
                .insertedCount(0)
                .errorMessage(limitLength(message, MAX_ERROR_MESSAGE_LENGTH))
                .createdAt(runningHistory.getCreatedAt())
                .build();
        crawlHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-17
     * @desc 최근 크롤링 이력 20건을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrawlHistory> findRecentHistory() {
        return crawlHistoryRepository.findTop20ByOrderByCreatedAtDesc();
    }

    /**
     * @date 2026-08-10
     * @desc 문자열을 지정한 최대 길이 이하로 잘라 DB 컬럼 길이를 보호합니다.
     */
    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() <= maxLength) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, maxLength);
    }
}
