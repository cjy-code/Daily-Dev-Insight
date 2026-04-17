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
@Table(name = "crawl_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crawl_history_seq_gen")
    @SequenceGenerator(name = "crawl_history_seq_gen", sequenceName = "seq_crawl_history", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "requested_count", nullable = false)
    private Integer requestedCount;

    @Column(name = "collected_count", nullable = false)
    private Integer collectedCount;

    @Column(name = "inserted_count", nullable = false)
    private Integer insertedCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
