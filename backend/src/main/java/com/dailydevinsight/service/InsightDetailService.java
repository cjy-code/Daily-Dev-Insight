package com.dailydevinsight.service;

import com.dailydevinsight.dto.InsightCommentDTO;
import com.dailydevinsight.dto.InsightContentType;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.dto.InsightToggleResponseDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.InsightBookmark;
import com.dailydevinsight.entity.InsightComment;
import com.dailydevinsight.entity.InsightLike;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.InsightCommentRepository;
import com.dailydevinsight.repository.InsightLikeRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Transactional
public class InsightDetailService {

    private static final int COMMENT_ACTIVE = 0;
    private static final int MAX_COMMENT_LENGTH = 500;

    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final TechNewsRepository techNewsRepository;
    private final InsightLikeRepository insightLikeRepository;
    private final InsightBookmarkRepository insightBookmarkRepository;
    private final InsightCommentRepository insightCommentRepository;
    private final UserRepository userRepository;

    /**
     * @date 2026-04-14
     * @desc 상세 페이지 진입 시 조회수 증가 여부를 반영하여 상세 데이터를 반환합니다.
     */
    public InsightDetailResponseDTO getInsightDetail(String type, Long contentId, String userEmail, boolean shouldIncreaseViewCount) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        if (shouldIncreaseViewCount) {
            incrementViewCount(contentType, contentId);
        }
        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 조회수 증가 없이 상호작용 집계 상태만 조회합니다.
     */
    public InsightDetailResponseDTO getEngagementOnly(String type, Long contentId, String userEmail) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 좋아요를 사용자 단위로 토글하고 최신 카운트를 반환합니다.
     */
    public InsightToggleResponseDTO toggleLike(String type, Long contentId, String userEmail) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        ensureContentExists(contentType, contentId);

        String contentTypeKey = toContentTypeKey(contentType);
        boolean active;
        InsightLike existing = insightLikeRepository
                .findByContentTypeAndContentIdAndUserId(contentTypeKey, contentId, userId)
                .orElse(null);

        if (existing != null) {
            insightLikeRepository.delete(existing);
            active = false;
        } else {
            insightLikeRepository.save(InsightLike.builder()
                    .contentType(contentTypeKey)
                    .contentId(contentId)
                    .userId(userId)
                    .build());
            active = true;
        }

