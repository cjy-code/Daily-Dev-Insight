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

import java.time.LocalDateTime;

@Entity
@Table(name = "insight_like")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightLike {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insight_like_seq_gen")
    @SequenceGenerator(name = "insight_like_seq_gen", sequenceName = "seq_insight_like", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * @date 2026-04-14
     * @desc 생성 시각 누락 시 현재 시각으로 보정합니다.
     */
    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
