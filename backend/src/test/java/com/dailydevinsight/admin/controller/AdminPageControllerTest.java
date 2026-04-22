package com.dailydevinsight.admin.controller;

import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.CrawlPreviewResponse;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.admin.service.AdminStatsData;
import com.dailydevinsight.admin.service.CrawlHistoryService;
import com.dailydevinsight.admin.service.CrawlConditionPresetService;
import com.dailydevinsight.admin.service.CrawlScheduleService;
import com.dailydevinsight.admin.service.DailyKnowledgeGenerationService;
import com.dailydevinsight.admin.service.GenerationHistoryService;
import com.dailydevinsight.admin.service.GenerationScheduleService;
import com.dailydevinsight.admin.service.PromptTemplateService;
import com.dailydevinsight.admin.service.TechNewsCrawlingService;
import com.dailydevinsight.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private CrawlScheduleService crawlScheduleService;

    @MockBean
    private CrawlHistoryService crawlHistoryService;

    @MockBean
    private CrawlConditionPresetService crawlConditionPresetService;

    @MockBean
    private TechNewsCrawlingService techNewsCrawlingService;

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
    void postsRoot_ShouldRedirectToKnowledgePage() throws Exception {
        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/knowledge"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postsKnowledgePage_ShouldRenderView() throws Exception {
        given(adminManagementService.findRecentKnowledgePosts()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/posts/knowledge"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/posts-knowledge"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postsNewsPage_ShouldRenderView() throws Exception {
        given(adminManagementService.findRecentTechNewsPosts()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/posts/news"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/posts-news"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadKnowledgeThumbnail_ShouldRedirectToKnowledgePage() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "thumb.jpg",
                "image/jpeg",
                "thumbnail".getBytes()
        );

        willDoNothing().given(adminManagementService).updateKnowledgeThumbnail(org.mockito.ArgumentMatchers.eq(1L), any());

        mockMvc.perform(multipart("/admin/posts/knowledge/1/thumbnail")
                        .file(thumbnailFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/knowledge"));

        verify(adminManagementService).updateKnowledgeThumbnail(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteKnowledgeThumbnail_ShouldRedirectToKnowledgePage() throws Exception {
        willDoNothing().given(adminManagementService).deleteKnowledgeThumbnail(1L);

        mockMvc.perform(post("/admin/posts/knowledge/1/thumbnail/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/knowledge"));

        verify(adminManagementService).deleteKnowledgeThumbnail(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadTechNewsThumbnail_ShouldRedirectToNewsPage() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "thumb.jpg",
                "image/jpeg",
                "thumbnail".getBytes()
        );

        willDoNothing().given(adminManagementService).updateTechNewsThumbnail(org.mockito.ArgumentMatchers.eq(1L), any());

        mockMvc.perform(multipart("/admin/posts/news/1/thumbnail")
                        .file(thumbnailFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/news"));

        verify(adminManagementService).updateTechNewsThumbnail(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTechNewsThumbnail_ShouldRedirectToNewsPage() throws Exception {
        willDoNothing().given(adminManagementService).deleteTechNewsThumbnail(1L);

        mockMvc.perform(post("/admin/posts/news/1/thumbnail/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/news"));

        verify(adminManagementService).deleteTechNewsThumbnail(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadKnowledgeThumbnail_ShouldRejectForUserRole() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "thumb.jpg",
                "image/jpeg",
                "thumbnail".getBytes()
        );

        mockMvc.perform(multipart("/admin/posts/knowledge/1/thumbnail")
                        .file(thumbnailFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?adminDenied=true"));

        verifyNoInteractions(adminManagementService);
    }

    /**
     * @date 2026-04-22
     * @desc CSRF 토큰 없이 썸네일 업로드 요청 시 기존 관리자 페이지로 csrfError 쿼리와 함께 리다이렉트하는지 검증합니다.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadKnowledgeThumbnail_WithoutCsrf_ShouldRedirectToKnowledgePageWithCsrfError() throws Exception {
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "thumb.jpg",
                "image/jpeg",
                "thumbnail".getBytes()
        );

        mockMvc.perform(multipart("/admin/posts/knowledge/1/thumbnail")
                        .file(thumbnailFile)
                        .header("Referer", "http://localhost/admin/posts/knowledge"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/admin/posts/knowledge?csrfError=true"));

        verifyNoInteractions(adminManagementService);
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
    @WithMockUser(roles = "ADMIN")
    void crawlingPage_ShouldRenderView() throws Exception {
        given(crawlScheduleService.getOrCreateSchedule()).willReturn(
                CrawlSchedule.builder()
                        .id(1L)
                        .enabled(false)
                        .cronExpression("0 0 8 * * *")
                        .sourceName("Hacker News")
                        .sourceUrl("https://hnrss.org/frontpage")
                        .maxArticles(20)
                        .keywordMatchType("OR")
                        .includeKeywords(null)
                        .targetDomains(null)
                        .connectTimeoutSeconds(5)
                        .readTimeoutSeconds(5)
                        .retryCount(1)
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
        given(crawlConditionPresetService.findActivePresets()).willReturn(Collections.emptyList());
        given(crawlHistoryService.findRecentHistory()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/crawling"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/crawling"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void dashboard_ShouldReturnForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?adminDenied=true"));
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void previewManualCrawling_ShouldReturnPreviewResponse() throws Exception {
        given(techNewsCrawlingService.previewManualCrawling(any())).willReturn(
                CrawlPreviewResponse.builder()
                        .success(true)
                        .message("ok")
                        .collectedCount(3)
                        .filteredCount(2)
                        .previewItems(Collections.emptyList())
                        .build()
        );

        mockMvc.perform(post("/admin/crawling/preview")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetDate":"2026-04-17",
                                  "sourceName":"Hacker News",
                                  "sourceUrl":"https://hnrss.org/frontpage",
                                  "maxArticles":20,
                                  "keywordMatchType":"OR",
                                  "includeKeywords":["spring"],
                                  "includeKeywordOperators":[],
                                  "excludeKeywords":[],
                                  "targetDomains":[],
                                  "connectTimeoutSeconds":5,
                                  "readTimeoutSeconds":5,
                                  "retryCount":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filteredCount").value(2));
    }
}
