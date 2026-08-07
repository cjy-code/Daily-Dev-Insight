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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Transactional
public class InsightDetailService {

    private static final int COMMENT_ACTIVE = 0;
    private static final int COMMENT_DELETED = 1;
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";
    private static final String DELETED_COMMENT_AUTHOR_NAME = "삭제된 사용자";

    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final TechNewsRepository techNewsRepository;
    private final InsightLikeRepository insightLikeRepository;
    private final InsightBookmarkRepository insightBookmarkRepository;
    private final InsightCommentRepository insightCommentRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    /**
     * @date 2026-04-14
     * @desc ?怨멸쉭 ??륁뵠筌왖 筌욊쑴????鈺곌퀬???筌앹빓? ?????獄쏆꼷???뤿연 ?怨멸쉭 ?怨쀬뵠?怨? 獄쏆꼹???몃빍??
     */
    public InsightDetailResponseDTO getInsightDetail(String type, Long contentId, String loginUserId, boolean shouldIncreaseViewCount) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
        if (shouldIncreaseViewCount) {
            incrementViewCount(contentType, contentId);
            clearInsightEngagementCache();
        }
        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 鈺곌퀬???筌앹빓? ??곸뵠 ?怨뱀깈?臾믪뒠 筌욌쵌???怨밴묶筌?鈺곌퀬???몃빍??
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
     * @desc ?ル뿭釉?遺? ???????μ맄嚥??醫???랁?筌ㅼ뮇??燁삳똻??紐? 獄쏆꼹???몃빍??
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
     * @desc ?브낮彛??? ???????μ맄嚥??醫???랁?筌ㅼ뮇??燁삳똻??紐? 獄쏆꼹???몃빍??
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
     * @desc ?蹂????源낆쨯??랁?筌ㅼ뮇???怨멸쉭 ?怨밴묶??獄쏆꼹???몃빍??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightDetailResponseDTO addComment(String type, Long contentId, String loginUserId, String content, Long parentCommentId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
        ensureContentExists(contentType, contentId);

        String normalizedContent = normalizeCommentContent(content);
        Long validatedParentCommentId = validateParentCommentId(contentType, contentId, parentCommentId);
        insightCommentRepository.save(InsightComment.builder()
                .contentType(toContentTypeKey(contentType))
                .contentId(contentId)
                .userId(userId)
                .content(normalizedContent)
                .parentCommentId(validatedParentCommentId)
                .isDeleted(COMMENT_ACTIVE)
                .build());

        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc 癰귣챷???蹂?筌???곕늄???????랁?筌ㅼ뮇???怨멸쉭 ?怨밴묶??獄쏆꼹???몃빍??
     */
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT, allEntries = true)
    public InsightDetailResponseDTO deleteComment(String type, Long contentId, Long commentId, String loginUserId) {
        InsightContentType contentType = resolveContentType(type);
        Long userId = resolveUserId(loginUserId);
        ensureContentExists(contentType, contentId);

        InsightComment targetComment = insightCommentRepository
                .findByIdAndContentTypeAndContentIdAndIsDeleted(commentId, toContentTypeKey(contentType), contentId, COMMENT_ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "이미 삭제되었거나 존재하지 않는 댓글입니다."));

        if (!userId.equals(targetComment.getUserId())) {
            throw new ResponseStatusException(FORBIDDEN, "본인 댓글만 삭제할 수 있습니다.");
        }
        targetComment.markDeleted();
        insightCommentRepository.save(targetComment);

