package com.dailydevinsight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "insight_comment")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insight_comment_seq_gen")
    @SequenceGenerator(name = "insight_comment_seq_gen", sequenceName = "seq_insight_comment", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false)
    @Lob
    private String content;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @date 2026-04-14
     * @desc 댓글을 소프트 삭제 상태로 변경합니다.
     */
    public void markDeleted() {
        isDeleted = 1;
    }

    /**
     * @date 2026-04-14
     * @desc 등록 전 기본값(삭제 여부, 시각)을 채웁니다.
     */
    @PrePersist
    public void prePersist() {
        if (isDeleted == null) {
            isDeleted = 0;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * @date 2026-04-14
     * @desc 수정 시각을 최신 시각으로 갱신합니다.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
