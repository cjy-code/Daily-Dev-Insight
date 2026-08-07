package com.dailydevinsight.service;

import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.dto.MyPageActivityDTO;
import com.dailydevinsight.dto.MyPageActivityItemDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.InsightBookmark;
import com.dailydevinsight.entity.InsightLike;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.InsightLikeRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int DEFAULT_ACTIVITY_PAGE_SIZE = 20;

    private final UserService userService;
    private final InsightBookmarkRepository insightBookmarkRepository;
    private final InsightLikeRepository insightLikeRepository;
    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final TechNewsRepository techNewsRepository;

    /**
     * @date 2026-04-20
     * @desc 로그인 사용자 정보를 조회합니다.
     */
    public User getMyProfile(String loginUserId) {
        return userService.findRequiredByUserId(loginUserId);
    }

    /**
     * @date 2026-08-06
     * @desc 북마크/좋아요 활동 목록을 각각 페이지 단위로 조회합니다.
     */
    public MyPageActivityDTO getMyActivity(String loginUserId, int bookmarkPageNumber, int likePageNumber) {
        User user = userService.findRequiredByUserId(loginUserId);

        Page<InsightBookmark> bookmarkPage = insightBookmarkRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), toPageable(bookmarkPageNumber));
        Page<InsightLike> likePage = insightLikeRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), toPageable(likePageNumber));

        return MyPageActivityDTO.builder()
                .bookmarkPage(toBookmarkActivityPage(bookmarkPage))
                .likePage(toLikeActivityPage(likePage))
                .build();
    }

    /**
     * @date 2026-08-06
     * @desc 0 미만의 요청 페이지 번호를 보정하여 고정 크기의 Pageable을 생성합니다.
     */
    private Pageable toPageable(int pageNumber) {
        return PageRequest.of(Math.max(pageNumber, 0), DEFAULT_ACTIVITY_PAGE_SIZE);
    }

    /**
     * @date 2026-04-20
     * @desc 회원정보(이름/이메일)를 수정합니다.
     */
    @Transactional
    public void updateProfile(String loginUserId, String name, String email) {
        userService.updateProfile(loginUserId, name, email);
    }

    /**
     * @date 2026-04-20
     * @desc 비밀번호를 변경합니다.
     */
    @Transactional
    public void changePassword(String loginUserId, String currentPassword, String newPassword, String newPasswordConfirm) {
        userService.changePassword(loginUserId, currentPassword, newPassword, newPasswordConfirm);
    }

    /**
     * @date 2026-04-20
     * @desc 회원 탈퇴를 처리하고 사용자 활동 데이터를 정리합니다.
     */
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true)
    public void withdraw(String loginUserId, String currentPassword) {
        User user = userService.findRequiredByUserId(loginUserId);
        insightBookmarkRepository.deleteByUserId(user.getId());
        insightLikeRepository.deleteByUserId(user.getId());
        userService.withdraw(loginUserId, currentPassword);
    }

    /**
     * @date 2026-08-06
     * @desc 북마크 엔티티 페이지를 화면 표시용 DTO 페이지로 변환합니다.
     */
    private Page<MyPageActivityItemDTO> toBookmarkActivityPage(Page<InsightBookmark> bookmarkPage) {
        List<MyPageActivityItemDTO> activityItemList = bookmarkPage.getContent().stream()
                .map(bookmark -> toActivityItem(bookmark.getContentType(), bookmark.getContentId(), bookmark.getCreatedAt()))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(activityItemList, bookmarkPage.getPageable(), bookmarkPage.getTotalElements());
    }

    /**
     * @date 2026-08-06
     * @desc 좋아요 엔티티 페이지를 화면 표시용 DTO 페이지로 변환합니다.
     */
    private Page<MyPageActivityItemDTO> toLikeActivityPage(Page<InsightLike> likePage) {
        List<MyPageActivityItemDTO> activityItemList = likePage.getContent().stream()
                .map(like -> toActivityItem(like.getContentType(), like.getContentId(), like.getCreatedAt()))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(activityItemList, likePage.getPageable(), likePage.getTotalElements());
    }

    /**
     * @date 2026-04-20
     * @desc 콘텐츠 타입에 맞춰 활동 DTO를 생성합니다.
     */
    private MyPageActivityItemDTO toActivityItem(String contentType, Long contentId, java.time.LocalDateTime activityAt) {
        if ("NEWS".equalsIgnoreCase(contentType)) {
            return techNewsRepository.findById(contentId)
                    .map(news -> toNewsActivityItem(contentType, news, activityAt))
                    .orElse(null);
        }

        return dailyKnowledgeRepository.findById(contentId)
                .map(knowledge -> toKnowledgeActivityItem(contentType, knowledge, activityAt))
                .orElse(null);
    }

    /**
     * @date 2026-04-20
     * @desc DailyKnowledge 엔티티를 활동 DTO로 변환합니다.
     */
    private MyPageActivityItemDTO toKnowledgeActivityItem(String contentType, DailyKnowledge knowledge, java.time.LocalDateTime activityAt) {
        return MyPageActivityItemDTO.builder()
                .contentType(contentType)
                .contentId(knowledge.getId())
                .title(knowledge.getTitle())
                .summary(knowledge.getSummary())
                .source(knowledge.getCategory())
                .thumbnailUrl(knowledge.getAttachmentImagePath())
                .publishedAt(knowledge.getKnowledgeDate())
                .activityAt(activityAt)
                .build();
    }

    /**
     * @date 2026-04-20
     * @desc TechNews 엔티티를 활동 DTO로 변환합니다.
     */
    private MyPageActivityItemDTO toNewsActivityItem(String contentType, TechNews news, java.time.LocalDateTime activityAt) {
        return MyPageActivityItemDTO.builder()
                .contentType(contentType)
                .contentId(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .source(news.getSource())
                .thumbnailUrl(news.getAttachmentImagePath())
                .publishedAt(news.getNewsDate())
                .activityAt(activityAt)
                .build();
    }
}
