package com.dailydevinsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDetailResponseDTO {

    private String type;
    private Long id;
    private String title;
    private String summary;
    private String detail;
    private String thumbnailUrl;
    private String source;
    private String url;
    private LocalDate publishedAt;
    private long viewCount;
    private long likeCount;
    private long bookmarkCount;
    private boolean liked;
    private boolean bookmarked;
    private List<InsightCommentDTO> comments;
}