        long count = insightLikeRepository.countByContentTypeAndContentId(contentTypeKey, contentId);
        return InsightToggleResponseDTO.builder()
                .active(active)
                .count(count)
                .build();
    }

    /**
     * @date 2026-04-14
     * @desc 북마크를 사용자 단위로 토글하고 최신 카운트를 반환합니다.
     */
    public InsightToggleResponseDTO toggleBookmark(String type, Long contentId, String userEmail) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        ensureContentExists(contentType, contentId);

        String contentTypeKey = toContentTypeKey(contentType);
        boolean active;
        InsightBookmark existing = insightBookmarkRepository
                .findByContentTypeAndContentIdAndUserId(contentTypeKey, contentId, userId)
                .orElse(null);

        if (existing != null) {
            insightBookmarkRepository.delete(existing);
            active = false;
        } else {
            insightBookmarkRepository.save(InsightBookmark.builder()
                    .contentType(contentTypeKey)
                    .contentId(contentId)
                    .userId(userId)
                    .build());
            active = true;
        }

        long count = insightBookmarkRepository.countByContentTypeAndContentId(contentTypeKey, contentId);
        return InsightToggleResponseDTO.builder()
                .active(active)
                .count(count)
                .build();
    }

    /**
     * @date 2026-04-14
     * @desc 댓글을 등록하고 최신 상세 상태를 반환합니다.
     */
    public InsightDetailResponseDTO addComment(String type, Long contentId, String userEmail, String content) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        ensureContentExists(contentType, contentId);

        String normalizedContent = normalizeCommentContent(content);
        insightCommentRepository.save(InsightComment.builder()
                .contentType(toContentTypeKey(contentType))
                .contentId(contentId)
                .userId(userId)
                .content(normalizedContent)
                .isDeleted(COMMENT_ACTIVE)
                .build());

        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 본인 댓글만 소프트 삭제하고 최신 상세 상태를 반환합니다.
     */
    public InsightDetailResponseDTO deleteComment(String type, Long contentId, Long commentId, String userEmail) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(userEmail);
        ensureContentExists(contentType, contentId);

        InsightComment targetComment = insightCommentRepository
                .findByIdAndContentTypeAndContentIdAndIsDeleted(commentId, toContentTypeKey(contentType), contentId, COMMENT_ACTIVE)
                .orElse(null);

        if (targetComment != null && userId.equals(targetComment.getUserId())) {
            targetComment.markDeleted();
            insightCommentRepository.save(targetComment);
        }

        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 콘텐츠 타입 문자열을 enum으로 변환하며 유효성 오류를 공통 처리합니다.
     */
    private InsightContentType resolveContentType(String type) {
        try {
            return InsightContentType.from(type);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, exception.getMessage());
        }
    }

    /**
     * @date 2026-04-14
     * @desc 인증 이메일을 users.id로 매핑하고 미인증 상태를 차단합니다.
     */
    private Long resolveUserId(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * @date 2026-04-14
     * @desc 콘텐츠 타입별 조회수 컬럼을 1 증가시킵니다.
     */
    private void incrementViewCount(InsightContentType contentType, Long contentId) {
        int updatedCount = switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.incrementViewCount(contentId);
            case NEWS -> techNewsRepository.incrementViewCount(contentId);
        };

        if (updatedCount == 0) {
            throw new ResponseStatusException(NOT_FOUND, "상세 대상을 찾을 수 없습니다.");
        }
    }

    /**
     * @date 2026-04-14
     * @desc 콘텐츠 타입/ID로 상세 기본 데이터를 조회합니다.
     */
    private InsightBaseData findBaseData(InsightContentType contentType, Long contentId) {
        return switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.findById(contentId)
                    .map(this::toKnowledgeBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "인사이트를 찾을 수 없습니다."));
            case NEWS -> techNewsRepository.findById(contentId)
                    .map(this::toNewsBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "뉴스를 찾을 수 없습니다."));
        };
    }

    /**
     * @date 2026-04-14
     * @desc 토글/댓글 작업 전 콘텐츠 존재 여부를 검증합니다.
     */
    private void ensureContentExists(InsightContentType contentType, Long contentId) {
        findBaseData(contentType, contentId);
    }

    /**
     * @date 2026-04-14
     * @desc 댓글 내용을 trim/길이 검증하여 저장 가능한 문자열로 정규화합니다.
     */
    private String normalizeCommentContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "댓글 내용을 입력해 주세요.");
        }
        if (normalizedContent.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "댓글은 500자 이하로 입력해 주세요.");
        }
        return normalizedContent;
    }

    /**
     * @date 2026-04-14
     * @desc enum 타입을 DB 저장용 문자열 키로 변환합니다.
     */
    private String toContentTypeKey(InsightContentType contentType) {
        return contentType.name();
    }

    /**
     * @date 2026-04-14
     * @desc 상세 응답 DTO를 DB 집계 기준으로 구성합니다.
     */
    private InsightDetailResponseDTO buildDetailResponse(InsightContentType contentType, InsightBaseData baseData, Long userId) {
        String contentTypeKey = toContentTypeKey(contentType);
        long likeCount = insightLikeRepository.countByContentTypeAndContentId(contentTypeKey, baseData.id());
        long bookmarkCount = insightBookmarkRepository.countByContentTypeAndContentId(contentTypeKey, baseData.id());
        boolean liked = insightLikeRepository.findByContentTypeAndContentIdAndUserId(contentTypeKey, baseData.id(), userId).isPresent();
        boolean bookmarked = insightBookmarkRepository.findByContentTypeAndContentIdAndUserId(contentTypeKey, baseData.id(), userId).isPresent();
        List<InsightCommentDTO> comments = findCommentDtos(contentTypeKey, baseData.id(), userId);

        return InsightDetailResponseDTO.builder()
                .type(contentType.getValue())
                .id(baseData.id())
                .title(baseData.title())
                .summary(baseData.summary())
                .detail(baseData.detail())
                .source(baseData.source())
                .url(baseData.url())
                .publishedAt(baseData.publishedAt())
                .viewCount(baseData.viewCount())
                .likeCount(likeCount)
                .bookmarkCount(bookmarkCount)
                .liked(liked)
                .bookmarked(bookmarked)
                .comments(comments)
                .build();
    }

    /**
     * @date 2026-04-14
     * @desc 댓글 목록과 작성자명을 결합하여 응답 DTO 목록으로 변환합니다.
     */
    private List<InsightCommentDTO> findCommentDtos(String contentTypeKey, Long contentId, Long loginUserId) {
        List<InsightComment> commentList = insightCommentRepository
                .findByContentTypeAndContentIdAndIsDeletedOrderByCreatedAtDesc(contentTypeKey, contentId, COMMENT_ACTIVE);
        Set<Long> commentWriterIds = commentList.stream().map(InsightComment::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNameById = userRepository.findAllById(commentWriterIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return commentList.stream()
                .map(comment -> InsightCommentDTO.builder()
                        .id(comment.getId())
                        .authorName(userNameById.getOrDefault(comment.getUserId(), "알 수 없음"))
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .mine(loginUserId.equals(comment.getUserId()))
                        .build())
                .toList();
    }

    /**
     * @date 2026-04-14
     * @desc DailyKnowledge 엔티티를 상세 기본 데이터로 변환합니다.
     */
    private InsightBaseData toKnowledgeBaseData(DailyKnowledge knowledge) {
        long viewCount = knowledge.getViewCount() == null ? 0L : knowledge.getViewCount();
        return new InsightBaseData(
                knowledge.getId(),
                knowledge.getTitle(),
                knowledge.getSummary(),
                knowledge.getDetail(),
                knowledge.getCategory(),
                null,
                knowledge.getKnowledgeDate(),
                viewCount
        );
    }

    /**
     * @date 2026-04-14
     * @desc TechNews 엔티티를 상세 기본 데이터로 변환합니다.
     */
    private InsightBaseData toNewsBaseData(TechNews news) {
        long viewCount = news.getViewCount() == null ? 0L : news.getViewCount();
        return new InsightBaseData(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getSummary(),
                news.getSource(),
                news.getUrl(),
                news.getNewsDate(),
                viewCount
        );
    }

    private record InsightBaseData(
            Long id,
            String title,
            String summary,
            String detail,
            String source,
            String url,
            LocalDate publishedAt,
            long viewCount
    ) {
    }
}
