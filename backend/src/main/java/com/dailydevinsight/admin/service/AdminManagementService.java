package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.config.RedisCacheConfig;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private static final String KNOWLEDGE_DIRECTORY_NAME = "knowledge";
    private static final String NEWS_DIRECTORY_NAME = "news";
    private static final String UPLOAD_PUBLIC_PREFIX = "/uploads/";
    private static final long MAX_THUMBNAIL_FILE_BYTES = 5L * 1024L * 1024L;
    private static final DateTimeFormatter DATE_DIRECTORY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif"
    );
    private static final String CONTENT_TYPE_KNOWLEDGE = "KNOWLEDGE";
    private static final String CONTENT_TYPE_NEWS = "NEWS";
    private static final int TOP_CONTENT_LIMIT = 5;
    private static final int ADMIN_LIST_PAGE_SIZE = 20;

    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final TechNewsRepository techNewsRepository;
    private final UserRepository userRepository;
    private final GenerationHistoryRepository generationHistoryRepository;
    private final InsightBookmarkRepository insightBookmarkRepository;

    @Value("${crawler.thumbnail-upload-dir:./uploads}")
    private String thumbnailUploadDirectory;

    /**
     * @date 2026-08-06
     * @desc 관리자 일일 지식 관리 화면에 출력할 게시물 목록을 페이지 단위로 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<DailyKnowledge> findKnowledgePosts(int pageNumber) {
        return dailyKnowledgeRepository.findAllByOrderByKnowledgeDateDescIdDesc(toPageable(pageNumber));
    }

    /**
     * @date 2026-08-06
     * @desc 관리자 테크 뉴스 관리 화면에 출력할 게시물 목록을 페이지 단위로 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<TechNews> findTechNewsPosts(int pageNumber) {
        return techNewsRepository.findAllByOrderByNewsDateDescIdDesc(toPageable(pageNumber));
    }

    /**
     * @date 2026-08-06
     * @desc 관리자 회원 관리 화면에 출력할 회원 목록을 페이지 단위로 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<User> findUsers(int pageNumber) {
        return userRepository.findAllByOrderByCreatedAtDescIdDesc(toPageable(pageNumber));
    }

    /**
     * @date 2026-08-06
     * @desc 0 미만의 요청 페이지 번호를 보정하여 고정 크기의 Pageable을 생성합니다.
     */
    private Pageable toPageable(int pageNumber) {
        int normalizedPageNumber = Math.max(pageNumber, 0);
        return PageRequest.of(normalizedPageNumber, ADMIN_LIST_PAGE_SIZE);
    }

    /**
     * @date 2026-04-15
     * @desc 일일 지식 게시물의 카테고리와 제목을 수정합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void updateKnowledgePost(Long postId, String category, String title) {
        DailyKnowledge originalPost = dailyKnowledgeRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        DailyKnowledge updatedPost = DailyKnowledge.builder()
                .id(originalPost.getId())
                .knowledgeDate(originalPost.getKnowledgeDate())
                .category(normalizeRequiredValue(category, "카테고리"))
                .title(normalizeRequiredValue(title, "제목"))
                .attachmentImagePath(originalPost.getAttachmentImagePath())
                .summary(originalPost.getSummary())
                .detail(originalPost.getDetail())
                .viewCount(originalPost.getViewCount())
                .createdAt(originalPost.getCreatedAt())
                .build();

        dailyKnowledgeRepository.save(updatedPost);
    }

    /**
     * @date 2026-04-21
     * @desc 일일 지식 게시물 썸네일 파일을 저장하고 공개 경로를 반영합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void updateKnowledgeThumbnail(Long postId, MultipartFile thumbnailFile) {
        DailyKnowledge originalPost = dailyKnowledgeRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        String storedThumbnailPath = storeThumbnailFile(thumbnailFile, KNOWLEDGE_DIRECTORY_NAME, originalPost.getKnowledgeDate());
        String previousThumbnailPath = originalPost.getAttachmentImagePath();

        try {
            DailyKnowledge updatedPost = buildKnowledgePostWithThumbnailPath(originalPost, storedThumbnailPath);
            dailyKnowledgeRepository.save(updatedPost);
            deleteStoredThumbnailIfPossible(previousThumbnailPath);
        } catch (RuntimeException runtimeException) {
            deleteStoredThumbnailIfPossible(storedThumbnailPath);
            throw runtimeException;
        }
    }

    /**
     * @date 2026-04-21
     * @desc 일일 지식 게시물의 썸네일 정보를 제거하고 파일 삭제를 시도합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void deleteKnowledgeThumbnail(Long postId) {
        DailyKnowledge originalPost = dailyKnowledgeRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 게시물을 찾을 수 없습니다."));

        String previousThumbnailPath = originalPost.getAttachmentImagePath();
        DailyKnowledge updatedPost = buildKnowledgePostWithThumbnailPath(originalPost, null);
        dailyKnowledgeRepository.save(updatedPost);
        deleteStoredThumbnailIfPossible(previousThumbnailPath);
    }

    /**
     * @date 2026-04-15
     * @desc 일일 지식 게시물을 삭제합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void deleteKnowledgePost(Long postId) {
        if (!dailyKnowledgeRepository.existsById(postId)) {
            throw new IllegalArgumentException("삭제할 게시물을 찾을 수 없습니다.");
        }
        generationHistoryRepository.clearCreatedKnowledgeId(postId);
        dailyKnowledgeRepository.deleteById(postId);
    }

    /**
     * @date 2026-04-17
     * @desc 테크 뉴스 게시물의 출처와 제목을 수정합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void updateTechNewsPost(Long newsId, String source, String title) {
        TechNews originalNews = techNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("테크 뉴스를 찾을 수 없습니다."));

        TechNews updatedNews = TechNews.builder()
                .id(originalNews.getId())
                .newsDate(originalNews.getNewsDate())
                .source(normalizeRequiredValue(source, "출처"))
                .title(normalizeRequiredValue(title, "제목"))
                .url(originalNews.getUrl())
                .attachmentImagePath(originalNews.getAttachmentImagePath())
                .summary(originalNews.getSummary())
                .viewCount(originalNews.getViewCount())
                .createdAt(originalNews.getCreatedAt())
                .build();

        techNewsRepository.save(updatedNews);
    }

    /**
     * @date 2026-04-21
     * @desc 테크 뉴스 게시물 썸네일 파일을 저장하고 공개 경로를 반영합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void updateTechNewsThumbnail(Long newsId, MultipartFile thumbnailFile) {
        TechNews originalNews = techNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("테크 뉴스를 찾을 수 없습니다."));

        String storedThumbnailPath = storeThumbnailFile(thumbnailFile, NEWS_DIRECTORY_NAME, originalNews.getNewsDate());
        String previousThumbnailPath = originalNews.getAttachmentImagePath();

        try {
            TechNews updatedNews = buildTechNewsWithThumbnailPath(originalNews, storedThumbnailPath);
            techNewsRepository.save(updatedNews);
            deleteStoredThumbnailIfPossible(previousThumbnailPath);
        } catch (RuntimeException runtimeException) {
            deleteStoredThumbnailIfPossible(storedThumbnailPath);
            throw runtimeException;
        }
    }

    /**
     * @date 2026-04-21
     * @desc 테크 뉴스 게시물의 썸네일 정보를 제거하고 파일 삭제를 시도합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void deleteTechNewsThumbnail(Long newsId) {
        TechNews originalNews = techNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 테크 뉴스를 찾을 수 없습니다."));

        String previousThumbnailPath = originalNews.getAttachmentImagePath();
        TechNews updatedNews = buildTechNewsWithThumbnailPath(originalNews, null);
        techNewsRepository.save(updatedNews);
        deleteStoredThumbnailIfPossible(previousThumbnailPath);
    }

    /**
     * @date 2026-04-15
     * @desc 테크 뉴스 게시물을 삭제합니다.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_DATE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_INSIGHTS_BY_RANGE, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP10, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_WEEKLY_TOP5, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS, allEntries = true),
            @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS, allEntries = true)
    })
    public void deleteTechNewsPost(Long newsId) {
        if (!techNewsRepository.existsById(newsId)) {
            throw new IllegalArgumentException("삭제할 테크 뉴스를 찾을 수 없습니다.");
        }
        techNewsRepository.deleteById(newsId);
    }

    /**
     * @date 2026-04-15
     * @desc 회원의 권한과 상태를 수정합니다.
     */
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS, allEntries = true)
    public void updateUser(Long userPrimaryKey, String role, String status) {
        User originalUser = userRepository.findById(userPrimaryKey)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeStatus(status);

        User updatedUser = User.builder()
                .id(originalUser.getId())
                .userId(originalUser.getUserId())
                .email(originalUser.getEmail())
                .password(originalUser.getPassword())
                .name(originalUser.getName())
                .role(normalizedRole)
                .status(normalizedStatus)
                .createdAt(originalUser.getCreatedAt())
                .updatedAt(originalUser.getUpdatedAt())
                .build();

        userRepository.save(updatedUser);
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 대시보드 통계 지표를 계산합니다.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_ADMIN_STATS)
    public AdminStatsData getAdminStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatusIgnoreCase("ACTIVE");
        long totalPosts = dailyKnowledgeRepository.count();
        long todayPosts = dailyKnowledgeRepository.countByKnowledgeDate(LocalDate.now());
        long generationSuccessCount = generationHistoryRepository.countByStatus("SUCCESS");
        long generationFailedCount = generationHistoryRepository.countByStatus("FAILED");
        long knowledgeViewCount = dailyKnowledgeRepository.sumViewCount();
        long newsViewCount = techNewsRepository.sumViewCount();
        long totalViewCount = knowledgeViewCount + newsViewCount;
        long totalBookmarkCount = insightBookmarkRepository.count();

        return AdminStatsData.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalPosts(totalPosts)
                .todayPosts(todayPosts)
                .generationSuccessCount(generationSuccessCount)
                .generationFailedCount(generationFailedCount)
                .totalViewCount(totalViewCount)
                .totalBookmarkCount(totalBookmarkCount)
                .build();
    }

    /**
     * @date 2026-04-22
     * @desc 조회수 통계 상세 화면용 집계 데이터(총합/콘텐츠별/상위 콘텐츠)를 계산합니다.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_ADMIN_CONTENT_VIEW_STATS)
    public AdminContentViewStatsData getContentViewStats() {
        long knowledgeViewCount = dailyKnowledgeRepository.sumViewCount();
        long newsViewCount = techNewsRepository.sumViewCount();

        return AdminContentViewStatsData.builder()
                .totalViewCount(knowledgeViewCount + newsViewCount)
                .knowledgeViewCount(knowledgeViewCount)
                .newsViewCount(newsViewCount)
                .topKnowledgeList(mapTopKnowledgeByViewCount())
                .topNewsList(mapTopNewsByViewCount())
                .build();
    }

    /**
     * @date 2026-04-22
     * @desc 북마크 통계 상세 화면용 집계 데이터(총 북마크/참여 사용자/상위 콘텐츠)를 계산합니다.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_ADMIN_BOOKMARK_STATS)
    public AdminBookmarkStatsData getBookmarkStats() {
        Pageable topContentPage = PageRequest.of(0, TOP_CONTENT_LIMIT);
        var topBookmarkedRows = insightBookmarkRepository.findTopBookmarkedContents(topContentPage);

        return AdminBookmarkStatsData.builder()
                .totalBookmarkCount(insightBookmarkRepository.count())
                .bookmarkedUserCount(insightBookmarkRepository.countDistinctUserId())
                .topBookmarkedContentList(mapTopBookmarkedContents(topBookmarkedRows))
                .build();
    }

    /**
     * @date 2026-04-22
     * @desc 일일 지식 조회수 상위 5개를 화면 출력 DTO로 변환합니다.
     */
    private List<AdminTopContentMetricData> mapTopKnowledgeByViewCount() {
        return dailyKnowledgeRepository.findTop5ByOrderByViewCountDescIdDesc().stream()
                .map(knowledge -> AdminTopContentMetricData.builder()
                        .contentType(CONTENT_TYPE_KNOWLEDGE)
                        .contentId(knowledge.getId())
                        .title(knowledge.getTitle())
                        .metricValue(defaultLongValue(knowledge.getViewCount()))
                        .build())
                .toList();
    }

    /**
     * @date 2026-04-22
     * @desc 테크 뉴스 조회수 상위 5개를 화면 출력 DTO로 변환합니다.
     */
    private List<AdminTopContentMetricData> mapTopNewsByViewCount() {
        return techNewsRepository.findTop5ByOrderByViewCountDescIdDesc().stream()
                .map(news -> AdminTopContentMetricData.builder()
                        .contentType(CONTENT_TYPE_NEWS)
                        .contentId(news.getId())
                        .title(news.getTitle())
                        .metricValue(defaultLongValue(news.getViewCount()))
                        .build())
                .toList();
    }

    /**
     * @date 2026-04-22
     * @desc 북마크 집계 결과를 콘텐츠 제목이 포함된 화면 출력 DTO 목록으로 변환합니다.
     */
    private List<AdminTopContentMetricData> mapTopBookmarkedContents(
            List<InsightBookmarkRepository.BookmarkSummaryProjection> topBookmarkedRows
    ) {
        return topBookmarkedRows.stream()
                .map(bookmarkRow -> AdminTopContentMetricData.builder()
                        .contentType(normalizeContentTypeForView(bookmarkRow.getContentType()))
                        .contentId(bookmarkRow.getContentId())
                        .title(resolveContentTitle(bookmarkRow.getContentType(), bookmarkRow.getContentId()))
                        .metricValue(defaultLongValue(bookmarkRow.getBookmarkCount()))
                        .build())
                .toList();
    }

    /**
     * @date 2026-04-22
     * @desc 콘텐츠 타입과 식별자 기반으로 북마크 대상 콘텐츠 제목을 조회합니다.
     */
    private String resolveContentTitle(String contentType, Long contentId) {
        String normalizedContentType = normalizeContentType(contentType);
        if (CONTENT_TYPE_KNOWLEDGE.equals(normalizedContentType)) {
            return dailyKnowledgeRepository.findById(contentId)
                    .map(DailyKnowledge::getTitle)
                    .orElse("삭제되었거나 찾을 수 없는 일일 지식");
        }
        if (CONTENT_TYPE_NEWS.equals(normalizedContentType)) {
            return techNewsRepository.findById(contentId)
                    .map(TechNews::getTitle)
                    .orElse("삭제되었거나 찾을 수 없는 테크 뉴스");
        }
        return "알 수 없는 콘텐츠";
    }

    /**
     * @date 2026-04-22
     * @desc 콘텐츠 타입 문자열을 표준 대문자 형태로 정규화합니다.
     */
    private String normalizeContentType(String contentType) {
        return normalizeOptionalText(contentType).toUpperCase(Locale.ROOT);
    }

    /**
     * @date 2026-04-22
     * @desc 콘텐츠 타입이 비어있거나 미정일 때 화면 표기를 위한 기본값을 보정합니다.
     */
    private String normalizeContentTypeForView(String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        if (normalizedContentType.isBlank()) {
            return "UNKNOWN";
        }
        return normalizedContentType;
    }

    /**
     * @date 2026-04-22
     * @desc null 가능 Long 값을 통계 계산/출력 용도로 0으로 보정합니다.
     */
    private long defaultLongValue(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * @date 2026-04-21
     * @desc 썸네일 업로드 파일을 검증한 뒤 카테고리별 디렉터리에 저장하고 공개 경로를 반환합니다.
     */
    private String storeThumbnailFile(MultipartFile thumbnailFile, String categoryDirectory, LocalDate targetDate) {
        validateThumbnailFile(thumbnailFile);

        String fileExtension = resolveThumbnailExtension(thumbnailFile);
        String dateDirectoryName = targetDate.format(DATE_DIRECTORY_FORMATTER);
        Path uploadRootPath = resolveUploadRootPath();
        Path targetDirectoryPath = uploadRootPath.resolve(categoryDirectory).resolve(dateDirectoryName).normalize();

        if (!targetDirectoryPath.startsWith(uploadRootPath)) {
            throw new IllegalArgumentException("썸네일 저장 경로가 올바르지 않습니다.");
        }

        try {
            Files.createDirectories(targetDirectoryPath);
            String fileName = UUID.randomUUID() + "." + fileExtension;
            Path targetFilePath = targetDirectoryPath.resolve(fileName).normalize();

            if (!targetFilePath.startsWith(uploadRootPath)) {
                throw new IllegalArgumentException("썸네일 저장 경로가 올바르지 않습니다.");
            }

            Files.copy(thumbnailFile.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            return UPLOAD_PUBLIC_PREFIX + categoryDirectory + "/" + dateDirectoryName + "/" + fileName;
        } catch (IOException ioException) {
            throw new IllegalArgumentException("썸네일 파일 저장에 실패했습니다.");
        }
    }

    /**
     * @date 2026-04-21
     * @desc 업로드 파일의 필수 여부, 크기, 이미지 형식 조건을 검증합니다.
     */
    private void validateThumbnailFile(MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            throw new IllegalArgumentException("썸네일 파일은 필수입니다.");
        }
        if (thumbnailFile.getSize() > MAX_THUMBNAIL_FILE_BYTES) {
            throw new IllegalArgumentException("썸네일 파일은 5MB 이하만 업로드할 수 있습니다.");
        }
        if (!isAllowedThumbnailType(thumbnailFile)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, webp, gif)");
        }
    }

    /**
     * @date 2026-04-21
     * @desc 업로드 파일의 Content-Type 또는 파일 확장자가 허용 범위인지 확인합니다.
     */
    private boolean isAllowedThumbnailType(MultipartFile thumbnailFile) {
        String contentType = normalizeOptionalText(thumbnailFile.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.isBlank() && ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            return true;
        }

        String originalFileName = normalizeOptionalText(thumbnailFile.getOriginalFilename());
        if (originalFileName.isBlank() || !originalFileName.contains(".")) {
            return false;
        }
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension);
    }

    /**
     * @date 2026-04-21
     * @desc 업로드 파일 확장자를 결정하되 형식이 불명확하면 jpg 기본값을 사용합니다.
     */
    private String resolveThumbnailExtension(MultipartFile thumbnailFile) {
        String originalFileName = normalizeOptionalText(thumbnailFile.getOriginalFilename());
        if (!originalFileName.isBlank() && originalFileName.contains(".")) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        String contentType = normalizeOptionalText(thumbnailFile.getContentType()).toLowerCase(Locale.ROOT);
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        if ("image/gif".equals(contentType)) {
            return "gif";
        }
        return "jpg";
    }

    /**
     * @date 2026-04-21
     * @desc 공개 경로가 uploads 루트 하위일 때만 실제 파일 삭제를 시도합니다.
     */
    private void deleteStoredThumbnailIfPossible(String thumbnailPublicPath) {
        Path thumbnailFilePath = resolveThumbnailAbsolutePath(thumbnailPublicPath);
        if (thumbnailFilePath == null) {
            return;
        }

        try {
            Files.deleteIfExists(thumbnailFilePath);
        } catch (IOException ignoredException) {
            // 삭제 실패는 게시물 저장 흐름에 영향을 주지 않도록 무시합니다.
        }
    }

    /**
     * @date 2026-04-21
     * @desc 공개 썸네일 경로를 로컬 파일 시스템 절대 경로로 변환합니다.
     */
    private Path resolveThumbnailAbsolutePath(String thumbnailPublicPath) {
        String normalizedPublicPath = normalizeOptionalText(thumbnailPublicPath);
        if (normalizedPublicPath.isBlank() || !normalizedPublicPath.startsWith(UPLOAD_PUBLIC_PREFIX)) {
            return null;
        }

        String relativePath = normalizedPublicPath.substring(UPLOAD_PUBLIC_PREFIX.length());
        Path uploadRootPath = resolveUploadRootPath();
        Path resolvedPath = uploadRootPath.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(uploadRootPath)) {
            return null;
        }
        return resolvedPath;
    }

    /**
     * @date 2026-04-21
     * @desc 썸네일 업로드 루트 디렉터리 절대 경로를 계산합니다.
     */
    private Path resolveUploadRootPath() {
        return Paths.get(thumbnailUploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * @date 2026-04-21
     * @desc 일일 지식 게시물 정보를 보존하면서 썸네일 경로만 교체한 엔티티를 생성합니다.
     */
    private DailyKnowledge buildKnowledgePostWithThumbnailPath(DailyKnowledge originalPost, String thumbnailPath) {
        return DailyKnowledge.builder()
                .id(originalPost.getId())
                .knowledgeDate(originalPost.getKnowledgeDate())
                .category(originalPost.getCategory())
                .title(originalPost.getTitle())
                .attachmentImagePath(thumbnailPath)
                .summary(originalPost.getSummary())
                .detail(originalPost.getDetail())
                .viewCount(originalPost.getViewCount())
                .createdAt(originalPost.getCreatedAt())
                .build();
    }

    /**
     * @date 2026-04-21
     * @desc 테크 뉴스 정보를 보존하면서 썸네일 경로만 교체한 엔티티를 생성합니다.
     */
    private TechNews buildTechNewsWithThumbnailPath(TechNews originalNews, String thumbnailPath) {
        return TechNews.builder()
                .id(originalNews.getId())
                .newsDate(originalNews.getNewsDate())
                .source(originalNews.getSource())
                .title(originalNews.getTitle())
                .url(originalNews.getUrl())
                .attachmentImagePath(thumbnailPath)
                .summary(originalNews.getSummary())
                .viewCount(originalNews.getViewCount())
                .createdAt(originalNews.getCreatedAt())
                .build();
    }

    /**
     * @date 2026-04-15
     * @desc 필수 문자열 값을 공백 제거 후 검증합니다.
     */
    private String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }

    /**
     * @date 2026-04-15
     * @desc 회원 권한 값을 허용 범위로 정규화합니다.
     */
    private String normalizeRole(String role) {
        String normalizedRole = normalizeRequiredValue(role, "권한").toUpperCase(Locale.ROOT);
        if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            throw new IllegalArgumentException("권한은 USER 또는 ADMIN만 허용합니다.");
        }
        return normalizedRole;
    }

    /**
     * @date 2026-04-15
     * @desc 회원 상태 값을 허용 범위로 정규화합니다.
     */
    private String normalizeStatus(String status) {
        String normalizedStatus = normalizeRequiredValue(status, "상태").toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalizedStatus) && !"INACTIVE".equals(normalizedStatus)) {
            throw new IllegalArgumentException("상태는 ACTIVE 또는 INACTIVE만 허용합니다.");
        }
        return normalizedStatus;
    }

    /**
     * @date 2026-04-21
     * @desc 선택 입력값을 공백 제거한 문자열로 정규화하고 null은 빈 문자열로 변환합니다.
     */
    private String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
