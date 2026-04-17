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
@Table(name = "crawl_schedule")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlSchedule {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "max_articles", nullable = false)
    private Integer maxArticles;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
