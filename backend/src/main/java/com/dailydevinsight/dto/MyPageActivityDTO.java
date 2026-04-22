package com.dailydevinsight.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyPageActivityDTO {

    private List<MyPageActivityItemDTO> bookmarkItems;
    private List<MyPageActivityItemDTO> likeItems;

    /**
     * @date 2026-04-20
     * @desc 북마크 목록 개수를 반환합니다.
     */
    public int getBookmarkCount() {
        if (bookmarkItems == null) {
            return 0;
        }
        return bookmarkItems.size();
    }

    /**
     * @date 2026-04-20
     * @desc 좋아요 목록 개수를 반환합니다.
     */
    public int getLikeCount() {
        if (likeItems == null) {
            return 0;
        }
        return likeItems.size();
    }
}
