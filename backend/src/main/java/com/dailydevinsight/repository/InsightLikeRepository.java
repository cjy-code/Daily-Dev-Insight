package com.dailydevinsight.repository;

import com.dailydevinsight.entity.InsightLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsightLikeRepository extends JpaRepository<InsightLike, Long> {

    Optional<InsightLike> findByContentTypeAndContentIdAndUserId(String contentType, Long contentId, Long userId);

    long countByContentTypeAndContentId(String contentType, Long contentId);

    List<InsightLike> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
