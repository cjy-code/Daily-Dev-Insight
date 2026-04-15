package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.PromptTemplateForm;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private static final String DEFAULT_TEMPLATE_NAME = "일일 개발 지식 기본 템플릿";
    private static final String DEFAULT_TEMPLATE_CONTENT = """
            You are a senior developer educator.
            Create one daily development knowledge article for ${date}.
            Category: ${category}
            Tone: ${tone}
            Difficulty: ${difficulty}

            Respond in this structure:
            TITLE: one clear title
            SUMMARY: concise summary within 3 lines
            DETAIL: detailed explanation with practical example and checklist
            """;

    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * @date 2026-04-15
     * @desc 최신 수정 순으로 프롬프트 템플릿 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<PromptTemplate> findAllTemplates() {
        return promptTemplateRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * @date 2026-04-15
     * @desc 활성 프롬프트 템플릿을 조회하고 없으면 기본 템플릿을 생성합니다.
     */
    @Transactional
    public PromptTemplate getActiveTemplate() {
        ensureDefaultTemplateExists();
        return promptTemplateRepository.findTopByActiveTrueOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("활성 프롬프트 템플릿이 없습니다."));
    }

    /**
     * @date 2026-04-15
     * @desc 프롬프트 템플릿을 신규 등록 또는 수정 저장합니다.
     */
    @Transactional
    public PromptTemplate saveTemplate(PromptTemplateForm form) {
        validateTemplateForm(form);

        Long templateId = resolveTemplateId(form.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean shouldActivate = form.getId() == null && promptTemplateRepository.findTopByActiveTrueOrderByUpdatedAtDesc().isEmpty();

        PromptTemplate savedTemplate = PromptTemplate.builder()
                .id(templateId)
                .name(form.getName().trim())
                .description(trimToNull(form.getDescription()))
                .templateContent(form.getTemplateContent().trim())
                .active(shouldActivate || isTemplateCurrentlyActive(templateId))
                .createdAt(resolveCreatedAt(form.getId(), now))
                .updatedAt(now)
                .build();

        return promptTemplateRepository.save(savedTemplate);
    }

    /**
     * @date 2026-04-15
     * @desc 특정 템플릿을 활성화하고 나머지 템플릿은 비활성화합니다.
     */
    @Transactional
    public void activateTemplate(Long templateId) {
        PromptTemplate targetTemplate = promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("대상 프롬프트 템플릿을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        List<PromptTemplate> allTemplates = promptTemplateRepository.findAll();
        for (PromptTemplate template : allTemplates) {
            boolean isTarget = template.getId().equals(targetTemplate.getId());
            PromptTemplate updatedTemplate = PromptTemplate.builder()
                    .id(template.getId())
                    .name(template.getName())
                    .description(template.getDescription())
                    .templateContent(template.getTemplateContent())
                    .active(isTarget)
                    .createdAt(template.getCreatedAt())
                    .updatedAt(now)
                    .build();
            promptTemplateRepository.save(updatedTemplate);
        }
    }

    /**
     * @date 2026-04-15
     * @desc 템플릿이 하나도 없을 경우 기본 템플릿을 자동 생성합니다.
     */
    @Transactional
    public void ensureDefaultTemplateExists() {
        if (!promptTemplateRepository.findAll().isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PromptTemplate defaultTemplate = PromptTemplate.builder()
                .id(1L)
                .name(DEFAULT_TEMPLATE_NAME)
                .description("예약/수동 생성에서 공통으로 사용되는 기본 템플릿")
                .templateContent(DEFAULT_TEMPLATE_CONTENT)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        promptTemplateRepository.save(defaultTemplate);
    }

    /**
     * @date 2026-04-15
     * @desc 템플릿 폼 필수 항목을 검증합니다.
     */
    private void validateTemplateForm(PromptTemplateForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("템플릿 이름은 필수입니다.");
        }
        if (form.getTemplateContent() == null || form.getTemplateContent().isBlank()) {
            throw new IllegalArgumentException("프롬프트 본문은 필수입니다.");
        }
    }

    /**
     * @date 2026-04-15
     * @desc 신규 저장 시 다음 템플릿 식별자를 계산합니다.
     */
    private Long resolveTemplateId(Long requestId) {
        if (requestId != null) {
            return requestId;
        }
        return promptTemplateRepository.findTopByOrderByIdDesc()
                .map(PromptTemplate::getId)
                .orElse(0L) + 1L;
    }

    /**
     * @date 2026-04-15
     * @desc 수정 저장 시 기존 생성 시각을 유지합니다.
     */
    private LocalDateTime resolveCreatedAt(Long requestId, LocalDateTime now) {
        if (requestId == null) {
            return now;
        }
        return promptTemplateRepository.findById(requestId)
                .map(PromptTemplate::getCreatedAt)
                .orElse(now);
    }

    /**
     * @date 2026-04-15
     * @desc 대상 템플릿의 현재 활성 상태를 조회합니다.
     */
    private boolean isTemplateCurrentlyActive(Long templateId) {
        return promptTemplateRepository.findById(templateId)
                .map(PromptTemplate::getActive)
                .orElse(false);
    }

    /**
     * @date 2026-04-15
     * @desc 공백 문자열을 null로 정규화합니다.
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
