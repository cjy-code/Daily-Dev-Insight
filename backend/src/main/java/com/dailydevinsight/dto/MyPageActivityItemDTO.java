package com.dailydevinsight.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MyPageActivityItemDTO {

    private String contentType;
    private Long contentId;
    private String title;
    private String summary;
    private String source;
    private String thumbnailUrl;
    private LocalDate publishedAt;
    private LocalDateTime activityAt;

    /**
     * @date 2026-04-20
     * @desc 콘텐츠 타입 값을 상세 페이지 URL 파라미터 형식으로 변환합니다.
     */
    public String getDetailTypePath() {
        if ("NEWS".equalsIgnoreCase(contentType)) {
            return "news";
        }
        return "knowledge";
    }
}
