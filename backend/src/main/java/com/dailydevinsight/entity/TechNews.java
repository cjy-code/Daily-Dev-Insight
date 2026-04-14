package com.dailydevinsight.entity;

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
@Table(name = "tech_news")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechNews {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "news_date", nullable = false)
    private LocalDate newsDate;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Lob
    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
