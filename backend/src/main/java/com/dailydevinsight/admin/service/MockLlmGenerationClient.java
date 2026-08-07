package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GeneratedWeeklyInsightResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmGenerationClient implements LlmGenerationClient {

    /**
     * @date 2026-04-16
     * @desc 로컬 개발용 임시 Mock 구현체입니다.
     *       운영 연동 시 실제 LLM API 클라이언트 구현체로 교체해야 합니다.
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
     * @date 2026-05-08
     * @desc 로컬 개발 환경에서 사용할 주간 AI 인사이트 Mock 결과를 생성합니다.
     */
    @Override
    public GeneratedWeeklyInsightResult generateWeeklyInsight(String prompt, LocalDate weekStartDate, LocalDate weekEndDate) {
        return GeneratedWeeklyInsightResult.builder()
                .summary("이번 주 크롤링 뉴스에서는 AI 개발 도구, 백엔드 성능 개선, 보안 업데이트 흐름이 두드러졌습니다.")
                .trendAnalysis("반복적으로 등장한 패턴은 자동화 도구 확산, 인프라 비용 최적화, 데이터 처리 안정성 강화입니다.")
                .developerView("개발자는 새 도구 도입보다 운영 안정성, 검증 가능한 자동화, 장애 대응 전략을 우선 점검하는 것이 좋습니다.")
                .build();
    }

    /**
     * @date 2026-04-16
     * @desc 요청 조건과 프롬프트 미리보기를 포함한 Mock 본문을 구성합니다.
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
