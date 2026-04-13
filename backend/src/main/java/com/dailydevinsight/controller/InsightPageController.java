package com.dailydevinsight.controller;

import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.dto.DailyInsightDTO;
import com.dailydevinsight.service.DailyInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class InsightPageController {

    private final DailyInsightService dailyInsightService;

    /**
     * @date 2026-04-13
     * @desc 메인 화면 조회 조건을 해석해 인사이트 데이터를 렌더링합니다.
     */
    @GetMapping({"/", "/index"})
    public String index(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(value = "keyword", required = false)
            String keyword,
            @RequestParam(value = "searchType", required = false)
            String searchType,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = date != null ? date : (endDate != null ? endDate : today);
        LocalDate resolvedEndDate = endDate != null ? endDate : targetDate;
        LocalDate resolvedStartDate = startDate != null ? startDate : resolvedEndDate.minusMonths(3);
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            LocalDate temp = resolvedStartDate;
            resolvedStartDate = resolvedEndDate;
            resolvedEndDate = temp;
        }
        String resolvedSearchType = (searchType == null || searchType.isBlank()) ? "title_content" : searchType;
        DailyInsightResponseDTO response = dailyInsightService.getInsightsByRange(
                resolvedStartDate,
                resolvedEndDate,
                keyword,
                resolvedSearchType
        );

        model.addAttribute("response", response);
        model.addAttribute("dailyKnowledgeList", safeList(response.getDailyKnowledgeList()));
        model.addAttribute("techNewsList", safeList(response.getTechNewsList()));
        model.addAttribute("top10List", safeList(response.getTop10List()));
        model.addAttribute("top5List", safeList(response.getTop5List()));
        model.addAttribute("weeklyHotList", safeList(response.getWeeklyHotList()));
        model.addAttribute("dailyKnowledgeChunks", chunkBySix(response.getDailyKnowledgeList()));
        model.addAttribute("techNewsChunks", chunkBySix(response.getTechNewsList()));
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("startDate", resolvedStartDate);
        model.addAttribute("endDate", resolvedEndDate);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("searchType", resolvedSearchType);
        return "index";
    }

    /**
     * @date 2026-04-13
     * @desc 테스트용 Hello 페이지를 렌더링합니다.
     */
    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("message", "Hello Daily Dev Insight!");
        return "hello";
    }

    /**
     * @date 2026-04-13
     * @desc null 리스트를 안전한 빈 리스트로 변환합니다.
     */
    private List<DailyInsightDTO> safeList(List<DailyInsightDTO> source) {
        return source == null ? Collections.emptyList() : source;
    }

    /**
     * @date 2026-04-13
     * @desc 카드 렌더링을 위해 리스트를 6개 단위로 분할합니다.
     */
    private List<List<DailyInsightDTO>> chunkBySix(List<DailyInsightDTO> items) {
        List<DailyInsightDTO> safeItems = safeList(items);
        if (safeItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<DailyInsightDTO>> chunks = new ArrayList<>();
        for (int i = 0; i < safeItems.size(); i += 6) {
            int end = Math.min(i + 6, safeItems.size());
            chunks.add(safeItems.subList(i, end));
        }
        return chunks;
    }
}

