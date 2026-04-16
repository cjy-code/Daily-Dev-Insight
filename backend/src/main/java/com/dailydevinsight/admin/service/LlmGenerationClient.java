package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;

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
}
