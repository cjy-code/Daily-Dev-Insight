package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;

import java.time.LocalDate;

public interface LlmGenerationClient {

    /**
     * @date 2026-04-15
     * @desc 프롬프트와 생성 조건을 사용해 일일 개발 지식 결과를 생성합니다.
     */
    GeneratedKnowledgeResult generateKnowledge(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    );
}
