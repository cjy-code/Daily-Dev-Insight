package com.dailydevinsight.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int DEFAULT_ACTIVITY_LIMIT = 30;

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
     * @date 2026-04-20
     * @desc 북마크/좋아요 활동 목록을 조회합니다.
     */
    public MyPageActivityDTO getMyActivity(String loginUserId) {
        User user = userService.findRequiredByUserId(loginUserId);
        PageRequest activityPage = PageRequest.of(0, DEFAULT_ACTIVITY_LIMIT);

        List<InsightBookmark> bookmarkList = insightBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), activityPage);
        List<InsightLike> likeList = insightLikeRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), activityPage);

        return MyPageActivityDTO.builder()
                .bookmarkItems(toBookmarkActivityItems(bookmarkList))
                .likeItems(toLikeActivityItems(likeList))
                .build();
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
    public void withdraw(String loginUserId, String currentPassword) {
        User user = userService.findRequiredByUserId(loginUserId);
        insightBookmarkRepository.deleteByUserId(user.getId());
        insightLikeRepository.deleteByUserId(user.getId());
        userService.withdraw(loginUserId, currentPassword);
    }

    /**
     * @date 2026-04-20
     * @desc 북마크 엔티티 목록을 화면 표시용 DTO 목록으로 변환합니다.
     */
    private List<MyPageActivityItemDTO> toBookmarkActivityItems(List<InsightBookmark> bookmarkList) {
        if (bookmarkList == null || bookmarkList.isEmpty()) {
            return Collections.emptyList();
        }

        List<MyPageActivityItemDTO> activityItemList = new ArrayList<>();
        for (InsightBookmark bookmark : bookmarkList) {
            MyPageActivityItemDTO activityItem = toActivityItem(
                    bookmark.getContentType(),
                    bookmark.getContentId(),
                    bookmark.getCreatedAt()
            );
            if (activityItem != null) {
                activityItemList.add(activityItem);
            }
        }
        return activityItemList;
    }

    /**
     * @date 2026-04-20
     * @desc 좋아요 엔티티 목록을 화면 표시용 DTO 목록으로 변환합니다.
     */
    private List<MyPageActivityItemDTO> toLikeActivityItems(List<InsightLike> likeList) {
        if (likeList == null || likeList.isEmpty()) {
            return Collections.emptyList();
        }

        List<MyPageActivityItemDTO> activityItemList = new ArrayList<>();
        for (InsightLike like : likeList) {
            MyPageActivityItemDTO activityItem = toActivityItem(
                    like.getContentType(),
                    like.getContentId(),
                    like.getCreatedAt()
            );
            if (activityItem != null) {
                activityItemList.add(activityItem);
            }
        }
        return activityItemList;
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
