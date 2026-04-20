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

import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_condition_preset")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlConditionPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crawl_condition_preset_seq_gen")
    @SequenceGenerator(name = "crawl_condition_preset_seq_gen", sequenceName = "seq_crawl_condition_preset", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "preset_name", nullable = false, length = 100)
    private String presetName;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "max_articles", nullable = false)
    private Integer maxArticles;

    @Column(name = "keyword_match_type", nullable = false, length = 10)
    private String keywordMatchType;

    @Column(name = "include_keywords", length = 2000)
    private String includeKeywords;

    @Column(name = "include_keyword_operators", length = 2000)
    private String includeKeywordOperators;

    @Column(name = "exclude_keywords", length = 2000)
    private String excludeKeywords;

    @Column(name = "target_domains", length = 2000)
    private String targetDomains;

    @Column(name = "connect_timeout_seconds", nullable = false)
    private Integer connectTimeoutSeconds;

    @Column(name = "read_timeout_seconds", nullable = false)
    private Integer readTimeoutSeconds;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
