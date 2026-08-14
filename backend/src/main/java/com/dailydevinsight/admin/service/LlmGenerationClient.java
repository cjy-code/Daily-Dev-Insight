package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GeneratedDailyTrendResult;
import com.dailydevinsight.admin.dto.GeneratedWeeklyInsightResult;

import java.time.LocalDate;

public interface LlmGenerationClient {

    /**
     * @date 2026-04-16
     * @desc 렌더링된 프롬프트와 생성 조건을 LLM 제공자에게 전달해 구조화된 결과를 생성합니다.
     *       실제 외부 LLM API 연동 구현이 이루어지는 경계 인터페이스입니다.
     */
    GeneratedKnowledgeResult generateKnowledge(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    );

    /**
     * @date 2026-05-08
     * @desc 최근 크롤링 뉴스 목록을 기반으로 주간 개발 트렌드 분석 결과를 생성합니다.
     */
    GeneratedWeeklyInsightResult generateWeeklyInsight(String prompt, LocalDate weekStartDate, LocalDate weekEndDate);

    /**
     * @date 2026-08-13
     * @desc 크롤링 뉴스 목록을 기반으로 기준일의 일일 개발 트렌드를 생성합니다.
     */
    GeneratedDailyTrendResult generateDailyTrend(String prompt, LocalDate targetDate);
}
