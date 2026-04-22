package com.dailydevinsight.controller;

import com.dailydevinsight.dto.DailyInsightResponseDTO;
import com.dailydevinsight.service.DailyInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DailyInsightRestController {

    private final DailyInsightService dailyInsightService;

    @GetMapping(value = "/insights", produces = MediaType.APPLICATION_JSON_VALUE)
    public DailyInsightResponseDTO getInsights(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate targetDate = clampToToday(date != null ? date : LocalDate.now());
        return dailyInsightService.getInsightsByDate(targetDate);
    }

    /**
     * @date 2026-04-22
     * @desc 미래 날짜 요청을 방지하기 위해 조회 기준일을 오늘 날짜 이하로 보정합니다.
     */
    private LocalDate clampToToday(LocalDate targetDate) {
        LocalDate today = LocalDate.now();
        if (targetDate.isAfter(today)) {
            return today;
        }
        return targetDate;
    }
}
