package com.dailydevinsight.service;

import com.dailydevinsight.config.RedisCacheConfig;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * @desc ?곸꽭 ?섏씠吏 吏꾩엯 ??議고쉶??利앷? ?щ?瑜?諛섏쁺?섏뿬 ?곸꽭 ?곗씠?곕? 諛섑솚?⑸땲??
     */
    public InsightDetailResponseDTO getInsightDetail(String type, Long contentId, String loginUserId, boolean shouldIncreaseViewCount) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
        if (shouldIncreaseViewCount) {
            incrementViewCount(contentType, contentId);
        }
        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 議고쉶??利앷? ?놁씠 ?곹샇?묒슜 吏묎퀎 ?곹깭留?議고쉶?⑸땲??
     */
    @Cacheable(
            cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT,
            key = "(#type == null ? '' : #type.trim().toLowerCase())"
                    + " + ':' + "
                    + "(#contentId == null ? '' : #contentId.toString())"
                    + " + ':' + "
                    + "(#loginUserId == null ? '' : #loginUserId.trim().toLowerCase())"
    )
    public InsightDetailResponseDTO getEngagementOnly(String type, Long contentId, String loginUserId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 醫뗭븘?붾? ?ъ슜???⑥쐞濡??좉??섍퀬 理쒖떊 移댁슫?몃? 諛섑솚?⑸땲??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightToggleResponseDTO toggleLike(String type, Long contentId, String loginUserId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
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
     * @desc 遺곷쭏?щ? ?ъ슜???⑥쐞濡??좉??섍퀬 理쒖떊 移댁슫?몃? 諛섑솚?⑸땲??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightToggleResponseDTO toggleBookmark(String type, Long contentId, String loginUserId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
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
     * @desc ?볤????깅줉?섍퀬 理쒖떊 ?곸꽭 ?곹깭瑜?諛섑솚?⑸땲??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightDetailResponseDTO addComment(String type, Long contentId, String loginUserId, String content) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
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
     * @desc 蹂몄씤 ?볤?留??뚰봽????젣?섍퀬 理쒖떊 ?곸꽭 ?곹깭瑜?諛섑솚?⑸땲??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightDetailResponseDTO deleteComment(String type, Long contentId, Long commentId, String loginUserId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
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
     * @desc 肄섑뀗痢????臾몄옄?댁쓣 enum?쇰줈 蹂?섑븯硫??좏슚???ㅻ쪟瑜?怨듯넻 泥섎━?⑸땲??
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
     * @desc ?몄쬆 ?대찓?쇱쓣 users.id濡?留ㅽ븨?섍퀬 誘몄씤利??곹깭瑜?李⑤떒?⑸땲??
     */
    private Long resolveUserId(String loginUserId) {
        if (loginUserId == null || loginUserId.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "濡쒓렇?몄씠 ?꾩슂?⑸땲??");
        }

        return userRepository.findByUserId(loginUserId)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "?ъ슜???뺣낫瑜?李얠쓣 ???놁뒿?덈떎."));
    }

    /**
     * @date 2026-04-14
     * @desc 肄섑뀗痢???낅퀎 議고쉶??而щ읆??1 利앷??쒗궢?덈떎.
     */
    private void incrementViewCount(InsightContentType contentType, Long contentId) {
        int updatedCount = switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.incrementViewCount(contentId);
            case NEWS -> techNewsRepository.incrementViewCount(contentId);
        };

        if (updatedCount == 0) {
            throw new ResponseStatusException(NOT_FOUND, "?곸꽭 ??곸쓣 李얠쓣 ???놁뒿?덈떎.");
        }
    }

    /**
     * @date 2026-04-14
     * @desc 肄섑뀗痢????ID濡??곸꽭 湲곕낯 ?곗씠?곕? 議고쉶?⑸땲??
     */
    private InsightBaseData findBaseData(InsightContentType contentType, Long contentId) {
        return switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.findById(contentId)
                    .map(this::toKnowledgeBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "?몄궗?댄듃瑜?李얠쓣 ???놁뒿?덈떎."));
            case NEWS -> techNewsRepository.findById(contentId)
                    .map(this::toNewsBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "?댁뒪瑜?李얠쓣 ???놁뒿?덈떎."));
        };
    }

    /**
     * @date 2026-04-14
     * @desc ?좉?/?볤? ?묒뾽 ??肄섑뀗痢?議댁옱 ?щ?瑜?寃利앺빀?덈떎.
     */
    private void ensureContentExists(InsightContentType contentType, Long contentId) {
        findBaseData(contentType, contentId);
    }

    /**
     * @date 2026-04-14
     * @desc ?볤? ?댁슜??trim/湲몄씠 寃利앺븯?????媛?ν븳 臾몄옄?대줈 ?뺢퇋?뷀빀?덈떎.
     */
    private String normalizeCommentContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "?볤? ?댁슜???낅젰??二쇱꽭??");
        }
        if (normalizedContent.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "?볤?? 500???댄븯濡??낅젰??二쇱꽭??");
        }
        return normalizedContent;
    }

    /**
     * @date 2026-04-14
     * @desc enum ??낆쓣 DB ??μ슜 臾몄옄???ㅻ줈 蹂?섑빀?덈떎.
     */
    private String toContentTypeKey(InsightContentType contentType) {
        return contentType.name();
    }

    /**
     * @date 2026-04-14
     * @desc ?곸꽭 ?묐떟 DTO瑜?DB 吏묎퀎 湲곗??쇰줈 援ъ꽦?⑸땲??
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
     * @desc ?볤? 紐⑸줉怨??묒꽦?먮챸??寃고빀?섏뿬 ?묐떟 DTO 紐⑸줉?쇰줈 蹂?섑빀?덈떎.
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
                        .authorName(userNameById.getOrDefault(comment.getUserId(), "?????놁쓬"))
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .mine(loginUserId.equals(comment.getUserId()))
                        .build())
                .toList();
    }

    /**
     * @date 2026-04-14
     * @desc DailyKnowledge ?뷀떚?곕? ?곸꽭 湲곕낯 ?곗씠?곕줈 蹂?섑빀?덈떎.
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
     * @desc TechNews ?뷀떚?곕? ?곸꽭 湲곕낯 ?곗씠?곕줈 蹂?섑빀?덈떎.
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

