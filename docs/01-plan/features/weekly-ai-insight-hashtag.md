# 주간 AI 인사이트 해시태그 (Weekly AI Insight Hashtag) Planning Document

> **Summary**: 기존 주간 AI 인사이트(`WeeklyAiInsight`) 생성 시 LLM이 summary/trendAnalysis/developerView와 함께 3~5개의 핵심 키워드(해시태그)를 함께 생성해, 사용자 홈 화면 상단 카드에 배지 형태로 노출하는 기능
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-10
> **Status**: Draft (사용자 승인 대기, 착수 전)

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 주간 인사이트가 summary/trendAnalysis/developerView 3개의 서술형 문단으로만 구성되어 있어, 사용자가 한눈에 "이번 주 핵심 주제"를 파악하기 어렵다 |
| **Solution** | `WeeklyAiInsightService.generateWeeklyInsight()` 실행 시 LLM이 3~5개의 핵심 키워드를 함께 생성하도록 프롬프트/응답 스키마를 확장하고, 결과를 저장해 홈 화면 카드 상단에 배지로 노출한다 |
| **Function/UX Effect** | 사용자는 홈 화면 "이번 주 개발 Trend" 섹션에서 서술형 문단을 읽기 전에 해시태그 배지로 핵심 주제를 빠르게 스캔할 수 있다 |
| **Core Value** | 기존 `WeeklyAiInsight` 파이프라인(관리자 수동 생성 → LLM 호출 → 저장 → 노출 토글)에 필드 하나를 확장하는 수준으로, 별도 파이프라인·엔티티 없이 낮은 비용으로 정보 스캔성을 높인다 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | "사용자 콘텐츠 및 서비스 제안" 브레인스토밍(2026-08-10, PM 세션)에서 시작된 3가지 아이디어(일일/주간 요약 강화, 해시태그, 프롬프트 참조 연동) 중, 기존 `WeeklyAiInsight`와 겹치지 않으면서 가장 착수 비용이 낮은 항목으로 사용자가 확정 |
| **WHO** | 인증 여부 무관 홈 화면 방문자(해시태그 열람), 관리자(생성 트리거) |
| **RISK** | LLM 응답 스키마 변경이므로 기존 파서(`OpenAiLlmGenerationClient.parseWeeklyInsightResult`)와 Mock 클라이언트(`MockLlmGenerationClient`)를 함께 갱신하지 않으면 배포 후 파싱 실패 발생 가능. DB 컬럼 추가이므로 `OracleSchemaMigrationRunner` 갱신 누락 시 스키마 드리프트 발생 |
| **SUCCESS** | 관리자가 주간 인사이트를 (재)생성하면 해시태그 3~5개가 함께 저장되고, 홈 화면 카드에 배지로 노출된다 |
| **SCOPE** | 해시태그 "생성 + 저장 + 단순 배지 표시"만 포함. 클릭 인터랙션(검색 연동), 관리자 편집/숨김 UI, 해시태그를 일일 지식 생성 프롬프트에 참조하는 것은 모두 범위 밖(사용자 확정, 2026-08-10) |

---

## 1. Overview

### 1.1 Purpose

주간 AI 인사이트 생성 시 LLM이 핵심 키워드(해시태그) 3~5개를 함께 생성하도록 확장하여, 홈 화면에서 서술형 요약과 함께 배지 형태로 노출한다.

### 1.2 Background

2026-08-10 PM 세션에서 "사용자 콘텐츠 및 서비스 제안" 기능을 논의하던 중, 범위가 불명확한 아이디어를 다음 3가지로 구체화했다:

1. 일일/주간 요약을 화면 상단에 노출 — 이미 `WeeklyAiInsight`가 구현되어 홈 화면 최상단(검색 툴바 바로 아래)에 노출 중임을 확인. 신규 다이제스트 대신 **기존 기능의 노출 개선**으로 범위를 좁힘
2. 키워드 기반 해시태그 생성 — `TechNews`/`DailyKnowledge`/`WeeklyAiInsight` 어디에도 없는 신규 필드. 세 콘텐츠 유형 중 **`WeeklyAiInsight`에 한정**하기로 확정(가장 이미 응집된 파이프라인이라 확장 비용이 낮음)
3. 해시태그/요약 결과를 일일 지식 생성 프롬프트에 참조 — `DailyKnowledgeGenerationService.renderPrompt()`가 현재 `${date}/${category}/${tone}/${difficulty}`만 치환하는 구조라 별도 설계가 필요 → **이번 스코프에서 제외**, 후속 Plan으로 분리

본 문서는 위 논의에서 확정된 2번 항목("WeeklyAiInsight 해시태그")만을 다룬다.

### 1.3 Related Documents

- `docs/01-plan/features/weekly-ai-insight.md` (기반 기능의 Plan 문서, 확장 대상)
- `docs/02-design/features/weekly-ai-insight.md` (기반 기능의 Design 문서 — 본 기능 설계 시 갱신 필요)
- `docs/01-plan/features/service-quality-roadmap.md` (Phase 4 확장 후보 목록과의 관계 — 본 기능은 로드맵 Phase 4 항목이 아닌 별도 신규 제안)

---

## 2. Scope

### 2.1 In Scope

