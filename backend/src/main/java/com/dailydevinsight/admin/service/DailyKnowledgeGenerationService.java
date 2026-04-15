package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.GenerationRequestForm;
import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.entity.GenerationHistory;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DailyKnowledgeGenerationService {

    private final PromptTemplateService promptTemplateService;
    private final GenerationScheduleService generationScheduleService;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final LlmGenerationClient llmGenerationClient;

    /**
     * @date 2026-04-15
     * @desc 관리자 수동 요청으로 일일 개발 지식 생성을 실행합니다.
     */
    @Transactional
    public GenerationExecutionResult executeManualGeneration(GenerationRequestForm form) {
        validateManualRequest(form);

        LocalDate targetDate = form.getTargetDate() == null ? LocalDate.now() : form.getTargetDate();
        return generateKnowledgeAndPersist(
                targetDate,
                normalizeRequiredValue(form.getCategory(), "Backend"),
                normalizeRequiredValue(form.getTone(), "실무형"),
                normalizeRequiredValue(form.getDifficulty(), "중급"),
                "MANUAL"
        );
    }

    /**
     * @date 2026-04-15
     * @desc 예약 스케줄 설정값으로 일일 개발 지식 생성을 실행합니다.
     */
    @Transactional
    public GenerationExecutionResult executeScheduledGeneration(LocalDate targetDate) {
        GenerationSchedule schedule = generationScheduleService.getOrCreateSchedule();

        return generateKnowledgeAndPersist(
                targetDate,
                schedule.getCategory(),
                schedule.getTone(),
                schedule.getDifficulty(),
                "SCHEDULED"
        );
    }

    /**
     * @date 2026-04-15
     * @desc 프롬프트 렌더링, LLM 호출, 지식 저장, 이력 저장을 일괄 처리합니다.
     */
    private GenerationExecutionResult generateKnowledgeAndPersist(
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty,
            String triggerType
    ) {
        PromptTemplate promptTemplate = promptTemplateService.getActiveTemplate();
        String renderedPrompt = renderPrompt(promptTemplate.getTemplateContent(), targetDate, category, tone, difficulty);

        try {
            GeneratedKnowledgeResult generatedKnowledgeResult = llmGenerationClient.generateKnowledge(
                    renderedPrompt,
                    targetDate,
                    category,
                    tone,
                    difficulty
            );

            DailyKnowledge savedKnowledge = saveDailyKnowledge(targetDate, category, generatedKnowledgeResult);
            saveSuccessHistory(triggerType, targetDate, promptTemplate, renderedPrompt, savedKnowledge);

            return GenerationExecutionResult.builder()
                    .success(true)
                    .message("생성이 완료되었습니다.")
                    .createdKnowledgeId(savedKnowledge.getId())
                    .build();
        } catch (Exception exception) {
            saveFailureHistory(triggerType, targetDate, promptTemplate, renderedPrompt, exception);
            return GenerationExecutionResult.builder()
                    .success(false)
                    .message("생성에 실패했습니다: " + exception.getMessage())
                    .createdKnowledgeId(null)
                    .build();
        }
    }

    /**
     * @date 2026-04-15
     * @desc 활성 템플릿과 입력값을 치환해 최종 프롬프트를 만듭니다.
     */
    private String renderPrompt(
            String template,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    ) {
        return template
                .replace("${date}", targetDate.toString())
                .replace("${category}", category)
                .replace("${tone}", tone)
                .replace("${difficulty}", difficulty);
    }

    /**
     * @date 2026-04-15
     * @desc 생성된 지식을 일일 개발 지식 테이블에 저장합니다.
     */
    private DailyKnowledge saveDailyKnowledge(
            LocalDate targetDate,
            String category,
            GeneratedKnowledgeResult generatedKnowledgeResult
    ) {
        Long nextKnowledgeId = dailyKnowledgeRepository.findTopByOrderByIdDesc()
                .map(DailyKnowledge::getId)
                .orElse(0L) + 1L;

        DailyKnowledge dailyKnowledge = DailyKnowledge.builder()
                .id(nextKnowledgeId)
                .knowledgeDate(targetDate)
                .category(category)
                .title(generatedKnowledgeResult.getTitle())
                .summary(generatedKnowledgeResult.getSummary())
                .detail(generatedKnowledgeResult.getDetail())
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        return dailyKnowledgeRepository.save(dailyKnowledge);
    }

    /**
     * @date 2026-04-15
     * @desc 성공 이력을 생성 이력 테이블에 저장합니다.
     */
    private void saveSuccessHistory(
            String triggerType,
            LocalDate targetDate,
            PromptTemplate promptTemplate,
            String renderedPrompt,
            DailyKnowledge savedKnowledge
    ) {
        GenerationHistory history = GenerationHistory.builder()
                .id(nextHistoryId())
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("SUCCESS")
                .promptTemplateId(promptTemplate.getId())
                .createdKnowledgeId(savedKnowledge.getId())
                .title(savedKnowledge.getTitle())
                .promptSnapshot(renderedPrompt)
                .errorMessage(null)
                .createdAt(LocalDateTime.now())
                .build();

        generationHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-15
     * @desc 실패 이력을 생성 이력 테이블에 저장합니다.
     */
    private void saveFailureHistory(
            String triggerType,
            LocalDate targetDate,
            PromptTemplate promptTemplate,
            String renderedPrompt,
            Exception exception
    ) {
        GenerationHistory history = GenerationHistory.builder()
                .id(nextHistoryId())
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("FAILED")
                .promptTemplateId(promptTemplate.getId())
                .createdKnowledgeId(null)
                .title(null)
                .promptSnapshot(renderedPrompt)
                .errorMessage(limitErrorMessage(exception.getMessage()))
                .createdAt(LocalDateTime.now())
                .build();

        generationHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-15
     * @desc 생성 이력의 다음 식별자를 계산합니다.
     */
    private Long nextHistoryId() {
        return generationHistoryRepository.findTopByOrderByIdDesc()
                .map(GenerationHistory::getId)
                .orElse(0L) + 1L;
    }

    /**
     * @date 2026-04-15
     * @desc 수동 실행 입력값을 검증합니다.
     */
    private void validateManualRequest(GenerationRequestForm form) {
        if (form == null) {
            throw new IllegalArgumentException("생성 요청 값이 없습니다.");
        }
    }

    /**
     * @date 2026-04-15
     * @desc 공백 입력값을 기본값으로 치환합니다.
     */
    private String normalizeRequiredValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * @date 2026-04-15
     * @desc 오류 메시지를 DB 컬럼 길이에 맞춰 정리합니다.
     */
    private String limitErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "알 수 없는 오류";
        }
        String normalizedErrorMessage = errorMessage.trim();
        if (normalizedErrorMessage.length() <= 1000) {
            return normalizedErrorMessage;
        }
        return normalizedErrorMessage.substring(0, 1000);
    }
}
