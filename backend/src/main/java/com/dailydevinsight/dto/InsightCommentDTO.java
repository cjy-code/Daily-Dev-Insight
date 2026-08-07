package com.dailydevinsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightCommentDTO {

    private Long id;
    private Long parentCommentId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;
    private boolean deleted;
    private List<InsightCommentDTO> replies;
}