- [ ] `WeeklyAiInsightService.generateWeeklyInsight()` 실행 시 LLM이 summary/trendAnalysis/developerView와 함께 핵심 키워드 3~5개를 생성
- [ ] `GeneratedWeeklyInsightResult`, `WeeklyAiInsight` 엔티티, `WeeklyAiInsightViewDTO`에 해시태그 필드 추가
- [ ] `weekly_ai_insight` 테이블에 해시태그 저장 컬럼 추가 (`OracleSchemaMigrationRunner` 반영)
- [ ] `OpenAiLlmGenerationClient`의 요청 프롬프트/응답 JSON 스키마 확장 및 `MockLlmGenerationClient` 동기화
- [ ] 홈 화면(`index.html`) 주간 인사이트 카드에 해시태그를 단순 배지로 표시 (클릭 동작 없음)
- [ ] 관리자 미리보기 화면에도 동일 배지 노출(생성 결과 확인 목적)

### 2.2 Out of Scope

- 해시태그 클릭 시 검색/필터 연동
- 관리자의 해시태그 수동 편집/숨김/삭제 UI
- `DailyKnowledge`, `TechNews`에 대한 해시태그 확장
- 해시태그·요약 결과를 `DailyKnowledgeGenerationService`의 프롬프트 렌더링에 참조시키는 연동
- 기존에 저장된 과거 `WeeklyAiInsight` row에 대한 소급 해시태그 생성(마이그레이션 스크립트) — 신규 생성/재생성 시점부터만 적용

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 주간 인사이트 생성/재생성 시 LLM 응답에 3~5개의 해시태그(키워드)가 포함되어야 한다 | High | Pending |
| FR-02 | 해시태그가 3개 미만이거나 응답에 누락된 경우 생성 자체를 실패 처리한다(기존 필수 키 검증 패턴과 동일하게 처리) | Medium | Pending |
| FR-03 | 저장된 해시태그는 홈 화면 주간 인사이트 카드에 배지로 노출된다 (클릭 불가) | High | Pending |
| FR-04 | 관리자 미리보기 화면에서도 최신 생성 결과의 해시태그를 확인할 수 있다 | Medium | Pending |
| FR-05 | 기존 `visible` 토글, 최근 5건 이력 조회 등 기존 기능은 해시태그 추가와 무관하게 그대로 동작해야 한다(회귀 없음) | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Traceability | 본 기능은 Plan(본 문서) → Design(`docs/02-design/features/weekly-ai-insight.md` 갱신) → 코드로 추적 가능해야 함 |
| Backward Compatibility | 기존 `weekly_ai_insight` row(해시태그 없음)를 홈 화면이 렌더링할 때 오류 없이 배지 영역만 비워 보여야 함 |
| Data Integrity | LLM이 생성한 해시태그 문자열은 XSS 없이 안전하게 렌더링되어야 함(`th:text` 이스케이프 유지) |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 관리자가 주간 인사이트를 (재)생성하면 해시태그 3~5개가 함께 저장된다
- [ ] 홈 화면 카드에 해시태그 배지가 노출된다
- [ ] 해시태그가 없는 기존 row 조회 시 오류 없이 정상 렌더링된다(하위 호환)
- [ ] `WeeklyAiInsightServiceTest` 등 관련 테스트에 해시태그 생성/저장 시나리오 추가

### 4.2 Quality Criteria

- [ ] 레이어드 아키텍처 준수 (Controller → Service → Repository, DTO 분리)
- [ ] 기존 `weekly-ai-insight.md` Design 문서의 Known Gaps(서버 측 날짜 검증, race condition 등)는 본 기능 범위가 아니므로 별도로 유지, 혼동되지 않도록 Design 문서 갱신 시 구분 기재

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| LLM이 해시태그를 3~5개 범위를 벗어나거나 빈 문자열로 반환 | Medium | Medium | 파서 단계에서 개수/공백 검증 후 실패 처리(FR-02), 기존 `readRequiredValue` 패턴 재사용 |
| 응답 스키마 확장이 기존 summary/trendAnalysis/developerView 파싱에 회귀를 일으킴 | Medium | Low | `OpenAiLlmGenerationClient`/`MockLlmGenerationClient` 동시 수정 + 기존 테스트 재실행으로 회귀 확인 |
| DB 컬럼 추가 시 `OracleSchemaMigrationRunner` 반영 누락으로 환경 간 드리프트 | Medium | Medium | 기존 컬럼 추가 패턴과 동일하게 반영, 배포 전 스키마 확인 |
| 과거 row(해시태그 없음) 렌더링 시 NPE | Low | Low | DTO/템플릿에서 null-safe 처리(빈 리스트 기본값) |

---

## 6. Next Steps

1. [ ] 본 Plan 승인 후 `docs/02-design/features/weekly-ai-insight.md` 갱신(해시태그 필드/프롬프트 스키마/DB 컬럼 상세 설계) 또는 별도 Design 문서 분리 여부 결정
2. [ ] Design 확정 후 구현(Codex 인계 또는 직접 구현) → 테스트 → Gap 분석
3. [ ] 제외된 2개 항목(일일 다이제스트, 프롬프트 참조 연동)은 본 기능 완료 후 재논의 — 별도 Plan 문서로 분리 예정

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-10 | 최초 작성 — PM 세션 브레인스토밍(사용자 콘텐츠/서비스 제안 → 요약/해시태그/프롬프트 참조 3가지) 결과를 "WeeklyAiInsight 해시태그"로 범위 확정 | Claude (PM 세션 진행) |
