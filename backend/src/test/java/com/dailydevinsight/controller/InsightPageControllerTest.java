package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import com.dailydevinsight.admin.service.WeeklyAiInsightService;
import com.dailydevinsight.admin.service.DailyTrendInsightService;
import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.dto.InsightCommentDTO;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.service.DailyInsightService;
import com.dailydevinsight.service.InsightDetailService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(InsightPageController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class InsightPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyInsightService dailyInsightService;

    @MockBean
    private InsightDetailService insightDetailService;

    @MockBean
    private WeeklyAiInsightService weeklyAiInsightService;

    @MockBean
    private DailyTrendInsightService dailyTrendInsightService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void index_ShouldRenderIndexView() throws Exception {
        DailyInsightResponseDTO response = DailyInsightResponseDTO.builder()
                .date(LocalDate.of(2026, 4, 6))
                .todayKnowledge(DailyInsightDTO.builder()
                        .id(1L)
                        .title("Knowledge")
                        .url("https://example.com")
                        .source("Source")
                        .summary("Summary")
                        .publishedAt(LocalDate.of(2026, 4, 6))
                        .build())
                .dailyKnowledgeList(Collections.emptyList())
                .techNewsList(Collections.emptyList())
                .top10List(Collections.emptyList())
                .top5List(Collections.emptyList())
                .newsList(List.of())
                .build();

        given(dailyInsightService.getInsightsByRange(any(LocalDate.class), any(LocalDate.class), any(), any())).willReturn(response);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("response"))
                .andExpect(model().attributeExists("selectedDate"));
    }

    /**
     * @date 2026-04-22
     * @desc 미래 endDate 요청 시 서버에서 오늘 날짜로 보정하여 조회하는지 검증합니다.
     */
    @Test
    void index_ShouldClampFutureEndDateToToday() throws Exception {
        DailyInsightResponseDTO response = DailyInsightResponseDTO.builder()
                .date(LocalDate.now())
                .dailyKnowledgeList(Collections.emptyList())
                .techNewsList(Collections.emptyList())
                .top10List(Collections.emptyList())
                .top5List(Collections.emptyList())
                .newsList(List.of())
                .build();

        given(dailyInsightService.getInsightsByRange(any(LocalDate.class), any(LocalDate.class), any(), any())).willReturn(response);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(3);

        mockMvc.perform(get("/")
                        .param("endDate", futureDate.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> startDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyInsightService, atLeastOnce())
                .getInsightsByRange(startDateCaptor.capture(), endDateCaptor.capture(), any(), any());

        org.junit.jupiter.api.Assertions.assertEquals(today, endDateCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(today.minusMonths(3), startDateCaptor.getValue());
    }

    @Test
    void hello_ShouldReturnHelloView() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("hello"))
                .andExpect(model().attribute("message", "Hello Daily Dev Insight!"));
    }

    @Test
    void insightDetail_ShouldRenderInsightDetailView() throws Exception {
        InsightDetailResponseDTO detail = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .title("detail title")
                .summary("summary")
                .detail("content")
                .source("category")
                .publishedAt(LocalDate.of(2026, 4, 13))
                .viewCount(100L)
                .likeCount(10L)
                .bookmarkCount(3L)
                .comments(Collections.emptyList())
                .build();

        given(insightDetailService.getInsightDetail(anyString(), anyLong(), anyString(), anyBoolean())).willReturn(detail);

        mockMvc.perform(get("/insights/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("insight-detail"))
                .andExpect(model().attributeExists("detail"));
    }

    /**
     * @date 2026-08-07
     * @desc SSR 댓글 재귀 구조에 삭제 placeholder와 depth cap 클래스가 반영되는지 검증합니다.
     */
    @Test
    void insightDetail_ShouldRenderRecursiveCommentTree() throws Exception {
        InsightCommentDTO commentTree = buildCommentTree(0, 5);
        InsightDetailResponseDTO detail = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .title("detail title")
                .summary("summary")
                .detail("content")
                .source("category")
                .publishedAt(LocalDate.of(2026, 4, 13))
                .viewCount(100L)
                .likeCount(10L)
                .bookmarkCount(3L)
                .comments(List.of(commentTree))
                .build();

        given(insightDetailService.getInsightDetail(anyString(), anyLong(), anyString(), anyBoolean())).willReturn(detail);

        MvcResult result = mockMvc.perform(get("/insights/knowledge/1"))
                .andExpect(status().isOk())
                .andReturn();
        Document document = Jsoup.parse(result.getResponse().getContentAsString());
        Element deletedRoot = document.selectFirst("li[data-comment-id=100]");

        assertNotNull(deletedRoot);
        assertTrue(deletedRoot.hasClass("depth-0"));
        assertTrue(deletedRoot.hasClass("comment-item-deleted"));
        Element deletedMeta = deletedRoot.children().stream()
                .filter(element -> element.hasClass("comment-meta"))
                .findFirst()
                .orElse(null);
        Element deletedContent = deletedRoot.children().stream()
                .filter(element -> "p".equals(element.tagName()))
                .findFirst()
                .orElse(null);
        assertNotNull(deletedMeta);
        assertNotNull(deletedContent);
        assertEquals("삭제된 사용자", deletedMeta.selectFirst("strong").text());
        assertEquals("삭제된 댓글입니다.", deletedContent.text());

        Element deletedActions = deletedRoot.children().stream()
                .filter(element -> element.hasClass("comment-actions"))
                .findFirst()
                .orElse(null);
        assertNotNull(deletedActions);
        assertNotNull(deletedActions.selectFirst("[data-comment-reply]"));
        assertNull(deletedActions.selectFirst("[data-comment-delete]"));
        assertEquals(2, document.select("li.depth-4").size());
        assertEquals(1, document.select("[data-comment-list] > li.comment-item").size());
    }

    /**
     * @date 2026-04-17
     * @desc ??⑤㈇????瑜곷턄嶺뚯솘? ?브퀗??????브퀗????嶺뚯빘鍮? ????뗥윜諛멥늾? ??疫?true???熬곣뫀堉??濡ル츎嶺뚯솘? ?롪틵?嶺뚯빘鍮쒒뜮????덈펲.
     */
    @Test
    void insightDetail_ShouldIncreaseViewCountOnlyOncePerSession() throws Exception {
        InsightDetailResponseDTO detail = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .title("detail title")
                .summary("summary")
                .detail("content")
                .source("category")
                .publishedAt(LocalDate.of(2026, 4, 13))
                .viewCount(100L)
                .likeCount(10L)
                .bookmarkCount(3L)
                .comments(Collections.emptyList())
                .build();

        given(insightDetailService.getInsightDetail(anyString(), anyLong(), anyString(), anyBoolean())).willReturn(detail);

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/insights/knowledge/1").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/insights/knowledge/1").session(session))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> increaseFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(insightDetailService, times(2))
                .getInsightDetail(anyString(), anyLong(), anyString(), increaseFlagCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, increaseFlagCaptor.getAllValues().get(0));
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.FALSE, increaseFlagCaptor.getAllValues().get(1));
    }

    /**
     * @date 2026-08-07
     * @desc 지정 깊이까지 연결된 SSR 검증용 댓글 DTO 트리를 생성합니다.
     */
    private InsightCommentDTO buildCommentTree(int depth, int maxDepth) {
        boolean deleted = depth == 0;
        List<InsightCommentDTO> replies = depth < maxDepth
                ? List.of(buildCommentTree(depth + 1, maxDepth))
                : List.of();

        return InsightCommentDTO.builder()
                .id(100L + depth)
                .parentCommentId(depth == 0 ? null : 99L + depth)
                .authorName(deleted ? "삭제된 사용자" : "작성자 " + depth)
                .content(deleted ? "삭제된 댓글입니다." : "댓글 " + depth)
                .createdAt(LocalDateTime.of(2026, 8, 7, 10, depth))
                .mine(!deleted)
                .deleted(deleted)
                .replies(replies)
                .build();
    }
}
