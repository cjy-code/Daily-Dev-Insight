package com.dailydevinsight.entity;

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
@Table(name = "daily_trend_insight")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_trend_insight_seq_gen")
    @SequenceGenerator(name = "daily_trend_insight_seq_gen", sequenceName = "seq_daily_trend_insight", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trend_date", nullable = false, unique = true)
    private LocalDate trendDate;

    @Column(name = "keywords", nullable = false, length = 500)
    private String keywords;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "source_news_count", nullable = false)
    private Integer sourceNewsCount;

    @Column(name = "is_visible", nullable = false)
    private Boolean visible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @date 2026-08-13
     * @desc 일일 개발 트렌드 분석 결과와 출처 뉴스 수를 갱신합니다.
     */
    public void updateAnalysis(String keywords, String summary, Integer sourceNewsCount) {
        this.keywords = keywords;
        this.summary = summary;
        this.sourceNewsCount = sourceNewsCount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * @date 2026-08-13
     * @desc 사용자 화면 노출 여부를 변경합니다.
     */
    public void changeVisible(Boolean visible) {
        this.visible = visible;
        this.updatedAt = LocalDateTime.now();
    }
}
