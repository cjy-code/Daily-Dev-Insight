package com.dailydevinsight.service;

import com.dailydevinsight.dto.InsightCommentDTO;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.entity.InsightComment;
import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.InsightCommentRepository;
import com.dailydevinsight.repository.InsightLikeRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InsightDetailServiceTest {

    private static final String LOGIN_USER_ID = "login-user";
    private static final Long CONTENT_ID = 1L;

    @Mock
    private DailyKnowledgeRepository dailyKnowledgeRepository;

    @Mock
    private TechNewsRepository techNewsRepository;

    @Mock
    private InsightLikeRepository insightLikeRepository;

    @Mock
    private InsightBookmarkRepository insightBookmarkRepository;

    @Mock
    private InsightCommentRepository insightCommentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private MarkdownRenderService markdownRenderService;

    @InjectMocks
    private InsightDetailService insightDetailService;

    /**
     * @date 2026-08-07
     * @desc 상세 댓글 테스트에 공통으로 필요한 로그인 사용자와 콘텐츠 조회 결과를 구성합니다.
     */
    @BeforeEach
    void setUp() {
        User loginUser = buildUser(1L, LOGIN_USER_ID, "로그인 사용자");
        DailyKnowledge knowledge = DailyKnowledge.builder()
                .id(CONTENT_ID)
                .knowledgeDate(LocalDate.of(2026, 8, 7))
                .category("Java")
                .title("테스트 인사이트")
                .summary("요약")
                .detail("본문")
                .viewCount(0L)
                .build();

        given(userRepository.findByUserId(LOGIN_USER_ID)).willReturn(Optional.of(loginUser));
        given(dailyKnowledgeRepository.findById(CONTENT_ID)).willReturn(Optional.of(knowledge));
        given(insightCommentRepository.findByContentTypeAndContentIdOrderByCreatedAtAsc("KNOWLEDGE", CONTENT_ID))
                .willReturn(List.of());
    }

    /**
     * @date 2026-08-07
     * @desc 부모가 이미 대댓글인 경우에도 다음 단계 댓글이 정상 저장되는지 검증합니다.
     */
    @Test
    void addComment_ShouldAllowThirdLevelReply() {
        InsightComment secondLevelComment = buildComment(30L, 2L, 20L, 0, "2단계 댓글");
        given(insightCommentRepository.findById(30L)).willReturn(Optional.of(secondLevelComment));

        insightDetailService.addComment("knowledge", CONTENT_ID, LOGIN_USER_ID, "3단계 댓글", 30L);

        ArgumentCaptor<InsightComment> commentCaptor = ArgumentCaptor.forClass(InsightComment.class);
        verify(insightCommentRepository).save(commentCaptor.capture());
        assertEquals(30L, commentCaptor.getValue().getParentCommentId());
    }

    /**
     * @date 2026-08-07
     * @desc 삭제된 placeholder 댓글에도 답글을 등록할 수 있는지 검증합니다.
     */
    @Test
    void addComment_ShouldAllowReplyToDeletedParent() {
        InsightComment deletedParent = buildComment(10L, 2L, null, 1, "노출되면 안 되는 원문");
        given(insightCommentRepository.findById(10L)).willReturn(Optional.of(deletedParent));

        insightDetailService.addComment("knowledge", CONTENT_ID, LOGIN_USER_ID, "삭제 부모의 답글", 10L);

        ArgumentCaptor<InsightComment> commentCaptor = ArgumentCaptor.forClass(InsightComment.class);
        verify(insightCommentRepository).save(commentCaptor.capture());
        assertEquals(10L, commentCaptor.getValue().getParentCommentId());
    }

    /**
     * @date 2026-08-07
     * @desc 삭제 댓글의 원문과 작성자를 숨기면서 자식 위치와 삭제 leaf를 유지하는지 검증합니다.
     */
    @Test
    void getEngagementOnly_ShouldKeepDeletedCommentsAsPlaceholders() {
        InsightComment deletedParent = buildComment(10L, 99L, null, 1, "부모 비밀 원문");
        InsightComment activeChild = buildComment(11L, 2L, 10L, 0, "자식 댓글");
        InsightComment deletedLeaf = buildComment(12L, 98L, null, 1, "leaf 비밀 원문");
        User activeWriter = buildUser(2L, "active-writer", "활성 작성자");

        given(insightCommentRepository.findByContentTypeAndContentIdOrderByCreatedAtAsc("KNOWLEDGE", CONTENT_ID))
                .willReturn(List.of(deletedParent, activeChild, deletedLeaf));
        given(userRepository.findAllById(any())).willReturn(List.of(activeWriter));

        InsightDetailResponseDTO response = insightDetailService.getEngagementOnly("knowledge", CONTENT_ID, LOGIN_USER_ID);

        assertEquals(2, response.getComments().size());
        InsightCommentDTO deletedParentDto = response.getComments().get(0);
        assertEquals("삭제된 사용자", deletedParentDto.getAuthorName());
        assertEquals("삭제된 댓글입니다.", deletedParentDto.getContent());
        assertTrue(deletedParentDto.isDeleted());
        assertFalse(deletedParentDto.isMine());
        assertEquals(1, deletedParentDto.getReplies().size());
        assertEquals("자식 댓글", deletedParentDto.getReplies().get(0).getContent());
        assertFalse(deletedParentDto.getReplies().get(0).isDeleted());
        assertTrue(response.getComments().get(1).isDeleted());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> writerIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userRepository).findAllById(writerIdsCaptor.capture());
        assertEquals(Set.of(2L), writerIdsCaptor.getValue());
    }

    /**
     * @date 2026-08-07
     * @desc 테스트용 사용자 엔티티를 생성합니다.
     */
    private User buildUser(Long id, String userId, String name) {
        return User.builder()
                .id(id)
                .userId(userId)
                .email(userId + "@example.com")
                .password("password")
                .name(name)
                .role("USER")
                .build();
    }

    /**
     * @date 2026-08-07
     * @desc 테스트용 댓글 엔티티를 생성합니다.
     */
    private InsightComment buildComment(Long id, Long userId, Long parentCommentId, Integer isDeleted, String content) {
        return InsightComment.builder()
                .id(id)
                .contentType("KNOWLEDGE")
                .contentId(CONTENT_ID)
                .userId(userId)
                .content(content)
                .parentCommentId(parentCommentId)
                .isDeleted(isDeleted)
                .createdAt(LocalDateTime.of(2026, 8, 7, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 7, 10, 0))
                .build();
    }
}
