package com.dailydevinsight.admin.controller;

import com.dailydevinsight.admin.dto.GenerationRequestForm;
import com.dailydevinsight.admin.dto.GenerationExecutionResult;
import com.dailydevinsight.admin.dto.GenerationPreviewRequest;
import com.dailydevinsight.admin.dto.GenerationPreviewResponse;
import com.dailydevinsight.admin.dto.GenerationSaveRequest;
import com.dailydevinsight.admin.dto.PromptTemplateForm;
import com.dailydevinsight.admin.dto.ScheduleForm;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.entity.PromptTemplate;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.admin.service.DailyKnowledgeGenerationService;
import com.dailydevinsight.admin.service.GenerationHistoryService;
import com.dailydevinsight.admin.service.GenerationScheduleService;
import com.dailydevinsight.admin.service.PromptTemplateService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private static final String MENU_DASHBOARD = "dashboard";
    private static final String MENU_POSTS = "posts";
    private static final String MENU_MEMBERS = "members";
    private static final String MENU_GENERATION = "generation";

    private final PromptTemplateService promptTemplateService;
    private final GenerationScheduleService generationScheduleService;
    private final DailyKnowledgeGenerationService dailyKnowledgeGenerationService;
    private final GenerationHistoryService generationHistoryService;
    private final AdminManagementService adminManagementService;

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
     * @date 2026-04-15
     * @desc 관리자 게시물 관리 페이지를 렌더링합니다.
     */
    @GetMapping("/posts")
    public String postsPage(Model model) {
        model.addAttribute("currentMenu", MENU_POSTS);
        model.addAttribute("knowledgePostList", adminManagementService.findRecentKnowledgePosts());
        return "admin/posts";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 회원 관리 페이지를 렌더링합니다.
     */
    @GetMapping("/members")
    public String membersPage(Model model) {
        model.addAttribute("currentMenu", MENU_MEMBERS);
        model.addAttribute("memberList", adminManagementService.findRecentUsers());
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
     * @date 2026-04-15
     * @desc 게시물의 기본 정보(카테고리/제목)를 수정합니다.
     */
    @PostMapping("/posts/{id}/update")
    public String updateKnowledgePost(
            @PathVariable("id") Long postId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminManagementService.updateKnowledgePost(postId, category, title);
            redirectAttributes.addFlashAttribute("adminMessage", "게시물 정보가 수정되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/posts";
    }

    /**
     * @date 2026-04-15
     * @desc 게시물을 삭제합니다.
     */
    @PostMapping("/posts/{id}/delete")
    public String deleteKnowledgePost(
            @PathVariable("id") Long postId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminManagementService.deleteKnowledgePost(postId);
            redirectAttributes.addFlashAttribute("adminMessage", "게시물이 삭제되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/posts";
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
        try {
            adminManagementService.updateUser(userPrimaryKey, role, status);
            redirectAttributes.addFlashAttribute("adminMessage", "회원 정보가 수정되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/members";
    }

    /**
     * @date 2026-04-15
     * @desc 프롬프트 템플릿 신규 등록 또는 수정 저장을 처리합니다.
     */
    @PostMapping("/prompts")
    public String savePromptTemplate(PromptTemplateForm promptTemplateForm, RedirectAttributes redirectAttributes) {
        try {
            promptTemplateService.saveTemplate(promptTemplateForm);
            redirectAttributes.addFlashAttribute("adminMessage", "프롬프트 템플릿이 저장되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
    }

    /**
     * @date 2026-04-15
     * @desc 선택한 프롬프트 템플릿을 활성화합니다.
     */
    @PostMapping("/prompts/{id}/activate")
    public String activatePromptTemplate(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        try {
            promptTemplateService.activateTemplate(templateId);
            redirectAttributes.addFlashAttribute("adminMessage", "활성 프롬프트가 변경되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
    }

    /**
     * @date 2026-04-16
     * @desc 선택한 프롬프트 템플릿의 활성 상태를 토글합니다.
     */
    @PostMapping("/prompts/{id}/toggle-active")
    public String togglePromptTemplateActive(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        try {
            promptTemplateService.toggleTemplateActive(templateId);
            redirectAttributes.addFlashAttribute("adminMessage", "프롬프트 활성 상태가 변경되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
    }

    /**
     * @date 2026-04-16
     * @desc 선택한 프롬프트 템플릿을 삭제합니다.
     */
    @PostMapping("/prompts/{id}/delete")
    public String deletePromptTemplate(@PathVariable("id") Long templateId, RedirectAttributes redirectAttributes) {
        try {
            promptTemplateService.deleteTemplate(templateId);
            redirectAttributes.addFlashAttribute("adminMessage", "프롬프트 템플릿이 삭제되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
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

        try {
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
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
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
        try {
            generationScheduleService.updateSchedule(scheduleForm);
            redirectAttributes.addFlashAttribute("adminMessage", "예약 생성 설정이 저장되었습니다.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/generation";
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
        form.setCronExpression(generationSchedule.getCronExpression());
        form.setCategory(generationSchedule.getCategory());
        form.setTone(generationSchedule.getTone());
        form.setDifficulty(generationSchedule.getDifficulty());
        return form;
    }
}
