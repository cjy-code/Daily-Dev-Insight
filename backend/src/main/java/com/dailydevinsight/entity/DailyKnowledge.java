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
@Table(name = "daily_knowledge")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyKnowledge {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "knowledge_date", nullable = false)
    private LocalDate knowledgeDate;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "attachment_image_path", length = 500)
    private String attachmentImagePath;

    @Lob
    @Column(name = "summary", nullable = false)
    private String summary;

    @Lob
    @Column(name = "detail", nullable = false)
    private String detail;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
