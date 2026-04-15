package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MockLlmGenerationClient implements LlmGenerationClient {

    /**
     * @date 2026-04-15
     * @desc 실제 LLM 연동 전까지 템플릿 기반 응답을 생성합니다.
     */
    @Override
    public GeneratedKnowledgeResult generateKnowledge(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    ) {
        String title = String.format("%s %s 핵심 인사이트", targetDate, category);
        String summary = String.format("%s 관점의 %s 난이도 개발 지식을 요약했습니다.", tone, difficulty);
        String detail = buildDetail(prompt, category, tone, difficulty);

        return GeneratedKnowledgeResult.builder()
                .title(title)
                .summary(summary)
                .detail(detail)
                .build();
    }

    /**
     * @date 2026-04-15
     * @desc 템플릿 입력값을 포함한 상세 본문을 구성합니다.
     */
    private String buildDetail(String prompt, String category, String tone, String difficulty) {
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        String promptPreview = normalizedPrompt.length() > 500
                ? normalizedPrompt.substring(0, 500)
                : normalizedPrompt;

        return "[개요]\n"
                + "- 카테고리: " + category + "\n"
                + "- 톤: " + tone + "\n"
                + "- 난이도: " + difficulty + "\n\n"
                + "[실무 포인트]\n"
                + "1) 문제를 작은 단위로 분리하고 검증 가능한 기준을 먼저 정의합니다.\n"
                + "2) 기능 구현 전 장애 시나리오를 먼저 정의해 운영 리스크를 줄입니다.\n"
                + "3) 성능이 민감한 구간은 반복문/DB 쿼리 횟수를 우선 점검합니다.\n\n"
                + "[프롬프트 스냅샷]\n"
                + promptPreview;
    }
}
