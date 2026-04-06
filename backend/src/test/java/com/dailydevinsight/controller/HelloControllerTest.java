package com.dailydevinsight.controller;

import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.service.DailyInsightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HelloController.class)
public class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyInsightService dailyInsightService;

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
                .newsList(List.of())
                .build();

        given(dailyInsightService.getInsightsByDate(any(LocalDate.class))).willReturn(response);

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
}
