package com.dailydevinsight.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generation_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationHistory {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(name = "created_knowledge_id")
    private Long createdKnowledgeId;

    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "prompt_snapshot")
    private String promptSnapshot;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
