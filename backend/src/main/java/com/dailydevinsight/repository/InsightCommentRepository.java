package com.dailydevinsight.repository;

import com.dailydevinsight.entity.InsightComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsightCommentRepository extends JpaRepository<InsightComment, Long> {

    List<InsightComment> findByContentTypeAndContentIdAndIsDeletedOrderByCreatedAtDesc(String contentType, Long contentId, Integer isDeleted);
    List<InsightComment> findByContentTypeAndContentIdAndIsDeletedOrderByCreatedAtAsc(String contentType, Long contentId, Integer isDeleted);
    List<InsightComment> findByContentTypeAndContentIdOrderByCreatedAtAsc(String contentType, Long contentId);

    long countByContentTypeAndContentIdAndIsDeleted(String contentType, Long contentId, Integer isDeleted);

    Optional<InsightComment> findByIdAndContentTypeAndContentIdAndIsDeleted(Long id, String contentType, Long contentId, Integer isDeleted);
}
