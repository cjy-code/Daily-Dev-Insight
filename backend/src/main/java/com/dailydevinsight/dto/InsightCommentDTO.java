package com.dailydevinsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightCommentDTO {

    private Long id;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;
}
