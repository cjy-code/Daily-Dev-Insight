package com.dailydevinsight.admin.controller;

import com.dailydevinsight.admin.dto.CrawlRunForm;
import com.dailydevinsight.admin.dto.CrawlScheduleForm;
import com.dailydevinsight.admin.dto.CrawlPresetForm;
import com.dailydevinsight.admin.dto.CrawlPreviewResponse;
import com.dailydevinsight.admin.dto.GenerationRequestForm;
import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.GenerationImageRefreshRequest;
import com.dailydevinsight.admin.dto.GenerationImageRefreshResponse;
import com.dailydevinsight.admin.dto.GenerationPreviewRequest;
import com.dailydevinsight.admin.dto.GenerationPreviewResponse;
import com.dailydevinsight.admin.dto.GenerationSaveRequest;
import com.dailydevinsight.admin.dto.PromptTemplateForm;
import com.dailydevinsight.admin.dto.ScheduleForm;
import com.dailydevinsight.admin.entity.CrawlSchedule;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.admin.service.CrawlHistoryService;
import com.dailydevinsight.admin.service.CrawlConditionPresetService;
import com.dailydevinsight.admin.service.CrawlScheduleService;
import com.dailydevinsight.admin.service.DailyKnowledgeGenerationService;
import com.dailydevinsight.admin.service.GenerationHistoryService;
import com.dailydevinsight.admin.service.GenerationScheduleService;
import com.dailydevinsight.admin.service.PromptTemplateService;
import com.dailydevinsight.admin.service.TechNewsCrawlingService;
import com.dailydevinsight.admin.service.WeeklyAiInsightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Collections;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private static final String MENU_DASHBOARD = "dashboard";
    private static final String MENU_POSTS_KNOWLEDGE = "posts-knowledge";
    private static final String MENU_POSTS_NEWS = "posts-news";
    private static final String MENU_MEMBERS = "members";
    private static final String MENU_GENERATION = "generation";
    private static final String MENU_CRAWLING = "crawling";
    private static final String MENU_STATS_VIEWS = "stats-views";
    private static final String MENU_STATS_BOOKMARKS = "stats-bookmarks";

    private final PromptTemplateService promptTemplateService;
    private final GenerationScheduleService generationScheduleService;
    private final DailyKnowledgeGenerationService dailyKnowledgeGenerationService;
    private final GenerationHistoryService generationHistoryService;
    private final AdminManagementService adminManagementService;
    private final CrawlScheduleService crawlScheduleService;
    private final CrawlHistoryService crawlHistoryService;
    private final CrawlConditionPresetService crawlConditionPresetService;
    private final TechNewsCrawlingService techNewsCrawlingService;
    private final WeeklyAiInsightService weeklyAiInsightService;

    /**
     * @date 2026-04-15
     * @desc 관리자 기본 진입 경로를 대시보드 화면으로 리다이렉트합니다.
     */
    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 대시보드 페이지를 렌더링합니다.
     */
    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("currentMenu", MENU_DASHBOARD);
        model.addAttribute("stats", adminManagementService.getAdminStats());
        return "admin/dashboard";
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 통계 메뉴의 기본 경로를 조회수 통계 화면으로 리다이렉트합니다.
     */
    @GetMapping("/stats")
    public String statsRootPage() {
        return "redirect:/admin/stats/views";
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 조회수 통계 화면을 렌더링합니다.
     */
    @GetMapping("/stats/views")
    public String statsViewsPage(Model model) {
        model.addAttribute("currentMenu", MENU_STATS_VIEWS);
        model.addAttribute("viewStats", adminManagementService.getContentViewStats());
        return "admin/stats-views";
    }

    /**
     * @date 2026-04-22
     * @desc 관리자 북마크 통계 화면을 렌더링합니다.
     */
    @GetMapping("/stats/bookmarks")
    public String statsBookmarksPage(Model model) {
        model.addAttribute("currentMenu", MENU_STATS_BOOKMARKS);
        model.addAttribute("bookmarkStats", adminManagementService.getBookmarkStats());
        return "admin/stats-bookmarks";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 게시물 관리 페이지를 렌더링합니다.
     */
    @GetMapping("/posts")
    public String postsRootPage() {
        return "redirect:/admin/posts/knowledge";
    }

    /**
     * @date 2026-08-06
     * @desc 일일 지식 게시물 관리 페이지를 페이지 단위로 렌더링합니다.
     */
    @GetMapping("/posts/knowledge")
    public String postsKnowledgePage(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("currentMenu", MENU_POSTS_KNOWLEDGE);
        model.addAttribute("knowledgePostPage", adminManagementService.findKnowledgePosts(page));
        return "admin/posts-knowledge";
    }

    /**
     * @date 2026-08-06
     * @desc 테크 뉴스 게시물 관리 페이지를 페이지 단위로 렌더링합니다.
     */
    @GetMapping("/posts/news")
    public String postsNewsPage(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("currentMenu", MENU_POSTS_NEWS);
        model.addAttribute("techNewsPostPage", adminManagementService.findTechNewsPosts(page));
        return "admin/posts-news";
    }

    /**
     * @date 2026-08-06
     * @desc 관리자 회원 관리 페이지를 페이지 단위로 렌더링합니다.
     */
    @GetMapping("/members")
    public String membersPage(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("currentMenu", MENU_MEMBERS);
        model.addAttribute("memberPage", adminManagementService.findUsers(page));
        return "admin/members";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 생성 관리 페이지를 렌더링합니다.
     */
    @GetMapping("/generation")
    public String generationPage(Model model) {
        promptTemplateService.ensureDefaultTemplateExists();

        model.addAttribute("currentMenu", MENU_GENERATION);
        model.addAttribute("templateList", promptTemplateService.findAllTemplates());
        model.addAttribute("activeTemplate", promptTemplateService.findActiveTemplate().orElse(null));
        model.addAttribute("promptTemplateForm", new PromptTemplateForm());
        model.addAttribute("generationRequestForm", createDefaultGenerationRequestForm());
        model.addAttribute("scheduleForm", toScheduleForm(generationScheduleService.getOrCreateSchedule()));
        model.addAttribute("historyList", generationHistoryService.findRecentHistory());
        return "admin/generation";
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 크롤링 관리 페이지를 렌더링합니다.
     */
    @GetMapping("/crawling")
    public String crawlingPage(Model model) {
        CrawlSchedule crawlSchedule = crawlScheduleService.getOrCreateSchedule();

        model.addAttribute("currentMenu", MENU_CRAWLING);
        model.addAttribute("crawlPresetList", crawlConditionPresetService.findActivePresets());
        model.addAttribute("crawlPresetForm", createDefaultCrawlPresetForm());
        model.addAttribute("crawlRunForm", createDefaultCrawlRunForm(crawlSchedule));
        model.addAttribute("crawlScheduleForm", toCrawlScheduleForm(crawlSchedule));
        model.addAttribute("crawlHistoryList", crawlHistoryService.findRecentHistory());
        model.addAttribute("weeklyAiInsight", weeklyAiInsightService.findLatestInsightForAdmin());
        model.addAttribute("weeklyAiInsightList", weeklyAiInsightService.findRecentInsightsForAdmin());
        model.addAttribute("weeklyAiInsightReferenceDate", LocalDate.now());
        return "admin/crawling";
    }

    /**
     * @date 2026-04-15
     * @desc 게시물의 기본 정보(카테고리/제목)를 수정합니다.
     */
    @PostMapping("/posts/knowledge/{id}/update")
    public String updateKnowledgePost(
            @PathVariable("id") Long postId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/knowledge",
                "게시물 정보가 수정되었습니다.",
                () -> adminManagementService.updateKnowledgePost(postId, category, title)
        );
    }

    /**
     * @date 2026-04-15
     * @desc 게시물을 삭제합니다.
     */
    @PostMapping("/posts/knowledge/{id}/delete")
    public String deleteKnowledgePost(
            @PathVariable("id") Long postId,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/knowledge",
                "게시물이 삭제되었습니다.",
                () -> adminManagementService.deleteKnowledgePost(postId)
        );
    }

    /**
     * @date 2026-04-21
     * @desc 일일 지식 게시물의 썸네일 첨부파일을 업로드합니다.
     */
    @PostMapping("/posts/knowledge/{id}/thumbnail")
    public String uploadKnowledgeThumbnail(
            @PathVariable("id") Long postId,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/knowledge",
                "일일 지식 썸네일이 업로드되었습니다.",
                () -> adminManagementService.updateKnowledgeThumbnail(postId, thumbnailFile)
        );
    }

    /**
     * @date 2026-04-21
     * @desc 일일 지식 게시물의 썸네일 첨부파일을 삭제합니다.
     */
    @PostMapping("/posts/knowledge/{id}/thumbnail/delete")
    public String deleteKnowledgeThumbnail(
            @PathVariable("id") Long postId,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/knowledge",
                "일일 지식 썸네일이 삭제되었습니다.",
                () -> adminManagementService.deleteKnowledgeThumbnail(postId)
        );
    }

    /**
     * @date 2026-04-17
     * @desc 테크 뉴스 게시물의 출처/제목을 수정합니다.
     */
    @PostMapping("/posts/news/{id}/update")
    public String updateTechNewsPost(
            @PathVariable("id") Long newsId,
            @RequestParam("source") String source,
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/news",
                "테크 뉴스 정보가 수정되었습니다.",
                () -> adminManagementService.updateTechNewsPost(newsId, source, title)
        );
    }

    /**
     * @date 2026-04-17
     * @desc 테크 뉴스 게시물을 삭제합니다.
     */
    @PostMapping("/posts/news/{id}/delete")
    public String deleteTechNewsPost(
            @PathVariable("id") Long newsId,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/news",
                "테크 뉴스 게시물이 삭제되었습니다.",
                () -> adminManagementService.deleteTechNewsPost(newsId)
        );
    }

    /**
     * @date 2026-04-21
     * @desc 테크 뉴스 게시물의 썸네일 첨부파일을 업로드합니다.
     */
    @PostMapping("/posts/news/{id}/thumbnail")
    public String uploadTechNewsThumbnail(
            @PathVariable("id") Long newsId,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/news",
                "테크 뉴스 썸네일이 업로드되었습니다.",
                () -> adminManagementService.updateTechNewsThumbnail(newsId, thumbnailFile)
        );
    }

    /**
     * @date 2026-04-21
     * @desc 테크 뉴스 게시물의 썸네일 첨부파일을 삭제합니다.
     */
    @PostMapping("/posts/news/{id}/thumbnail/delete")
    public String deleteTechNewsThumbnail(
            @PathVariable("id") Long newsId,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/posts/news",
                "테크 뉴스 썸네일이 삭제되었습니다.",
                () -> adminManagementService.deleteTechNewsThumbnail(newsId)
        );
    }

    /**
     * @date 2026-04-15
     * @desc 회원의 권한과 상태를 수정합니다.
     */
    @PostMapping("/members/{id}/update")
    public String updateMember(
            @PathVariable("id") Long userPrimaryKey,
            @RequestParam("role") String role,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/members",
                "회원 정보가 수정되었습니다.",
                () -> adminManagementService.updateUser(userPrimaryKey, role, status)
        );
    }

    /**
     * @date 2026-04-15
     * @desc 프롬프트 템플릿 신규 등록 또는 수정 저장을 처리합니다.
     */
    @PostMapping("/prompts")
    public String savePromptTemplate(PromptTemplateForm promptTemplateForm, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/generation",
                "프롬프트 템플릿이 저장되었습니다.",
                () -> promptTemplateService.saveTemplate(promptTemplateForm)
        );
    }

    /**
     * @date 2026-04-15
     * @desc 선택한 프롬프트 템플릿을 활성화합니다.
     */
    @PostMapping("/prompts/{id}/activate")
    public String activatePromptTemplate(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/generation",
                "활성 프롬프트가 변경되었습니다.",
                () -> promptTemplateService.activateTemplate(templateId)
        );
    }

    /**
     * @date 2026-04-16
     * @desc 선택한 프롬프트 템플릿의 활성 상태를 토글합니다.
     */
    @PostMapping("/prompts/{id}/toggle-active")
    public String togglePromptTemplateActive(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/generation",
                "프롬프트 활성 상태가 변경되었습니다.",
                () -> promptTemplateService.toggleTemplateActive(templateId)
        );
    }

    /**
     * @date 2026-04-16
     * @desc 선택한 프롬프트 템플릿을 삭제합니다.
     */
    @PostMapping("/prompts/{id}/delete")
    public String deletePromptTemplate(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/generation",
                "프롬프트 템플릿이 삭제되었습니다.",
                () -> promptTemplateService.deleteTemplate(templateId)
        );
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 수동 실행으로 일일 개발 지식을 생성합니다.
     */
    @PostMapping("/generate")
    public String runManualGeneration(
            @Valid GenerationRequestForm generationRequestForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            String validationMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("adminError", validationMessage);
            return "redirect:/admin/generation";
        }

        return executeAdminAction(redirectAttributes, "/admin/generation", null, () -> {
            var executionResult = dailyKnowledgeGenerationService.executeManualGeneration(generationRequestForm);
            if (executionResult.isSuccess()) {
                redirectAttributes.addFlashAttribute(
                        "adminMessage",
                        "일일 개발 지식 생성 완료 (ID: " + executionResult.getCreatedKnowledgeId() + ")"
                );
            } else {
                if (executionResult.getErrorCode() != null && !executionResult.getErrorCode().isBlank()) {
                    redirectAttributes.addFlashAttribute("adminErrorCode", executionResult.getErrorCode());
                }
                redirectAttributes.addFlashAttribute("adminError", executionResult.getMessage());
            }
        });
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창 페이지를 렌더링하고 현재 활성 템플릿을 프롬프트로 주입합니다.
     */
    @GetMapping("/generation/compose")
    public String generationComposePage(
            @RequestParam("targetDate") LocalDate targetDate,
            @RequestParam("category") String category,
            @RequestParam("tone") String tone,
            @RequestParam("difficulty") String difficulty,
            Model model
    ) {
        PromptTemplate activeTemplate = promptTemplateService.findActiveTemplate().orElse(null);
        String renderedPrompt = "";
        if (activeTemplate != null) {
            renderedPrompt = dailyKnowledgeGenerationService.buildRenderedPromptForManual(
                    targetDate,
                    category,
                    tone,
                    difficulty
            );
        }

        model.addAttribute("targetDate", targetDate);
        model.addAttribute("category", category);
        model.addAttribute("tone", tone);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("activeTemplate", activeTemplate);
        model.addAttribute("renderedPrompt", renderedPrompt);
        model.addAttribute("imagePromptTemplate", dailyKnowledgeGenerationService.buildImagePromptTemplateForManual(activeTemplate));
        return "admin/generation-compose";
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창에서 LLM 미리보기 결과를 비동기로 생성합니다.
     */
    @PostMapping("/generate/preview")
    @ResponseBody
    public ResponseEntity<GenerationPreviewResponse> previewManualGeneration(@RequestBody GenerationPreviewRequest request) {
        return ResponseEntity.ok(dailyKnowledgeGenerationService.previewManualGeneration(request));
    }

    /**
     * @date 2026-04-24
     * @desc 수동 생성 확인 화면에서 LLM 결과 이미지 미리보기를 다시 생성합니다.
     */
    @PostMapping("/generate/preview/image-refresh")
    @ResponseBody
    public ResponseEntity<GenerationImageRefreshResponse> refreshPreviewImage(@RequestBody GenerationImageRefreshRequest request) {
        return ResponseEntity.ok(dailyKnowledgeGenerationService.refreshPreviewImage(request));
    }

    /**
     * @date 2026-04-16
     * @desc 수동 생성 새창에서 확인된 LLM 결과를 최종 저장합니다.
     */
    @PostMapping("/generate/save")
    @ResponseBody
    public ResponseEntity<GenerationExecutionResult> saveManualGeneration(@RequestBody GenerationSaveRequest request) {
        return ResponseEntity.ok(dailyKnowledgeGenerationService.saveManualGenerationFromPreview(request));
    }

    /**
     * @date 2026-04-15
     * @desc 예약 생성 설정을 저장합니다.
     */
    @PostMapping("/schedule")
    public String updateSchedule(ScheduleForm scheduleForm, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/generation",
                "예약 생성 설정이 저장되었습니다.",
                () -> generationScheduleService.updateSchedule(scheduleForm)
        );
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 수동 실행 요청으로 뉴스 크롤링을 수행합니다.
     */
    @PostMapping("/crawling/run")
    public String runManualCrawling(CrawlRunForm crawlRunForm, RedirectAttributes redirectAttributes) {
        return executeAdminAction(redirectAttributes, "/admin/crawling", null, () -> {
            var executionResult = techNewsCrawlingService.executeManualCrawling(crawlRunForm);
            if (executionResult.isSuccess()) {
                redirectAttributes.addFlashAttribute(
                        "adminMessage",
                        "크롤링 완료 (수집: " + executionResult.getCollectedCount() + "건, 신규 저장: " + executionResult.getInsertedCount() + "건)"
                );
            } else {
                if (executionResult.getErrorCode() != null && !executionResult.getErrorCode().isBlank()) {
                    redirectAttributes.addFlashAttribute("adminErrorCode", executionResult.getErrorCode());
                }
                redirectAttributes.addFlashAttribute("adminError", executionResult.getMessage());
            }
        });
    }

    /**
     * @date 2026-04-17
     * @desc 뉴스 크롤링 예약 설정을 저장합니다.
     */
    @PostMapping("/crawling/schedule")
    public String updateCrawlSchedule(CrawlScheduleForm crawlScheduleForm, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/crawling",
                "크롤링 예약 설정이 저장되었습니다.",
                () -> crawlScheduleService.updateSchedule(crawlScheduleForm)
        );
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 수동 실행 입력값으로 저장 없는 크롤링 미리보기 목록을 반환합니다.
     */
    @PostMapping("/crawling/preview")
    @ResponseBody
    public ResponseEntity<CrawlPreviewResponse> previewManualCrawling(@RequestBody CrawlRunForm crawlRunForm) {
        return ResponseEntity.ok(techNewsCrawlingService.previewManualCrawling(crawlRunForm));
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 조건 프리셋을 저장합니다.
     */
    @PostMapping("/crawling/presets")
    public String saveCrawlPreset(CrawlPresetForm crawlPresetForm, RedirectAttributes redirectAttributes) {
        return executeAdminAction(
                redirectAttributes,
                "/admin/crawling",
                "크롤링 조건 프리셋이 저장되었습니다.",
                () -> crawlConditionPresetService.savePreset(crawlPresetForm)
        );
    }

    /**
     * @date 2026-05-08
     * @desc 최근 7일 테크 뉴스 기반 주간 AI 인사이트를 생성하거나 재생성합니다.
     */
    @PostMapping("/weekly-insight/generate")
    public String generateWeeklyAiInsight(
            @RequestParam(value = "referenceDate", required = false) LocalDate referenceDate,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(redirectAttributes, "/admin/crawling", null, () -> {
            var weeklyAiInsight = weeklyAiInsightService.generateWeeklyInsight(referenceDate);
            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    "주간 AI 인사이트가 생성되었습니다. 분석 기간: "
                            + weeklyAiInsight.getWeekStartDate() + " ~ " + weeklyAiInsight.getWeekEndDate()
            );
        });
    }

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트의 사용자 노출 여부를 변경합니다.
     */
    @PostMapping("/weekly-insight/{id}/toggle-visible")
    public String toggleWeeklyAiInsightVisible(
            @PathVariable("id") Long insightId,
            RedirectAttributes redirectAttributes
    ) {
        return executeAdminAction(redirectAttributes, "/admin/crawling", null, () -> {
            var weeklyAiInsight = weeklyAiInsightService.toggleVisible(insightId);
            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    Boolean.TRUE.equals(weeklyAiInsight.getVisible())
                            ? "주간 AI 인사이트가 사용자 화면에 노출됩니다."
                            : "주간 AI 인사이트가 사용자 화면에서 숨김 처리되었습니다."
            );
        });
    }

    /**
     * @date 2026-08-07
     * @desc 관리자 작업을 실행하고 성공 또는 오류 메시지와 리다이렉트 경로를 일관되게 처리합니다.
     */
    private String executeAdminAction(
            RedirectAttributes redirectAttributes,
            String redirectView,
            String successMessage,
            AdminAction action
    ) {
        try {
            action.run();
            if (successMessage != null) {
                redirectAttributes.addFlashAttribute("adminMessage", successMessage);
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", "작업 처리 중 오류가 발생했습니다.");
        }
        return "redirect:" + redirectView;
    }

    @FunctionalInterface
    private interface AdminAction {

        /**
         * @date 2026-08-07
         * @desc 관리자 작업을 실행하며 checked exception을 호출자에게 전달합니다.
         */
        void run() throws Exception;
    }

    /**
     * @date 2026-04-15
     * @desc 관리자가 수동 생성에서 사용할 기본 입력값을 생성합니다.
     */
    private GenerationRequestForm createDefaultGenerationRequestForm() {
        GenerationRequestForm form = new GenerationRequestForm();
        form.setTargetDate(LocalDate.now());
        form.setCategory("Backend");
        form.setTone("실무형");
        form.setDifficulty("중급");
        return form;
    }

    /**
     * @date 2026-04-15
     * @desc 엔티티 기반 스케줄 값을 화면 입력 폼으로 변환합니다.
     */
    private ScheduleForm toScheduleForm(GenerationSchedule generationSchedule) {
        ScheduleForm form = new ScheduleForm();
        form.setEnabled(generationSchedule.getEnabled());
        form.setAllowDuplicate(generationSchedule.getAllowDuplicate());
        form.setCronExpression(generationSchedule.getCronExpression());
        form.setCategory(generationSchedule.getCategory());
        form.setTone(generationSchedule.getTone());
        form.setDifficulty(generationSchedule.getDifficulty());
        return form;
    }

    /**
     * @date 2026-04-17
     * @desc 관리자 크롤링 수동 실행 폼의 기본값을 생성합니다.
     */
    private CrawlRunForm createDefaultCrawlRunForm(CrawlSchedule crawlSchedule) {
        CrawlRunForm form = new CrawlRunForm();
        form.setTargetDate(LocalDate.now());
        form.setSourceName(crawlSchedule.getSourceName());
        form.setSourceUrl(crawlSchedule.getSourceUrl());
        form.setMaxArticles(crawlSchedule.getMaxArticles());
        form.setKeywordMatchType(crawlSchedule.getKeywordMatchType());
        form.setIncludeKeywords(crawlScheduleService.splitCsv(crawlSchedule.getIncludeKeywords()));
        form.setIncludeKeywordOperators(crawlScheduleService.splitCsv(crawlSchedule.getIncludeKeywordOperators()));
        form.setExcludeKeywords(crawlScheduleService.splitCsv(crawlSchedule.getExcludeKeywords()));
        form.setTargetDomains(crawlScheduleService.splitCsv(crawlSchedule.getTargetDomains()));
        form.setConnectTimeoutSeconds(crawlSchedule.getConnectTimeoutSeconds());
        form.setReadTimeoutSeconds(crawlSchedule.getReadTimeoutSeconds());
        form.setRetryCount(crawlSchedule.getRetryCount());
        return form;
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 예약 엔티티를 화면 입력 폼으로 변환합니다.
     */
    private CrawlScheduleForm toCrawlScheduleForm(CrawlSchedule crawlSchedule) {
        CrawlScheduleForm form = new CrawlScheduleForm();
        form.setEnabled(crawlSchedule.getEnabled());
        form.setAllowDuplicate(crawlSchedule.getAllowDuplicate());
        form.setCronExpression(crawlSchedule.getCronExpression());
        form.setSourceName(crawlSchedule.getSourceName());
        form.setSourceUrl(crawlSchedule.getSourceUrl());
        form.setMaxArticles(crawlSchedule.getMaxArticles());
        form.setKeywordMatchType(crawlSchedule.getKeywordMatchType());
        form.setIncludeKeywords(crawlScheduleService.splitCsv(crawlSchedule.getIncludeKeywords()));
        form.setIncludeKeywordOperators(crawlScheduleService.splitCsv(crawlSchedule.getIncludeKeywordOperators()));
        form.setExcludeKeywords(crawlScheduleService.splitCsv(crawlSchedule.getExcludeKeywords()));
        form.setTargetDomains(crawlScheduleService.splitCsv(crawlSchedule.getTargetDomains()));
        form.setConnectTimeoutSeconds(crawlSchedule.getConnectTimeoutSeconds());
        form.setReadTimeoutSeconds(crawlSchedule.getReadTimeoutSeconds());
        form.setRetryCount(crawlSchedule.getRetryCount());
        return form;
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 조건 프리셋 기본 입력값을 생성합니다.
     */
    private CrawlPresetForm createDefaultCrawlPresetForm() {
        CrawlPresetForm form = new CrawlPresetForm();
        form.setKeywordMatchType("OR");
        form.setIncludeKeywords(Collections.emptyList());
        form.setIncludeKeywordOperators(Collections.emptyList());
        form.setExcludeKeywords(Collections.emptyList());
        form.setTargetDomains(Collections.emptyList());
        form.setConnectTimeoutSeconds(5);
        form.setReadTimeoutSeconds(5);
        form.setRetryCount(1);
        form.setMaxArticles(20);
        return form;
    }
}
