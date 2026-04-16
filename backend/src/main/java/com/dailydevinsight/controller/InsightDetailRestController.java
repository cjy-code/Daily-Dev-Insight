package com.dailydevinsight.controller;

import com.dailydevinsight.dto.InsightCommentRequestDTO;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.dto.InsightToggleResponseDTO;
import com.dailydevinsight.service.InsightDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights/{type}/{id}")
@RequiredArgsConstructor
public class InsightDetailRestController {

    private final InsightDetailService insightDetailService;

    /**
     * @date 2026-04-13
     * @desc 상세 페이지 집계 상태를 조회합니다.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public InsightDetailResponseDTO getDetailState(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        return insightDetailService.getEngagementOnly(type, id, resolveUserId(authentication));
    }

    /**
     * @date 2026-04-13
     * @desc 좋아요를 토글합니다.
     */
    @PostMapping(value = "/likes/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    public InsightToggleResponseDTO toggleLike(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        return insightDetailService.toggleLike(type, id, resolveUserId(authentication));
    }

    /**
     * @date 2026-04-13
     * @desc 북마크를 토글합니다.
     */
    @PostMapping(value = "/bookmarks/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    public InsightToggleResponseDTO toggleBookmark(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        return insightDetailService.toggleBookmark(type, id, resolveUserId(authentication));
    }

    /**
     * @date 2026-04-13
     * @desc 댓글을 등록합니다.
     */
    @PostMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public InsightDetailResponseDTO addComment(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            @RequestBody InsightCommentRequestDTO request,
            Authentication authentication
    ) {
        return insightDetailService.addComment(
                type,
                id,
                resolveUserId(authentication),
                request.getContent(),
                request.getParentCommentId()
        );
    }

    /**
     * @date 2026-04-13
     * @desc 본인 댓글을 삭제합니다.
     */
    @DeleteMapping(value = "/comments/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public InsightDetailResponseDTO deleteComment(
            @PathVariable("type") String type,
            @PathVariable("id") Long id,
            @PathVariable("commentId") Long commentId,
            Authentication authentication
    ) {
        return insightDetailService.deleteComment(type, id, commentId, resolveUserId(authentication));
    }

    /**
     * @date 2026-04-14
     * @desc 인증 객체에서 로그인 사용자 이메일을 추출합니다.
     */
    private String resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "";
        }
        return authentication.getName();
    }
}
