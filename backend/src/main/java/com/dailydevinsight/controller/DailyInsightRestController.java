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
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dailyInsightService.getInsightsByDate(targetDate);
    }
}