package com.dailydevinsight.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class MyPageActivityDTO {

    private Page<MyPageActivityItemDTO> bookmarkPage;
    private Page<MyPageActivityItemDTO> likePage;

    /**
     * @date 2026-08-06
     * @desc 북마크 총 개수를 반환합니다.
     */
    public long getBookmarkCount() {
        return bookmarkPage == null ? 0L : bookmarkPage.getTotalElements();
    }

    /**
     * @date 2026-08-06
     * @desc 좋아요 총 개수를 반환합니다.
     */
    public long getLikeCount() {
        return likePage == null ? 0L : likePage.getTotalElements();
    }
}