        InsightBaseData baseData = findBaseData(contentType, contentId);
        return buildDetailResponse(contentType, baseData, userId);
    }

    /**
     * @date 2026-04-14
     * @desc ?꾩꼹?쀯㎘??????얜챷???곸뱽 enum??곗쨮 癰궰??묐릭筌??醫륁뒞????살첒???⑤벏??筌ｌ꼶???몃빍??
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
     * @desc ?紐꾩쵄 ??李??깆뱽 users.id嚥?筌띲끋釉??랁?沃섎챷?ㅿ쭩??怨밴묶??筌△뫀???몃빍??
     */
    private Long resolveUserId(String loginUserId) {
        if (loginUserId == null || loginUserId.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "嚥≪뮄??紐꾩뵠 ?袁⑹뒄??몃빍??");
        }

        return userRepository.findByUserId(loginUserId)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "??????類ｋ궖??筌≪뼚??????곷뮸??덈뼄."));
    }

    /**
     * @date 2026-04-14
     * @desc ?꾩꼹?쀯㎘?????낇?鈺곌퀬????뚎됱쓥??1 筌앹빓???쀪땁??덈뼄.
     */
    private void incrementViewCount(InsightContentType contentType, Long contentId) {
        int updatedCount = switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.incrementViewCount(contentId);
            case NEWS -> techNewsRepository.incrementViewCount(contentId);
        };

        if (updatedCount == 0) {
            throw new ResponseStatusException(NOT_FOUND, "?怨멸쉭 ???怨몄뱽 筌≪뼚??????곷뮸??덈뼄.");
        }
    }

    /**
     * @date 2026-04-16
     * @desc 조회수 증가 직후 집계 캐시를 비워 상세 페이지에 최신 조회수가 반영되도록 합니다.
     */
    private void clearInsightEngagementCache() {
        Cache engagementCache = cacheManager.getCache(RedisCacheConfig.CACHE_INSIGHT_ENGAGEMENT);
        if (engagementCache != null) {
            engagementCache.clear();
        }
    }

    /**
     * @date 2026-04-14
     * @desc ?꾩꼹?쀯㎘?????ID嚥??怨멸쉭 疫꿸퀡???怨쀬뵠?怨? 鈺곌퀬???몃빍??
     */
    private InsightBaseData findBaseData(InsightContentType contentType, Long contentId) {
        return switch (contentType) {
            case KNOWLEDGE -> dailyKnowledgeRepository.findById(contentId)
                    .map(this::toKnowledgeBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "?紐꾧텢??꾨뱜??筌≪뼚??????곷뮸??덈뼄."));
            case NEWS -> techNewsRepository.findById(contentId)
                    .map(this::toNewsBaseData)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "??곷뮞??筌≪뼚??????곷뮸??덈뼄."));
        };
    }

    /**
     * @date 2026-04-14
     * @desc ?醫?/?蹂? ?臾믩씜 ???꾩꼹?쀯㎘?鈺곕똻???????野꺜筌앹빜鍮??덈뼄.
     */
    private void ensureContentExists(InsightContentType contentType, Long contentId) {
        findBaseData(contentType, contentId);
    }

    /**
     * @date 2026-04-14
     * @desc ?蹂? ??곸뒠??trim/疫뀀챷??野꺜筌앹빜釉??????揶쎛?館釉??얜챷???以??類?뇣?酉鍮??덈뼄.
     */
    private String normalizeCommentContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "?蹂? ??곸뒠????낆젾??雅뚯눘苑??");
        }
        if (normalizedContent.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "?蹂??? 500????꾨릭嚥???낆젾??雅뚯눘苑??");
        }
        return normalizedContent;
    }
    /**
     * @date 2026-08-07
     * @desc 대댓글 등록 시 삭제 여부와 관계없이 부모 댓글의 존재 여부와 동일 콘텐츠 여부를 검증합니다.
     */
    private Long validateParentCommentId(InsightContentType contentType, Long contentId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        String contentTypeKey = toContentTypeKey(contentType);
        InsightComment parentComment = insightCommentRepository
                .findById(parentCommentId)
                .filter(comment -> contentTypeKey.equals(comment.getContentType()) && contentId.equals(comment.getContentId()))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "유효하지 않은 부모 댓글입니다."));

        return parentComment.getId();
    }

    /**
     * @date 2026-04-14
     * @desc enum ????놁뱽 DB ???關???얜챷?????살쨮 癰궰??묐???덈뼄.
     */
    private String toContentTypeKey(InsightContentType contentType) {
        return contentType.name();
    }

    /**
     * @date 2026-04-14
     * @desc ?怨멸쉭 ?臾먮뼗 DTO??DB 筌욌쵌??疫꿸퀣???곗쨮 ?닌딄쉐??몃빍??
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
                .thumbnailUrl(baseData.thumbnailUrl())
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
     * @date 2026-08-07
     * @desc 삭제된 댓글을 placeholder로 치환하고 전체 댓글을 계층형 DTO 목록으로 변환합니다.
     */
    private List<InsightCommentDTO> findCommentDtos(String contentTypeKey, Long contentId, Long loginUserId) {
        List<InsightComment> commentList = insightCommentRepository
                .findByContentTypeAndContentIdOrderByCreatedAtAsc(contentTypeKey, contentId);
        if (commentList.isEmpty()) {
            return List.of();
        }

        Set<Long> commentWriterIds = commentList.stream()
                .filter(comment -> !isDeletedComment(comment))
                .map(InsightComment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> userNameById = userRepository.findAllById(commentWriterIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, InsightCommentDTO> commentDtoById = new LinkedHashMap<>();

        for (InsightComment comment : commentList) {
            boolean deleted = isDeletedComment(comment);
            commentDtoById.put(comment.getId(), InsightCommentDTO.builder()
                    .id(comment.getId())
                    .parentCommentId(comment.getParentCommentId())
                    .authorName(deleted ? DELETED_COMMENT_AUTHOR_NAME : userNameById.getOrDefault(comment.getUserId(), "??????곸벉"))
                    .content(deleted ? DELETED_COMMENT_CONTENT : comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .mine(!deleted && loginUserId.equals(comment.getUserId()))
                    .deleted(deleted)
                    .replies(new ArrayList<>())
                    .build());
        }

        List<InsightCommentDTO> rootComments = new ArrayList<>();
        for (InsightComment comment : commentList) {
            InsightCommentDTO commentDto = commentDtoById.get(comment.getId());
            Long parentCommentId = comment.getParentCommentId();
            InsightCommentDTO parentCommentDto = parentCommentId == null ? null : commentDtoById.get(parentCommentId);

            if (parentCommentDto == null) {
                rootComments.add(commentDto);
                continue;
            }
            parentCommentDto.getReplies().add(commentDto);
        }

        return rootComments;
    }

    /**
     * @date 2026-08-07
     * @desc 댓글 엔티티가 소프트 삭제 상태인지 확인합니다.
     */
    private boolean isDeletedComment(InsightComment comment) {
        return Integer.valueOf(COMMENT_DELETED).equals(comment.getIsDeleted());
    }

    /**
     * @date 2026-04-14
     * @desc DailyKnowledge ?酉??怨? ?怨멸쉭 疫꿸퀡???怨쀬뵠?怨뺤쨮 癰궰??묐???덈뼄.
     */
    private InsightBaseData toKnowledgeBaseData(DailyKnowledge knowledge) {
        long viewCount = knowledge.getViewCount() == null ? 0L : knowledge.getViewCount();
        return new InsightBaseData(
                knowledge.getId(),
                knowledge.getTitle(),
                knowledge.getSummary(),
                knowledge.getDetail(),
                knowledge.getAttachmentImagePath(),
                knowledge.getCategory(),
                null,
                knowledge.getKnowledgeDate(),
                viewCount
        );
    }

    /**
     * @date 2026-04-14
     * @desc TechNews ?酉??怨? ?怨멸쉭 疫꿸퀡???怨쀬뵠?怨뺤쨮 癰궰??묐???덈뼄.
     */
    private InsightBaseData toNewsBaseData(TechNews news) {
        long viewCount = news.getViewCount() == null ? 0L : news.getViewCount();
        return new InsightBaseData(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getSummary(),
                news.getAttachmentImagePath(),
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
            String thumbnailUrl,
            String source,
            String url,
            LocalDate publishedAt,
            long viewCount
    ) {
    }
}



