package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final DailyKnowledgeRepository dailyKnowledgeRepository;
    private final UserRepository userRepository;
    private final GenerationHistoryRepository generationHistoryRepository;

    /**
     * @date 2026-04-15
     * @desc 관리자 게시물 관리 화면에 노출할 최근 게시물 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<DailyKnowledge> findRecentKnowledgePosts() {
        return dailyKnowledgeRepository.findTop30ByOrderByKnowledgeDateDescIdDesc();
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 회원 관리 화면에 노출할 최근 회원 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<User> findRecentUsers() {
        return userRepository.findTop30ByOrderByCreatedAtDesc();
    }

    /**
     * @date 2026-04-15
     * @desc 게시물의 카테고리와 제목을 수정합니다.
     */
    @Transactional
    public void updateKnowledgePost(Long postId, String category, String title) {
        DailyKnowledge originalPost = dailyKnowledgeRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        DailyKnowledge updatedPost = DailyKnowledge.builder()
                .id(originalPost.getId())
                .knowledgeDate(originalPost.getKnowledgeDate())
                .category(normalizeRequiredValue(category, "카테고리"))
                .title(normalizeRequiredValue(title, "제목"))
                .summary(originalPost.getSummary())
                .detail(originalPost.getDetail())
                .viewCount(originalPost.getViewCount())
                .createdAt(originalPost.getCreatedAt())
                .build();

        dailyKnowledgeRepository.save(updatedPost);
    }

    /**
     * @date 2026-04-15
     * @desc 게시물을 삭제합니다.
     */
    @Transactional
    public void deleteKnowledgePost(Long postId) {
        if (!dailyKnowledgeRepository.existsById(postId)) {
            throw new IllegalArgumentException("삭제할 게시물을 찾을 수 없습니다.");
        }
        dailyKnowledgeRepository.deleteById(postId);
    }

    /**
     * @date 2026-04-15
     * @desc 회원의 권한과 상태를 수정합니다.
     */
    @Transactional
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
     * @desc 관리자 대시보드 통계 수치를 계산합니다.
     */
    @Transactional(readOnly = true)
    public AdminStatsData getAdminStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatusIgnoreCase("ACTIVE");
        long totalPosts = dailyKnowledgeRepository.count();
        long todayPosts = dailyKnowledgeRepository.countByKnowledgeDate(LocalDate.now());
        long generationSuccessCount = generationHistoryRepository.countByStatus("SUCCESS");
        long generationFailedCount = generationHistoryRepository.countByStatus("FAILED");

        return AdminStatsData.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalPosts(totalPosts)
                .todayPosts(todayPosts)
                .generationSuccessCount(generationSuccessCount)
                .generationFailedCount(generationFailedCount)
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
        String normalizedRole = normalizeRequiredValue(role, "권한").toUpperCase();
        if (!"USER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            throw new IllegalArgumentException("권한은 USER 또는 ADMIN만 허용됩니다.");
        }
        return normalizedRole;
    }

    /**
     * @date 2026-04-15
     * @desc 회원 상태 값을 허용 범위로 정규화합니다.
     */
    private String normalizeStatus(String status) {
        String normalizedStatus = normalizeRequiredValue(status, "상태").toUpperCase();
        if (!"ACTIVE".equals(normalizedStatus) && !"INACTIVE".equals(normalizedStatus)) {
            throw new IllegalArgumentException("상태는 ACTIVE 또는 INACTIVE만 허용됩니다.");
        }
        return normalizedStatus;
    }
}
