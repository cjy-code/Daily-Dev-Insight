package com.dailydevinsight.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_trend_generation_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendGenerationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_trend_gen_history_seq_gen")
    @SequenceGenerator(name = "daily_trend_gen_history_seq_gen", sequenceName = "seq_daily_trend_gen_history", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "source_news_count")
    private Integer sourceNewsCount;

    @Column(name = "created_trend_id")
    private Long createdTrendId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
