package com.dailydevinsight.admin.controller;

import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.admin.service.AdminStatsData;
import com.dailydevinsight.admin.service.DailyKnowledgeGenerationService;
import com.dailydevinsight.admin.service.GenerationHistoryService;
import com.dailydevinsight.admin.service.GenerationScheduleService;
import com.dailydevinsight.admin.service.PromptTemplateService;
import com.dailydevinsight.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminPageController.class)
@Import(SecurityConfig.class)
class AdminPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @MockBean
    private GenerationScheduleService generationScheduleService;

    @MockBean
    private DailyKnowledgeGenerationService dailyKnowledgeGenerationService;

    @MockBean
    private GenerationHistoryService generationHistoryService;

    @MockBean
    private AdminManagementService adminManagementService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoot_ShouldRedirectToDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_ShouldRenderView() throws Exception {
        given(adminManagementService.getAdminStats()).willReturn(AdminStatsData.builder().build());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void generationPage_ShouldRenderView() throws Exception {
        PromptTemplate activeTemplate = PromptTemplate.builder()
                .id(1L)
                .name("default")
                .description("desc")
                .templateContent("content")
                .active(true)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        willDoNothing().given(promptTemplateService).ensureDefaultTemplateExists();
        given(promptTemplateService.findAllTemplates()).willReturn(Collections.singletonList(activeTemplate));
        given(promptTemplateService.findActiveTemplate()).willReturn(java.util.Optional.of(activeTemplate));
        given(generationScheduleService.getOrCreateSchedule()).willReturn(
                GenerationSchedule.builder()
                        .id(1L)
                        .enabled(false)
                        .cronExpression("0 0 9 * * *")
                        .category("Backend")
                        .tone("Practical")
                        .difficulty("Intermediate")
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
        given(generationHistoryService.findRecentHistory()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/generation"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/generation"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void dashboard_ShouldReturnForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void runManualGeneration_ShouldRedirectToGenerationPage() throws Exception {
        given(dailyKnowledgeGenerationService.executeManualGeneration(any())).willReturn(
                GenerationExecutionResult.builder()
                        .success(true)
                        .message("ok")
                        .createdKnowledgeId(10L)
                        .build()
        );

        mockMvc.perform(post("/admin/generate")
                        .with(csrf())
                        .param("targetDate", "2026-04-15")
                        .param("category", "Backend")
                        .param("tone", "Practical")
                        .param("difficulty", "Intermediate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/generation"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void runManualGeneration_ShouldRejectWhenValidationFails() throws Exception {
        mockMvc.perform(post("/admin/generate")
                        .with(csrf())
                        .param("targetDate", "2026-04-15")
                        .param("category", "")
                        .param("tone", "Practical")
                        .param("difficulty", "Intermediate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/generation"));

        verifyNoInteractions(dailyKnowledgeGenerationService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePromptTemplate_ShouldRedirectToGenerationPage() throws Exception {
        willDoNothing().given(promptTemplateService).deleteTemplate(1L);

        mockMvc.perform(post("/admin/prompts/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/generation"));

        verify(promptTemplateService).deleteTemplate(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void togglePromptTemplateActive_ShouldRedirectToGenerationPage() throws Exception {
        willDoNothing().given(promptTemplateService).toggleTemplateActive(1L);

        mockMvc.perform(post("/admin/prompts/1/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/generation"));

        verify(promptTemplateService).toggleTemplateActive(1L);
    }
}
