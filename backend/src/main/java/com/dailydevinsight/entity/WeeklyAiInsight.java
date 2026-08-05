package com.dailydevinsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_ai_insight")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weekly_ai_insight_seq_gen")
    @SequenceGenerator(name = "weekly_ai_insight_seq_gen", sequenceName = "seq_weekly_ai_insight", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Lob
    @Column(name = "summary", nullable = false)
    private String summary;

    @Lob
    @Column(name = "trend_analysis", nullable = false)
    private String trendAnalysis;

    @Lob
    @Column(name = "developer_view", nullable = false)
    private String developerView;

    @Column(name = "source_news_count", nullable = false)
    private Integer sourceNewsCount;

    @Column(name = "is_visible", nullable = false)
    private Boolean visible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트 분석 결과와 출처 뉴스 수를 갱신합니다.
     */
    public void updateAnalysis(String summary, String trendAnalysis, String developerView, Integer sourceNewsCount) {
        this.summary = summary;
        this.trendAnalysis = trendAnalysis;
        this.developerView = developerView;
        this.sourceNewsCount = sourceNewsCount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * @date 2026-05-08
     * @desc 사용자 화면 노출 여부를 변경합니다.
     */
    public void changeVisible(Boolean visible) {
        this.visible = visible;
        this.updatedAt = LocalDateTime.now();
    }
}
