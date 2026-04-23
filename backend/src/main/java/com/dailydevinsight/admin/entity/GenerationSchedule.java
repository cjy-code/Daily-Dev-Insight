package com.dailydevinsight.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_schedule")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationSchedule {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "allow_duplicate", nullable = false)
    private Boolean allowDuplicate;

    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "tone", nullable = false, length = 50)
    private String tone;

    @Column(name = "difficulty", nullable = false, length = 50)
    private String difficulty;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
