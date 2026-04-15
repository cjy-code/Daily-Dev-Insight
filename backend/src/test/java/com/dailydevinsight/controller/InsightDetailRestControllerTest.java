package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import com.dailydevinsight.dto.InsightCommentRequestDTO;
import com.dailydevinsight.dto.InsightDetailResponseDTO;
import com.dailydevinsight.dto.InsightToggleResponseDTO;
import com.dailydevinsight.service.InsightDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightDetailRestController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class InsightDetailRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InsightDetailService insightDetailService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getDetailState_ShouldReturnOk() throws Exception {
        InsightDetailResponseDTO response = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .title("?곸꽭 ?쒕ぉ")
                .summary("?붿빟")
                .detail("蹂몃Ц")
                .source("移댄뀒怨좊━")
                .publishedAt(LocalDate.of(2026, 4, 13))
                .viewCount(11L)
                .likeCount(2L)
                .bookmarkCount(1L)
                .comments(Collections.emptyList())
                .build();

        given(insightDetailService.getEngagementOnly(anyString(), anyLong(), anyString())).willReturn(response);

        mockMvc.perform(get("/api/insights/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.viewCount").value(11));
    }

    @Test
    void toggleLike_ShouldReturnCount() throws Exception {
        InsightToggleResponseDTO response = InsightToggleResponseDTO.builder()
                .active(true)
                .count(3L)
                .build();

        given(insightDetailService.toggleLike(anyString(), anyLong(), anyString())).willReturn(response);

        mockMvc.perform(post("/api/insights/knowledge/1/likes/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void addComment_ShouldReturnUpdatedState() throws Exception {
        InsightDetailResponseDTO response = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .comments(Collections.emptyList())
                .build();

        given(insightDetailService.addComment(anyString(), anyLong(), anyString(), anyString(), any())).willReturn(response);

        mockMvc.perform(post("/api/insights/knowledge/1/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InsightCommentRequestDTO("댓글", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteComment_ShouldReturnUpdatedState() throws Exception {
        InsightDetailResponseDTO response = InsightDetailResponseDTO.builder()
                .type("knowledge")
                .id(1L)
                .comments(Collections.emptyList())
                .build();

        given(insightDetailService.deleteComment(anyString(), anyLong(), anyLong(), anyString())).willReturn(response);

        mockMvc.perform(delete("/api/insights/knowledge/1/comments/10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
