package com.dailydevinsight.repository;

import com.dailydevinsight.entity.InsightBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsightBookmarkRepository extends JpaRepository<InsightBookmark, Long> {

    Optional<InsightBookmark> findByContentTypeAndContentIdAndUserId(String contentType, Long contentId, Long userId);

    long countByContentTypeAndContentId(String contentType, Long contentId);
}
