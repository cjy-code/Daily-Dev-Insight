package com.dailydevinsight.admin.controller;

import com.dailydevinsight.admin.dto.GenerationRequestForm;
import com.dailydevinsight.admin.dto.PromptTemplateForm;
import com.dailydevinsight.admin.dto.ScheduleForm;
import com.dailydevinsight.admin.entity.GenerationSchedule;
import com.dailydevinsight.admin.service.AdminManagementService;
import com.dailydevinsight.admin.service.DailyKnowledgeGenerationService;
import com.dailydevinsight.admin.service.GenerationHistoryService;
import com.dailydevinsight.admin.service.GenerationScheduleService;
import com.dailydevinsight.admin.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final PromptTemplateService promptTemplateService;
    private final GenerationScheduleService generationScheduleService;
    private final DailyKnowledgeGenerationService dailyKnowledgeGenerationService;
    private final GenerationHistoryService generationHistoryService;
    private final AdminManagementService adminManagementService;

    /**
     * @date 2026-04-15
     * @desc 관리자 페이지를 렌더링하고 메뉴별 데이터(통계/게시물/회원/생성관리)를 모델에 바인딩합니다.
     */
    @GetMapping
    public String adminPage(Model model) {
        promptTemplateService.ensureDefaultTemplateExists();

        model.addAttribute("stats", adminManagementService.getAdminStats());
        model.addAttribute("knowledgePostList", adminManagementService.findRecentKnowledgePosts());
        model.addAttribute("memberList", adminManagementService.findRecentUsers());

        model.addAttribute("templateList", promptTemplateService.findAllTemplates());
        model.addAttribute("activeTemplate", promptTemplateService.getActiveTemplate());
        model.addAttribute("promptTemplateForm", new PromptTemplateForm());
        model.addAttribute("generationRequestForm", createDefaultGenerationRequestForm());
        model.addAttribute("scheduleForm", toScheduleForm(generationScheduleService.getOrCreateSchedule()));
        model.addAttribute("historyList", generationHistoryService.findRecentHistory());

        return "admin";
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
        return "redirect:/admin";
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
        return "redirect:/admin";
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
        return "redirect:/admin";
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
        return "redirect:/admin";
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
        return "redirect:/admin";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자 수동 실행으로 일일 개발 지식을 생성합니다.
     */
    @PostMapping("/generate")
    public String runManualGeneration(GenerationRequestForm generationRequestForm, RedirectAttributes redirectAttributes) {
        try {
            var executionResult = dailyKnowledgeGenerationService.executeManualGeneration(generationRequestForm);
            if (executionResult.isSuccess()) {
                redirectAttributes.addFlashAttribute(
                        "adminMessage",
                        "일일 개발 지식 생성 완료 (ID: " + executionResult.getCreatedKnowledgeId() + ")"
                );
            } else {
                redirectAttributes.addFlashAttribute("adminError", executionResult.getMessage());
            }
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin";
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
        return "redirect:/admin";
    }

    /**
     * @date 2026-04-15
     * @desc 관리자가 날짜를 지정해 빠르게 수동 생성 값을 초기화합니다.
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
