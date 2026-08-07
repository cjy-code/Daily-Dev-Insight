package com.dailydevinsight.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.GenerationImageRefreshRequest;
import com.dailydevinsight.admin.dto.GenerationImageRefreshResponse;
import com.dailydevinsight.admin.dto.GenerationPreviewRequest;
import com.dailydevinsight.admin.dto.GenerationPreviewResponse;
import com.dailydevinsight.admin.dto.GenerationRequestForm;
import com.dailydevinsight.admin.dto.GenerationSaveRequest;
import com.dailydevinsight.admin.entity.GenerationHistory;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DailyKnowledgeGenerationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_MANUAL_TEXT_LENGTH = 50;
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final String ERROR_CODE_ALREADY_EXISTS = "already_exists";

    private final PromptTemplateService promptTemplateService;
    private final GenerationScheduleService generationScheduleService;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final LlmGenerationClient llmGenerationClient;
    private final ObjectProvider<ImageGenerationClient> imageGenerationClientProvider;

    /**
     * @date 2026-04-16
     * @desc 관리자 수동 요청으로 일일 개발 지식 생성을 실행합니다.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true)
    })
    public GenerationExecutionResult executeManualGeneration(GenerationRequestForm form) {
        validateManualRequest(form);

        return generateKnowledgeAndPersist(
                form.getTargetDate(),
                normalizeRequiredValue(form.getCategory(), "Backend"),
                normalizeRequiredValue(form.getTone(), "실무형"),
                normalizeRequiredValue(form.getDifficulty(), "중급"),
                "MANUAL",
                true
        );
    }

    /**
     * @date 2026-04-16
     * @desc 예약 스케줄 설정값으로 일일 개발 지식 생성을 실행합니다.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true)
    })
    public GenerationExecutionResult executeScheduledGeneration(LocalDate targetDate) {
        GenerationSchedule schedule = generationScheduleService.getOrCreateSchedule();
        if (!Boolean.TRUE.equals(schedule.getAllowDuplicate())) {
            DailyKnowledge existingKnowledge = dailyKnowledgeRepository
                    .findTopByKnowledgeDateOrderByIdDesc(targetDate)
                    .orElse(null);
            if (existingKnowledge != null) {
                saveSkippedHistory(
                        "SCHEDULED",
                        targetDate,
                        existingKnowledge.getId(),
                        ERROR_CODE_ALREADY_EXISTS,
                        "동일 대상일 데이터가 이미 존재하여 예약 생성을 건너뜁니다."
                );
                return GenerationExecutionResult.builder()
                        .success(true)
                        .errorCode(ERROR_CODE_ALREADY_EXISTS)
                        .message("동일 대상일 데이터가 이미 존재하여 예약 생성을 건너뜁니다.")
                        .createdKnowledgeId(existingKnowledge.getId())
                        .build();
            }
        }

        return generateKnowledgeAndPersist(
                targetDate,
                schedule.getCategory(),
                schedule.getTone(),
                schedule.getDifficulty(),
                "SCHEDULED",
                true
        );
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창에서 사용할 렌더링 프롬프트를 생성합니다.
     */
    public String buildRenderedPromptForManual(LocalDate targetDate, String category, String tone, String difficulty) {
        PromptTemplate activeTemplate = promptTemplateService.getActiveTemplate();
        return renderPrompt(activeTemplate.getTemplateContent(), targetDate, category, tone, difficulty);
    }

    /**
     * @date 2026-04-24
     * @desc 수동 생성 확인 창에서 기본으로 노출할 이미지 프롬프트 템플릿을 반환합니다.
     */
    public String buildImagePromptTemplateForManual(PromptTemplate activeTemplate) {
        if (activeTemplate == null) {
            return "";
        }
        TemplateImageSettings imageSettings = resolveTemplateImageSettings(activeTemplate.getTemplateContent());
        return defaultIfBlank(imageSettings.promptTemplate(), "");
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창의 LLM 미리보기 결과를 생성하고 이전 결과를 함께 반환합니다.
     */
    public GenerationPreviewResponse previewManualGeneration(GenerationPreviewRequest request) {
        validatePreviewRequest(request);

        try {
            GeneratedKnowledgeResult generatedKnowledge = llmGenerationClient.generateKnowledge(
                    request.getPromptContent(),
                    request.getTargetDate(),
                    request.getCategory(),
                    request.getTone(),
                    request.getDifficulty()
            );
            DailyKnowledge previousKnowledge = dailyKnowledgeRepository
                    .findTopByKnowledgeDateOrderByIdDesc(request.getTargetDate())
                    .orElse(null);
            PromptTemplate activeTemplate = promptTemplateService.getActiveTemplate();
            TemplateImageSettings imageSettings = resolveTemplateImageSettings(activeTemplate.getTemplateContent());
            String generatedImageUrl = tryGeneratePreviewImage(
                    imageSettings,
                    generatedKnowledge,
                    request.getTargetDate(),
                    request.getCategory(),
                    request.getImagePromptTemplate()
            );

            return GenerationPreviewResponse.builder()
                    .success(true)
                    .errorCode(null)
                    .message("LLM 결과를 생성했습니다.")
                    .generatedTitle(defaultIfBlank(generatedKnowledge.getTitle(), "제목 없음"))
                    .generatedSummary(defaultIfBlank(generatedKnowledge.getSummary(), "요약 정보가 없습니다."))
                    .generatedDetail(defaultIfBlank(generatedKnowledge.getDetail(), "상세 내용이 없습니다."))
                    .generatedImageUrl(generatedImageUrl)
                    .hasPreviousResult(previousKnowledge != null)
                    .previousTitle(previousKnowledge == null ? "" : defaultIfBlank(previousKnowledge.getTitle(), ""))
                    .previousSummary(previousKnowledge == null ? "" : defaultIfBlank(previousKnowledge.getSummary(), ""))
                    .previousDetail(previousKnowledge == null ? "" : defaultIfBlank(previousKnowledge.getDetail(), ""))
                    .previousImageUrl(previousKnowledge == null ? "" : defaultIfBlank(previousKnowledge.getAttachmentImagePath(), ""))
                    .build();
        } catch (LlmClientException llmClientException) {
            return GenerationPreviewResponse.builder()
                    .success(false)
                    .errorCode(normalizeErrorCode(llmClientException.getErrorCode()))
                    .message(llmClientException.getUserMessage())
                    .generatedTitle("")
                    .generatedSummary("")
                    .generatedDetail("")
                    .generatedImageUrl("")
                    .hasPreviousResult(false)
                    .previousTitle("")
                    .previousSummary("")
                    .previousDetail("")
                    .previousImageUrl("")
                    .build();
        } catch (Exception exception) {
            return GenerationPreviewResponse.builder()
                    .success(false)
                    .errorCode("unexpected_error")
                    .message("LLM 생성에 실패했습니다: " + exception.getMessage())
                    .generatedTitle("")
                    .generatedSummary("")
                    .generatedDetail("")
                    .generatedImageUrl("")
                    .hasPreviousResult(false)
                    .previousTitle("")
                    .previousSummary("")
                    .previousDetail("")
                    .previousImageUrl("")
                    .build();
        }
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창에서 확인된 LLM 결과를 DB에 저장하고 이력을 남깁니다.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true)
    })
    public GenerationExecutionResult saveManualGenerationFromPreview(GenerationSaveRequest request) {
        validateSaveRequest(request);
        PromptTemplate promptTemplate = promptTemplateService.getActiveTemplate();

        try {
            GeneratedKnowledgeResult generatedKnowledgeResult = GeneratedKnowledgeResult.builder()
                    .title(request.getGeneratedTitle())
                    .summary(request.getGeneratedSummary())
                    .detail(request.getGeneratedDetail())
                    .build();

            DailyKnowledge savedKnowledge = saveDailyKnowledge(
                    request.getTargetDate(),
                    request.getCategory(),
                    generatedKnowledgeResult,
                    resolveGeneratedImagePath(request.getGeneratedImageUrl()),
                    true
            );
            saveSuccessHistory("MANUAL", request.getTargetDate(), promptTemplate, request.getPromptContent(), savedKnowledge);

            return GenerationExecutionResult.builder()
                    .success(true)
                    .errorCode(null)
                    .message("생성이 완료되었습니다.")
                    .createdKnowledgeId(savedKnowledge.getId())
                    .build();
        } catch (Exception exception) {
            saveFailureHistory("MANUAL", request.getTargetDate(), promptTemplate, request.getPromptContent(), exception);
            return GenerationExecutionResult.builder()
                    .success(false)
                    .errorCode("unexpected_error")
                    .message("저장에 실패했습니다: " + exception.getMessage())
                    .createdKnowledgeId(null)
                    .build();
        }
    }

    /**
     * @date 2026-04-16
     * @desc 프롬프트 렌더링, LLM 호출, 지식 저장, 이력 저장을 일괄 처리합니다.
     */
    private GenerationExecutionResult generateKnowledgeAndPersist(
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty,
            String triggerType,
            boolean updateExistingKnowledge
    ) {
        PromptTemplate promptTemplate = promptTemplateService.getActiveTemplate();
        // LLM 호출 전에 선택된 템플릿과 요청 필드를 결합해 최종 프롬프트를 생성합니다.
        String renderedPrompt = renderPrompt(promptTemplate.getTemplateContent(), targetDate, category, tone, difficulty);

        try {
            // LLM 연동 경계 지점입니다. 운영에서는 실제 외부 API 구현체가 주입되어 호출됩니다.
            GeneratedKnowledgeResult generatedKnowledgeResult = llmGenerationClient.generateKnowledge(
                    renderedPrompt,
                    targetDate,
                    category,
                    tone,
                    difficulty
            );
            TemplateImageSettings imageSettings = resolveTemplateImageSettings(promptTemplate.getTemplateContent());
            String generatedImagePath = resolveGeneratedImagePath(
                    tryGeneratePreviewImage(imageSettings, generatedKnowledgeResult, targetDate, category, "")
            );

            DailyKnowledge savedKnowledge = saveDailyKnowledge(
                    targetDate,
                    category,
                    generatedKnowledgeResult,
                    generatedImagePath,
                    updateExistingKnowledge
            );
            saveSuccessHistory(triggerType, targetDate, promptTemplate, renderedPrompt, savedKnowledge);

            return GenerationExecutionResult.builder()
                    .success(true)
                    .errorCode(null)
                    .message("생성이 완료되었습니다.")
                    .createdKnowledgeId(savedKnowledge.getId())
                    .build();
        } catch (LlmClientException llmClientException) {
            // LLM 제공자 예외는 코드/메시지를 표준화해 화면과 이력에 함께 남깁니다.
            saveFailureHistory(triggerType, targetDate, promptTemplate, renderedPrompt, llmClientException);
            return GenerationExecutionResult.builder()
                    .success(false)
                    .errorCode(llmClientException.getErrorCode())
                    .message(llmClientException.getUserMessage())
                    .createdKnowledgeId(null)
                    .build();
        } catch (Exception exception) {
            // LLM 호출 또는 저장 실패 시에도 이력을 남겨 감사/추적이 가능하도록 처리합니다.
            saveFailureHistory(triggerType, targetDate, promptTemplate, renderedPrompt, exception);
            return GenerationExecutionResult.builder()
                    .success(false)
                    .errorCode("unexpected_error")
                    .message("생성에 실패했습니다: " + exception.getMessage())
                    .createdKnowledgeId(null)
                    .build();
        }
    }

    /**
     * @date 2026-04-16
     * @desc 활성 템플릿과 입력값을 치환해 최종 프롬프트를 만듭니다.
     */
    private String renderPrompt(
            String template,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    ) {
        String promptTemplate = resolvePromptTemplateContent(template);
        return promptTemplate
                .replace("${date}", targetDate.toString())
                .replace("${category}", category)
                .replace("${tone}", tone)
                .replace("${difficulty}", difficulty);
    }

    /**
     * @date 2026-04-24
     * @desc 템플릿 본문에서 실제 프롬프트 본문 문자열을 추출합니다.
     */
    private String resolvePromptTemplateContent(String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            return "";
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(templateContent);
            JsonNode promptTemplateNode = rootNode.path("promptTemplate");
            if (promptTemplateNode.isTextual()) {
                return promptTemplateNode.asText("");
            }
        } catch (Exception ignoredException) {
            // 기존 평문 템플릿과의 호환을 위해 JSON 파싱 실패 시 원문을 그대로 사용합니다.
        }
        return templateContent;
    }

    /**
     * @date 2026-04-24
     * @desc 템플릿 본문에서 이미지 생성 설정값을 추출합니다.
     */
    private TemplateImageSettings resolveTemplateImageSettings(String templateContent) {
        TemplateImageSettings defaultSettings = new TemplateImageSettings(false, "medium", 1200, "");
        if (templateContent == null || templateContent.isBlank()) {
            return defaultSettings;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(templateContent);
            JsonNode imageSettingsNode = rootNode.path("imageSettings");
            if (!imageSettingsNode.isObject()) {
                return defaultSettings;
            }
            boolean enabled = imageSettingsNode.path("enabled").asBoolean(false);
            String quality = imageSettingsNode.path("quality").asText("medium");
            int maxTokens = imageSettingsNode.path("maxTokens").asInt(1200);
            String promptTemplate = imageSettingsNode.path("promptTemplate").asText("");
            return new TemplateImageSettings(enabled, quality, maxTokens, promptTemplate);
        } catch (Exception ignoredException) {
            return defaultSettings;
        }
    }

    /**
     * @date 2026-04-24
     * @desc 수동 생성 확인 화면에서 LLM 결과 이미지 미리보기를 재생성합니다.
     */
    public GenerationImageRefreshResponse refreshPreviewImage(GenerationImageRefreshRequest request) {
        if (request == null || request.getTargetDate() == null) {
            return GenerationImageRefreshResponse.builder()
                    .success(false)
                    .message("대상 날짜 정보가 없어 이미지를 새로고침할 수 없습니다.")
                    .imageUrl("")
                    .build();
        }
        if (request.getGeneratedTitle() == null || request.getGeneratedTitle().isBlank()) {
            return GenerationImageRefreshResponse.builder()
                    .success(false)
                    .message("이미지 생성에 사용할 제목이 없습니다. 먼저 LLM 생성을 실행해주세요.")
                    .imageUrl("")
                    .build();
        }

        PromptTemplate activeTemplate = promptTemplateService.findActiveTemplate().orElse(null);
        if (activeTemplate == null) {
            return GenerationImageRefreshResponse.builder()
                    .success(false)
                    .message("활성 프롬프트 템플릿이 없어 이미지를 새로고침할 수 없습니다.")
                    .imageUrl("")
                    .build();
        }

        TemplateImageSettings imageSettings = resolveTemplateImageSettings(activeTemplate.getTemplateContent());
        if (!imageSettings.enabled()) {
            return GenerationImageRefreshResponse.builder()
                    .success(false)
                    .message("현재 템플릿에서 이미지 생성이 비활성화되어 있습니다.")
                    .imageUrl("")
                    .build();
        }

        GeneratedKnowledgeResult generatedKnowledgeResult = GeneratedKnowledgeResult.builder()
                .title(defaultIfBlank(request.getGeneratedTitle(), ""))
                .summary(defaultIfBlank(request.getGeneratedSummary(), ""))
                .detail(defaultIfBlank(request.getGeneratedDetail(), ""))
                .build();

        String generatedImageUrl = tryGeneratePreviewImage(
                imageSettings,
                generatedKnowledgeResult,
                request.getTargetDate(),
                request.getCategory(),
                request.getImagePromptTemplate()
        );
        if (generatedImageUrl.isBlank()) {
            return GenerationImageRefreshResponse.builder()
                    .success(false)
                    .message("이미지 생성에 실패했습니다. API 키/설정값을 확인해주세요.")
                    .imageUrl("")
                    .build();
        }

        return GenerationImageRefreshResponse.builder()
                .success(true)
                .message("이미지를 새로고침했습니다.")
                .imageUrl(generatedImageUrl)
                .build();
    }

    /**
     * @date 2026-04-24
     * @desc 이미지 생성 설정과 LLM 결과를 기반으로 미리보기 이미지 생성 URL을 반환합니다.
     */
    private String tryGeneratePreviewImage(
            TemplateImageSettings imageSettings,
            GeneratedKnowledgeResult generatedKnowledge,
            LocalDate targetDate,
            String category,
            String overridePromptTemplate
    ) {
        if (imageSettings == null || !imageSettings.enabled()) {
            return "";
        }
        if (generatedKnowledge == null || targetDate == null) {
            return "";
        }

        ImageGenerationClient imageGenerationClient = imageGenerationClientProvider.getIfAvailable();
        if (imageGenerationClient == null) {
            return "";
        }

        String imagePromptTemplate = defaultIfBlank(overridePromptTemplate, imageSettings.promptTemplate());
        String imagePrompt = resolveImagePrompt(imagePromptTemplate, generatedKnowledge, targetDate, category);
        if (imagePrompt.isBlank()) {
            return "";
        }
        return defaultIfBlank(
                imageGenerationClient.generateAndStoreImage(
                        imagePrompt,
                        targetDate,
                        imageSettings.quality(),
                        imageSettings.maxTokens()
                ),
                ""
        );
    }

    /**
     * @date 2026-04-24
     * @desc 이미지 프롬프트 템플릿에 동적 값을 치환해 최종 이미지 프롬프트를 생성합니다.
     */
    private String resolveImagePrompt(
            String promptTemplate,
            GeneratedKnowledgeResult generatedKnowledge,
            LocalDate targetDate,
            String category
    ) {
        String title = defaultIfBlank(generatedKnowledge.getTitle(), "");
        String summary = defaultIfBlank(generatedKnowledge.getSummary(), "");
        String defaultPrompt = "기술 아티클 썸네일 이미지. 제목: " + title + ", 요약: " + summary;
        if (promptTemplate == null || promptTemplate.isBlank()) {
            return defaultPrompt;
        }
        return promptTemplate
                .replace("${title}", title)
                .replace("${summary}", summary)
                .replace("${category}", defaultIfBlank(category, ""))
                .replace("${date}", targetDate == null ? "" : targetDate.toString())
                .trim();
    }

    /**
     * @date 2026-04-16
     * @desc 생성된 지식을 일일 개발 지식 테이블에 저장합니다.
     */
    private DailyKnowledge saveDailyKnowledge(
            LocalDate targetDate,
            String category,
            GeneratedKnowledgeResult generatedKnowledgeResult,
            String generatedImagePath,
            boolean updateExistingKnowledge
    ) {
        DailyKnowledge existingKnowledge = null;
        if (updateExistingKnowledge) {
            existingKnowledge = dailyKnowledgeRepository
                    .findTopByKnowledgeDateOrderByIdDesc(targetDate)
                    .orElse(null);
        }

        DailyKnowledge dailyKnowledge = DailyKnowledge.builder()
                .id(existingKnowledge == null ? null : existingKnowledge.getId())
                .knowledgeDate(targetDate)
                .category(limitLength(category, MAX_CATEGORY_LENGTH))
                .title(limitLength(defaultIfBlank(generatedKnowledgeResult.getTitle(), "제목 없음"), MAX_TITLE_LENGTH))
                .summary(defaultIfBlank(generatedKnowledgeResult.getSummary(), "요약 정보가 없습니다."))
                .detail(defaultIfBlank(generatedKnowledgeResult.getDetail(), "상세 내용이 없습니다."))
                .viewCount(existingKnowledge == null ? 0L : existingKnowledge.getViewCount())
                .createdAt(existingKnowledge == null ? LocalDateTime.now() : existingKnowledge.getCreatedAt())
                .attachmentImagePath(resolveAttachmentImagePath(generatedImagePath, existingKnowledge))
                .build();

        return dailyKnowledgeRepository.save(dailyKnowledge);
    }

    /**
     * @date 2026-04-24
     * @desc 생성 미리보기 이미지 URL에서 저장 가능한 업로드 경로만 추출합니다.
     */
    private String resolveGeneratedImagePath(String generatedImageUrl) {
        if (generatedImageUrl == null || generatedImageUrl.isBlank()) {
            return "";
        }
        String normalizedImageUrl = generatedImageUrl.trim();
        try {
            if (normalizedImageUrl.startsWith("http://") || normalizedImageUrl.startsWith("https://")) {
                normalizedImageUrl = URI.create(normalizedImageUrl).getPath();
            }
        } catch (Exception ignoredException) {
            return "";
        }
        return normalizeUploadPath(normalizedImageUrl);
    }

    /**
     * @date 2026-04-24
     * @desc 절대/상대 경로를 DB 저장 형식(uploads/...)으로 정규화합니다.
     */
    private String normalizeUploadPath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        String normalizedPath = imagePath.trim().replace('\\', '/');
        while (normalizedPath.startsWith("./")) {
            normalizedPath = normalizedPath.substring(2);
        }
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (normalizedPath.startsWith("backend/")) {
            normalizedPath = normalizedPath.substring("backend/".length());
        }
        if (!normalizedPath.startsWith("uploads/")) {
            return "";
        }
        return "/" + normalizedPath;
    }

    /**
     * @date 2026-04-24
     * @desc 신규 이미지 경로가 있으면 우선 적용하고, 없으면 기존 첨부 이미지를 유지합니다.
     */
    private String resolveAttachmentImagePath(String generatedImagePath, DailyKnowledge existingKnowledge) {
        if (generatedImagePath != null && !generatedImagePath.isBlank()) {
            return generatedImagePath;
        }
        if (existingKnowledge == null) {
            return null;
        }
        String existingImagePath = normalizeUploadPath(existingKnowledge.getAttachmentImagePath());
        if (!existingImagePath.isBlank()) {
            return existingImagePath;
        }
        return existingKnowledge.getAttachmentImagePath();
    }

    /**
     * @date 2026-04-16
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
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("SUCCESS")
                .promptTemplateId(promptTemplate.getId())
                .createdKnowledgeId(savedKnowledge.getId())
                .title(limitLength(savedKnowledge.getTitle(), MAX_TITLE_LENGTH))
                .promptSnapshot(renderedPrompt)
                .errorMessage(null)
                .createdAt(LocalDateTime.now())
                .build();

        generationHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-16
     * @desc 실패 이력을 생성 이력 테이블에 저장합니다.
     */
    /**
     * @date 2026-04-23
     * @desc 예약 생성 스킵 사유를 생성 이력 테이블에 저장합니다.
     */
    private void saveSkippedHistory(
            String triggerType,
            LocalDate targetDate,
            Long existingKnowledgeId,
            String errorCode,
            String reason
    ) {
        GenerationHistory history = GenerationHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("SKIPPED")
                .promptTemplateId(null)
                .createdKnowledgeId(existingKnowledgeId)
                .title(null)
                .promptSnapshot(null)
                .errorMessage(limitErrorMessage("[" + errorCode + "] " + reason))
                .createdAt(LocalDateTime.now())
                .build();

        generationHistoryRepository.save(history);
    }

    private void saveFailureHistory(
            String triggerType,
            LocalDate targetDate,
            PromptTemplate promptTemplate,
            String renderedPrompt,
            Exception exception
    ) {
        String errorCode = resolveErrorCode(exception);
        String errorMessage = resolveHistoryErrorMessage(exception);
        GenerationHistory history = GenerationHistory.builder()
                .triggerType(triggerType)
                .targetDate(targetDate)
                .status("FAILED")
                .promptTemplateId(promptTemplate.getId())
                .createdKnowledgeId(null)
                .title(null)
                .promptSnapshot(renderedPrompt)
                .errorMessage(limitErrorMessage("[" + errorCode + "] " + errorMessage))
                .createdAt(LocalDateTime.now())
                .build();

        generationHistoryRepository.save(history);
    }

    /**
     * @date 2026-04-16
     * @desc 예외 타입에 따라 표준 에러 코드를 반환합니다.
     */
    private String resolveErrorCode(Exception exception) {
        if (exception instanceof LlmClientException llmClientException) {
            return normalizeErrorCode(llmClientException.getErrorCode());
        }
        return "unexpected_error";
    }

    /**
     * @date 2026-04-16
     * @desc 이력 저장용 에러 메시지를 예외 타입에 맞게 반환합니다.
     */
    private String resolveHistoryErrorMessage(Exception exception) {
        if (exception instanceof LlmClientException llmClientException) {
            if (llmClientException.getMessage() == null || llmClientException.getMessage().isBlank()) {
                return llmClientException.getUserMessage();
            }
            return llmClientException.getMessage();
        }
        return exception.getMessage();
    }

    /**
     * @date 2026-04-16
     * @desc 공백 코드 방지를 위해 에러 코드를 정규화합니다.
     */
    private String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "unknown_error";
        }
        return errorCode.trim();
    }

    /**
     * @date 2026-04-24
     * @desc 템플릿에 저장된 이미지 생성 설정값을 전달하기 위한 내부 구조체입니다.
     */
    private record TemplateImageSettings(
            boolean enabled,
            String quality,
            int maxTokens,
            String promptTemplate
    ) {
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 요청값을 검증합니다.
     */
    private void validateManualRequest(GenerationRequestForm form) {
        if (form == null) {
            throw new IllegalArgumentException("생성 요청 값이 없습니다.");
        }
        if (form.getTargetDate() == null) {
            throw new IllegalArgumentException("대상 날짜는 필수입니다.");
        }
        validateTextInput(form.getCategory(), "카테고리");
        validateTextInput(form.getTone(), "톤");
        validateTextInput(form.getDifficulty(), "난이도");
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 문자열 입력값을 검증합니다.
     */
    private void validateTextInput(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값은 필수입니다.");
        }
        if (value.trim().length() > MAX_MANUAL_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " 값은 최대 " + MAX_MANUAL_TEXT_LENGTH + "자까지 입력할 수 있습니다.");
        }
    }

    /**
     * @date 2026-04-16
     * @desc 공백 입력값을 기본값으로 치환합니다.
     */
    private String normalizeRequiredValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * @date 2026-04-16
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

    /**
     * @date 2026-04-16
     * @desc 공백 문자열이면 기본값을 반환하고 아니면 trim 값을 반환합니다.
     */
    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * @date 2026-04-16
     * @desc 문자열을 최대 길이로 잘라 DB 컬럼 제약을 초과하지 않도록 보정합니다.
     */
    private String limitLength(String value, int maxLength) {
        String normalizedValue = defaultIfBlank(value, "");
        if (normalizedValue.length() <= maxLength) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, maxLength);
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창의 미리보기 요청값을 검증합니다.
     */
    private void validatePreviewRequest(GenerationPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("미리보기 요청 값이 없습니다.");
        }
        if (request.getTargetDate() == null) {
            throw new IllegalArgumentException("대상 날짜는 필수입니다.");
        }
        validateTextInput(request.getCategory(), "카테고리");
        validateTextInput(request.getTone(), "톤");
        validateTextInput(request.getDifficulty(), "난이도");
        if (request.getPromptContent() == null || request.getPromptContent().isBlank()) {
            throw new IllegalArgumentException("프롬프트 본문은 필수입니다.");
        }
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창의 최종 저장 요청값을 검증합니다.
     */
    private void validateSaveRequest(GenerationSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("저장 요청 값이 없습니다.");
        }
        if (request.getTargetDate() == null) {
            throw new IllegalArgumentException("대상 날짜는 필수입니다.");
        }
        validateTextInput(request.getCategory(), "카테고리");
        validateTextInput(request.getTone(), "톤");
        validateTextInput(request.getDifficulty(), "난이도");
        if (request.getPromptContent() == null || request.getPromptContent().isBlank()) {
            throw new IllegalArgumentException("프롬프트 본문은 필수입니다.");
        }
        if (request.getGeneratedTitle() == null || request.getGeneratedTitle().isBlank()) {
            throw new IllegalArgumentException("LLM 결과 제목이 없습니다. 먼저 결과를 생성해주세요.");
        }
        if (request.getGeneratedSummary() == null || request.getGeneratedSummary().isBlank()) {
            throw new IllegalArgumentException("LLM 결과 요약이 없습니다. 먼저 결과를 생성해주세요.");
        }
        if (request.getGeneratedDetail() == null || request.getGeneratedDetail().isBlank()) {
            throw new IllegalArgumentException("LLM 결과 본문이 없습니다. 먼저 결과를 생성해주세요.");
        }
    }
}
