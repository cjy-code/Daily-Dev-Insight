package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.GenerationPreviewRequest;
import com.dailydevinsight.admin.dto.GenerationPreviewResponse;
import com.dailydevinsight.admin.dto.GenerationSaveRequest;
import com.dailydevinsight.admin.entity.GenerationHistory;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.DailyTrendInsight;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.DailyTrendInsightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        DailyTrendInsightRepository dailyTrendInsightRepository = mock(DailyTrendInsightRepository.class);
        ObjectProvider<ImageGenerationClient> imageGenerationClientProvider = mock(ObjectProvider.class);

        DailyKnowledgeGenerationService service = new DailyKnowledgeGenerationService(
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                dailyTrendInsightRepository,
                llmGenerationClient,
                imageGenerationClientProvider
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
        DailyTrendInsightRepository dailyTrendInsightRepository = mock(DailyTrendInsightRepository.class);
        ObjectProvider<ImageGenerationClient> imageGenerationClientProvider = mock(ObjectProvider.class);

        DailyKnowledgeGenerationService service = new DailyKnowledgeGenerationService(
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                dailyTrendInsightRepository,
                llmGenerationClient,
                imageGenerationClientProvider
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
     * @date 2026-08-13
     * @desc 기준일 트렌드가 있으면 예약 지식 생성 프롬프트 말미에 트렌드 블록을 추가하는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldAppendDailyTrendContext() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        when(fixture.generationScheduleService.getOrCreateSchedule()).thenReturn(createSchedule(true));
        when(fixture.promptTemplateService.getActiveTemplate()).thenReturn(createPromptTemplate(3L));
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate))
                .thenReturn(Optional.of(createTrend(91L, targetDate)));
        stubGeneratedKnowledge(fixture, targetDate, 77L);

        fixture.service.executeScheduledGeneration(targetDate);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.llmGenerationClient).generateKnowledge(
                promptCaptor.capture(),
                eq(targetDate),
                any(),
                any(),
                any()
        );
        assertTrue(promptCaptor.getValue().contains("[오늘의 개발 트렌드 참고]"));
        assertTrue(promptCaptor.getValue().endsWith("요약: 트렌드 요약"));

        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(fixture.generationHistoryRepository).save(historyCaptor.capture());
        assertEquals(91L, historyCaptor.getValue().getUsedTrendId());
    }

    /**
     * @date 2026-08-13
     * @desc 기준일 트렌드가 없으면 기존 렌더링 프롬프트를 변경하지 않고 LLM을 호출하는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldKeepPromptWhenDailyTrendIsMissing() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        when(fixture.generationScheduleService.getOrCreateSchedule()).thenReturn(createSchedule(true));
        when(fixture.promptTemplateService.getActiveTemplate()).thenReturn(createPromptTemplate(3L));
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate)).thenReturn(Optional.empty());
        stubGeneratedKnowledge(fixture, targetDate, 77L);

        fixture.service.executeScheduledGeneration(targetDate);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.llmGenerationClient).generateKnowledge(
                promptCaptor.capture(),
                eq(targetDate),
                any(),
                any(),
                any()
        );
        assertEquals(
                "date=2026-08-13,category=Backend,tone=실무형,difficulty=중급",
                promptCaptor.getValue()
        );
    }

    /**
     * @date 2026-08-13
     * @desc 템플릿의 dailyTrend 변수가 있으면 해당 위치를 트렌드 블록으로 치환하는지 검증합니다.
     */
    @Test
    void executeScheduledGeneration_ShouldReplaceDailyTrendPlaceholder() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        PromptTemplate promptTemplate = createPromptTemplate(3L, "앞 ${dailyTrend} 뒤");
        when(fixture.generationScheduleService.getOrCreateSchedule()).thenReturn(createSchedule(true));
        when(fixture.promptTemplateService.getActiveTemplate()).thenReturn(promptTemplate);
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate))
                .thenReturn(Optional.of(createTrend(91L, targetDate)));
        stubGeneratedKnowledge(fixture, targetDate, 77L);

        fixture.service.executeScheduledGeneration(targetDate);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.llmGenerationClient).generateKnowledge(
                promptCaptor.capture(),
                eq(targetDate),
                any(),
                any(),
                any()
        );
        assertTrue(promptCaptor.getValue().startsWith("앞 [오늘의 개발 트렌드 참고]"));
        assertTrue(promptCaptor.getValue().endsWith(" 뒤"));
        assertTrue(!promptCaptor.getValue().contains("${dailyTrend}"));
    }

    /**
     * @date 2026-08-13
     * @desc 수동 미리보기 응답에 서버가 조회한 트렌드 ID와 키워드, 요약을 포함하는지 검증합니다.
     */
    @Test
    void previewManualGeneration_ShouldReturnDailyTrendMetadata() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        GenerationPreviewRequest request = createPreviewRequest(targetDate, "기본 프롬프트");
        when(fixture.dailyTrendInsightRepository.findByTrendDate(targetDate))
                .thenReturn(Optional.of(createTrend(91L, targetDate)));
        when(fixture.llmGenerationClient.generateKnowledge(any(), eq(targetDate), any(), any(), any()))
                .thenReturn(createGeneratedKnowledge());
        when(fixture.dailyKnowledgeRepository.findTopByKnowledgeDateOrderByIdDesc(targetDate))
                .thenReturn(Optional.empty());
        when(fixture.promptTemplateService.getActiveTemplate()).thenReturn(createPromptTemplate(3L));

        GenerationPreviewResponse response = fixture.service.previewManualGeneration(request);

        assertTrue(response.isSuccess());
        assertEquals(91L, response.getDailyTrendId());
        assertEquals(List.of("Java", "AI", "Cloud"), response.getDailyTrendKeywords());
        assertEquals("트렌드 요약", response.getDailyTrendSummary());
    }

    /**
     * @date 2026-08-13
     * @desc 수동 저장 시 미리보기 요청의 트렌드 ID를 재조회 없이 생성 이력에 저장하는지 검증합니다.
     */
    @Test
    void saveManualGenerationFromPreview_ShouldPersistRequestedDailyTrendIdWithoutReload() {
        TestFixture fixture = createFixture();
        LocalDate targetDate = LocalDate.of(2026, 8, 13);
        GenerationSaveRequest request = createSaveRequest(targetDate, 91L);
        when(fixture.promptTemplateService.getActiveTemplate()).thenReturn(createPromptTemplate(3L));
        when(fixture.dailyKnowledgeRepository.save(any(DailyKnowledge.class)))
                .thenReturn(createKnowledge(77L, targetDate, "생성 제목"));

        GenerationExecutionResult result = fixture.service.saveManualGenerationFromPreview(request);

        assertTrue(result.isSuccess());
        verify(fixture.dailyTrendInsightRepository, never()).findByTrendDate(any(LocalDate.class));
        ArgumentCaptor<GenerationHistory> historyCaptor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(fixture.generationHistoryRepository).save(historyCaptor.capture());
        assertEquals(91L, historyCaptor.getValue().getUsedTrendId());
    }

    /**
     * @date 2026-08-13
     * @desc 트렌드 프롬프트 테스트에 사용할 서비스와 Mock 의존성을 구성합니다.
     */
    private TestFixture createFixture() {
        PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
        GenerationScheduleService generationScheduleService = mock(GenerationScheduleService.class);
        GenerationHistoryRepository generationHistoryRepository = mock(GenerationHistoryRepository.class);
        DailyKnowledgeRepository dailyKnowledgeRepository = mock(DailyKnowledgeRepository.class);
        DailyTrendInsightRepository dailyTrendInsightRepository = mock(DailyTrendInsightRepository.class);
        LlmGenerationClient llmGenerationClient = mock(LlmGenerationClient.class);
        ObjectProvider<ImageGenerationClient> imageGenerationClientProvider = mock(ObjectProvider.class);
        DailyKnowledgeGenerationService service = new DailyKnowledgeGenerationService(
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                dailyTrendInsightRepository,
                llmGenerationClient,
                imageGenerationClientProvider
        );
        return new TestFixture(
                service,
                promptTemplateService,
                generationScheduleService,
                generationHistoryRepository,
                dailyKnowledgeRepository,
                dailyTrendInsightRepository,
                llmGenerationClient
        );
    }

    /**
     * @date 2026-08-13
     * @desc 예약 생성 성공에 필요한 LLM 응답과 일일 지식 저장 결과를 설정합니다.
     */
    private void stubGeneratedKnowledge(TestFixture fixture, LocalDate targetDate, Long knowledgeId) {
        when(fixture.llmGenerationClient.generateKnowledge(any(), eq(targetDate), any(), any(), any()))
                .thenReturn(createGeneratedKnowledge());
        when(fixture.dailyKnowledgeRepository.save(any(DailyKnowledge.class)))
                .thenReturn(createKnowledge(knowledgeId, targetDate, "신규 생성 제목"));
    }

    /**
     * @date 2026-08-13
     * @desc 테스트용 LLM 지식 생성 결과를 생성합니다.
     */
    private GeneratedKnowledgeResult createGeneratedKnowledge() {
        return GeneratedKnowledgeResult.builder()
                .title("신규 생성 제목")
                .summary("요약")
                .detail("상세")
                .build();
    }

    /**
     * @date 2026-08-13
     * @desc 테스트용 일일 개발 트렌드 엔티티를 생성합니다.
     */
    private DailyTrendInsight createTrend(Long id, LocalDate targetDate) {
        return DailyTrendInsight.builder()
                .id(id)
                .trendDate(targetDate)
                .keywords("Java,AI,Cloud")
                .summary("트렌드 요약")
                .sourceNewsCount(3)
                .visible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * @date 2026-08-13
     * @desc 테스트용 수동 미리보기 요청을 생성합니다.
     */
    private GenerationPreviewRequest createPreviewRequest(LocalDate targetDate, String promptContent) {
        GenerationPreviewRequest request = new GenerationPreviewRequest();
        request.setTargetDate(targetDate);
        request.setCategory("Backend");
        request.setTone("실무형");
        request.setDifficulty("중급");
        request.setPromptContent(promptContent);
        request.setImagePromptTemplate("");
        return request;
    }

    /**
     * @date 2026-08-13
     * @desc 테스트용 수동 생성 저장 요청을 생성합니다.
     */
    private GenerationSaveRequest createSaveRequest(LocalDate targetDate, Long dailyTrendId) {
        GenerationSaveRequest request = new GenerationSaveRequest();
        request.setTargetDate(targetDate);
        request.setCategory("Backend");
        request.setTone("실무형");
        request.setDifficulty("중급");
        request.setPromptContent("기본 프롬프트");
        request.setGeneratedTitle("생성 제목");
        request.setGeneratedSummary("생성 요약");
        request.setGeneratedDetail("생성 상세");
        request.setGeneratedImageUrl("");
        request.setDailyTrendId(dailyTrendId);
        return request;
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
        return createPromptTemplate(
                templateId,
                "date=${date},category=${category},tone=${tone},difficulty=${difficulty}"
        );
    }

    /**
     * @date 2026-08-13
     * @desc 지정 본문을 가진 예약 생성 테스트용 프롬프트 템플릿을 생성합니다.
     */
    private PromptTemplate createPromptTemplate(Long templateId, String templateContent) {
        return PromptTemplate.builder()
                .id(templateId)
                .name("default")
                .description("desc")
                .templateContent(templateContent)
                .active(true)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private record TestFixture(
            DailyKnowledgeGenerationService service,
            PromptTemplateService promptTemplateService,
            GenerationScheduleService generationScheduleService,
            GenerationHistoryRepository generationHistoryRepository,
            DailyKnowledgeRepository dailyKnowledgeRepository,
            DailyTrendInsightRepository dailyTrendInsightRepository,
            LlmGenerationClient llmGenerationClient
    ) {
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
