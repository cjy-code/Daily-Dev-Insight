package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.entity.GenerationHistory;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyKnowledgeGenerationServiceTest {

    /**
     * @date 2026-04-23
     * @desc 예약 설정이 중복 비허용일 때 동일 날짜 데이터가 있으면 SKIPPED 이력으로 종료되는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldSkipWhenDuplicateIsNotAllowed() {
        PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
        GenerationScheduleService generationScheduleService = mock(GenerationScheduleService.class);
        GenerationHistoryRepository generationHistoryRepository = mock(GenerationHistoryRepository.class);
        DailyKnowledgeRepository dailyKnowledgeRepository = mock(DailyKnowledgeRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);

        DailyKnowledgeGenerationService service = new DailyKnowledgeGenerationService(
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                llmGenerationClient
        );

        LocalDate targetDate = LocalDate.of(2026, 4, 23);
        when(generationScheduleService.getOrCreateSchedule()).thenReturn(createSchedule(false));
        when(dailyKnowledgeRepository.findTopByKnowledgeDateOrderByIdDesc(targetDate))
                .thenReturn(Optional.of(createKnowledge(44L, targetDate, "기존 제목")));

        GenerationExecutionResult result = service.executeScheduledGeneration(targetDate);

        assertTrue(result.isSuccess());
        assertEquals("already_exists", result.getErrorCode());
        assertEquals(44L, result.getCreatedKnowledgeId());
        verify(llmGenerationClient, never()).generateKnowledge(any(), any(), any(), any(), any());

        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(generationHistoryRepository).save(historyCaptor.capture());
        GenerationHistory history = historyCaptor.getValue();
        assertEquals("SCHEDULED", history.getTriggerType());
        assertEquals("SKIPPED", history.getStatus());
        assertEquals(44L, history.getCreatedKnowledgeId());
    }

    /**
     * @date 2026-04-23
     * @desc 예약 설정이 중복 허용일 때 동일 날짜 데이터가 있어도 신규 생성을 수행하는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldCreateWhenDuplicateIsAllowed() {
        PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
        GenerationScheduleService generationScheduleService = mock(GenerationScheduleService.class);
        GenerationHistoryRepository generationHistoryRepository = mock(GenerationHistoryRepository.class);
        DailyKnowledgeRepository dailyKnowledgeRepository = mock(DailyKnowledgeRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);

        DailyKnowledgeGenerationService service = new DailyKnowledgeGenerationService(
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                llmGenerationClient
        );

        LocalDate targetDate = LocalDate.of(2026, 4, 23);
        when(generationScheduleService.getOrCreateSchedule()).thenReturn(createSchedule(true));
        when(promptTemplateService.getActiveTemplate()).thenReturn(createPromptTemplate(3L));
        when(llmGenerationClient.generateKnowledge(any(), eq(targetDate), any(), any(), any()))
                .thenReturn(GeneratedKnowledgeResult.builder()
                        .title("신규 생성 제목")
                        .summary("요약")
                        .detail("상세")
                        .build());
        when(dailyKnowledgeRepository.save(any(DailyKnowledge.class)))
                .thenReturn(createKnowledge(77L, targetDate, "신규 생성 제목"));

        GenerationExecutionResult result = service.executeScheduledGeneration(targetDate);

        assertTrue(result.isSuccess());
        assertEquals(77L, result.getCreatedKnowledgeId());
        verify(dailyKnowledgeRepository).findTopByKnowledgeDateOrderByIdDesc(targetDate);

        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(generationHistoryRepository).save(historyCaptor.capture());
        GenerationHistory history = historyCaptor.getValue();
        assertEquals("SCHEDULED", history.getTriggerType());
        assertEquals("SUCCESS", history.getStatus());
        assertEquals(77L, history.getCreatedKnowledgeId());
    }

    /**
     * @date 2026-04-23
     * @desc 예약 생성 테스트용 기본 스케줄 엔티티를 생성합니다.
     */
    private GenerationSchedule createSchedule(boolean allowDuplicate) {
        return GenerationSchedule.builder()
                .id(1L)
                .enabled(true)
                .allowDuplicate(allowDuplicate)
                .cronExpression("0 0 9 * * *")
                .category("Backend")
                .tone("실무형")
                .difficulty("중급")
                .lastExecutedAt(null)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * @date 2026-04-23
     * @desc 예약 생성 테스트용 프롬프트 템플릿 엔티티를 생성합니다.
     */
    private PromptTemplate createPromptTemplate(Long templateId) {
        return PromptTemplate.builder()
                .id(templateId)
                .name("default")
                .description("desc")
                .templateContent("date=${date},category=${category},tone=${tone},difficulty=${difficulty}")
                .active(true)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * @date 2026-04-23
     * @desc 예약 생성 테스트용 일일 지식 엔티티를 생성합니다.
     */
    private DailyKnowledge createKnowledge(Long id, LocalDate targetDate, String title) {
        return DailyKnowledge.builder()
                .id(id)
                .knowledgeDate(targetDate)
                .category("Backend")
                .title(title)
                .attachmentImagePath(null)
                .summary("요약")
                .detail("상세")
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
