package com.dailydevinsight.controller;

import com.dailydevinsight.config.SecurityConfig;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.service.DailyInsightService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailyInsightRestController.class)
@Import(SecurityConfig.class)
@WithMockUser
class DailyInsightRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyInsightService dailyInsightService;

    @MockBean
    private UserDetailsService userDetailsService;

    /**
     * @date 2026-04-22
     * @desc 미래 date 파라미터 요청 시 오늘 날짜로 보정하여 서비스에 전달하는지 검증합니다.
     */
    @Test
    void getInsights_ShouldClampFutureDateToToday() throws Exception {
        DailyInsightResponseDTO response = DailyInsightResponseDTO.builder()
                .date(LocalDate.now())
                .dailyKnowledgeList(Collections.emptyList())
                .techNewsList(Collections.emptyList())
                .top10List(Collections.emptyList())
                .top5List(Collections.emptyList())
                .newsList(List.of())
                .build();
        given(dailyInsightService.getInsightsByDate(any(LocalDate.class))).willReturn(response);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(4);

        mockMvc.perform(get("/api/insights")
                        .param("date", futureDate.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> targetDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyInsightService).getInsightsByDate(targetDateCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(today, targetDateCaptor.getValue());
    }
}
