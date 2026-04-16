package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.PromptTemplateForm;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private static final String DEFAULT_TEMPLATE_NAME = "Daily Knowledge Default Template";
    private static final String DEFAULT_TEMPLATE_DESCRIPTION = "Default template for admin manual and scheduled generation";
    private static final int MAX_TEMPLATE_COUNT = 10;
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
     * @date 2026-04-16
     * @desc Find active prompt template as Optional.
     */
    @Transactional(readOnly = true)
    public Optional<PromptTemplate> findActiveTemplate() {
        return promptTemplateRepository.findTopByActiveTrueAndDeletedFalseOrderByUpdatedAtDesc();
    }

    /**
     * @date 2026-04-16
     * @desc Find non-deleted templates in updated desc order.
     */
    @Transactional(readOnly = true)
    public List<PromptTemplate> findAllTemplates() {
        return promptTemplateRepository.findAllByDeletedFalseOrderByUpdatedAtDesc();
    }

    /**
     * @date 2026-04-16
     * @desc Return active template or throw when absent.
     */
    @Transactional(readOnly = true)
    public PromptTemplate getActiveTemplate() {
        return findActiveTemplate()
                .orElseThrow(() -> new IllegalStateException("No active prompt template. Please activate one and retry."));
    }

    /**
     * @date 2026-04-16
     * @desc Save prompt template as create or update.
     */
    @Transactional
    public PromptTemplate saveTemplate(PromptTemplateForm form) {
        validateTemplateForm(form);

        Long templateId = resolveTemplateId(form.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean isCreateRequest = form.getId() == null;
        boolean shouldActivate = isCreateRequest && findActiveTemplate().isEmpty();

        if (isCreateRequest && promptTemplateRepository.countByDeletedFalse() >= MAX_TEMPLATE_COUNT) {
            throw new IllegalArgumentException("Prompt templates are limited to " + MAX_TEMPLATE_COUNT + ".");
        }

        PromptTemplate savedTemplate = PromptTemplate.builder()
                .id(templateId)
                .name(form.getName().trim())
                .description(trimToNull(form.getDescription()))
                .templateContent(form.getTemplateContent().trim())
                .active(shouldActivate || isTemplateCurrentlyActive(templateId))
                .deleted(false)
                .createdAt(resolveCreatedAt(form.getId(), now))
                .updatedAt(now)
                .build();

        return promptTemplateRepository.save(savedTemplate);
    }

    /**
     * @date 2026-04-16
     * @desc Activate only target template and deactivate others.
     */
    @Transactional
    public void activateTemplate(Long templateId) {
        PromptTemplate targetTemplate = findNotDeletedTemplate(templateId);

        LocalDateTime now = LocalDateTime.now();
        List<PromptTemplate> allTemplates = promptTemplateRepository.findAllByDeletedFalseOrderByUpdatedAtDesc();
        for (PromptTemplate template : allTemplates) {
            boolean isTarget = template.getId().equals(targetTemplate.getId());
            promptTemplateRepository.save(copyTemplateWithFlags(template, isTarget, false, now));
        }
    }

    /**
     * @date 2026-04-16
     * @desc Toggle target template active flag.
     */
    @Transactional
    public void toggleTemplateActive(Long templateId) {
        PromptTemplate targetTemplate = findNotDeletedTemplate(templateId);

        if (!Boolean.TRUE.equals(targetTemplate.getActive())) {
            activateTemplate(templateId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        promptTemplateRepository.save(copyTemplateWithFlags(targetTemplate, false, false, now));
    }

    /**
     * @date 2026-04-16
     * @desc Soft delete template.
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        PromptTemplate targetTemplate = findNotDeletedTemplate(templateId);

        if (Boolean.TRUE.equals(targetTemplate.getActive())) {
            throw new IllegalArgumentException("Active template cannot be deleted. Deactivate it first.");
        }

        List<PromptTemplate> allTemplates = promptTemplateRepository.findAllByDeletedFalseOrderByUpdatedAtDesc();
        if (allTemplates.size() <= 1) {
            throw new IllegalArgumentException("At least one prompt template must remain.");
        }

        LocalDateTime now = LocalDateTime.now();
        promptTemplateRepository.save(copyTemplateWithFlags(targetTemplate, false, true, now));
    }

    /**
     * @date 2026-04-16
     * @desc Ensure default template exists when no non-deleted template remains.
     */
    @Transactional
    public void ensureDefaultTemplateExists() {
        if (promptTemplateRepository.countByDeletedFalse() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long templateId = resolveTemplateId(null);
        PromptTemplate defaultTemplate = PromptTemplate.builder()
                .id(templateId)
                .name(DEFAULT_TEMPLATE_NAME)
                .description(DEFAULT_TEMPLATE_DESCRIPTION)
                .templateContent(DEFAULT_TEMPLATE_CONTENT)
                .active(true)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        promptTemplateRepository.save(defaultTemplate);
    }

    /**
     * @date 2026-04-16
     * @desc Validate required fields of template form.
     */
    private void validateTemplateForm(PromptTemplateForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }
        if (form.getTemplateContent() == null || form.getTemplateContent().isBlank()) {
            throw new IllegalArgumentException("Template content is required.");
        }
    }

    /**
     * @date 2026-04-16
     * @desc Resolve next template id for create.
     */
    private Long resolveTemplateId(Long requestId) {
        if (requestId != null) {
            findNotDeletedTemplate(requestId);
            return requestId;
        }
        return promptTemplateRepository.findTopByOrderByIdDesc()
                .map(PromptTemplate::getId)
                .orElse(0L) + 1L;
    }

    /**
     * @date 2026-04-16
     * @desc Keep createdAt timestamp for update.
     */
    private LocalDateTime resolveCreatedAt(Long requestId, LocalDateTime now) {
        if (requestId == null) {
            return now;
        }
        return findNotDeletedTemplate(requestId).getCreatedAt();
    }

    /**
     * @date 2026-04-16
     * @desc Check whether template is currently active.
     */
    private boolean isTemplateCurrentlyActive(Long templateId) {
        return promptTemplateRepository.findByIdAndDeletedFalse(templateId)
                .map(PromptTemplate::getActive)
                .orElse(false);
    }

    /**
     * @date 2026-04-16
     * @desc Find non-deleted template by id or throw.
     */
    private PromptTemplate findNotDeletedTemplate(Long templateId) {
        return promptTemplateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Target prompt template not found."));
    }

    /**
     * @date 2026-04-16
     * @desc Build copied template with active/deleted flag changes.
     */
    private PromptTemplate copyTemplateWithFlags(PromptTemplate sourceTemplate, boolean active, boolean deleted, LocalDateTime now) {
        return PromptTemplate.builder()
                .id(sourceTemplate.getId())
                .name(sourceTemplate.getName())
                .description(sourceTemplate.getDescription())
                .templateContent(sourceTemplate.getTemplateContent())
                .active(active)
                .deleted(deleted)
                .createdAt(sourceTemplate.getCreatedAt())
                .updatedAt(now)
                .build();
    }

    /**
     * @date 2026-04-16
     * @desc Normalize blank string to null.
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
