package com.dailydevinsight.repository;

import com.dailydevinsight.entity.InsightBookmark;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsightBookmarkRepository extends JpaRepository<InsightBookmark, Long> {

    Optional<InsightBookmark> findByContentTypeAndContentIdAndUserId(String contentType, Long contentId, Long userId);

    long countByContentTypeAndContentId(String contentType, Long contentId);

    Page<InsightBookmark> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            select b.contentType as contentType, b.contentId as contentId, count(b.id) as bookmarkCount
            from InsightBookmark b
            group by b.contentType, b.contentId
            order by count(b.id) desc
            """)
    List<BookmarkSummaryProjection> findTopBookmarkedContents(Pageable pageable);

    @Query("select count(distinct b.userId) from InsightBookmark b")
    long countDistinctUserId();

    void deleteByUserId(Long userId);

    interface BookmarkSummaryProjection {
        String getContentType();

        Long getContentId();

        Long getBookmarkCount();
    }
}
