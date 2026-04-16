package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.service.DailyInsightService;
import com.dailydevinsight.service.InsightDetailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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
     * @date 2026-04-17
     * @desc ??⑤㈇????瑜곷턄嶺뚯솘? ?브퀗??????브퀗????嶺뚯빘鍮? ????뗥윜諛멥늾? ??疫?true???熬곣뫀堉??濡ル츎嶺뚯솘? ?롪틵?嶺뚯빘鍮쒒뜮????덈펲.
     */
    @Test
    void insightDetail_ShouldAlwaysPassTrueForViewCountIncrease() throws Exception {
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
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> increaseFlagCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(insightDetailService)
                .getInsightDetail(anyString(), anyLong(), anyString(), increaseFlagCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, increaseFlagCaptor.getValue());
    }
}
