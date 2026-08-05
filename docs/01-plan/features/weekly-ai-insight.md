# 주간 AI 인사이트 (Weekly AI Insight) Planning Document

> **Summary**: 최근 7일 테크 뉴스를 LLM으로 종합 요약해 "이번 주 개발 Trend"로 제공하는 관리자 수동 생성 기능
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **Status**: **Implemented (사후 문서화)** — 코드가 이미 구현되어 있으며, 본 문서는 실제 구현·설계 문서(`docs/02-design/features/weekly-ai-insight.md`)를 기준으로 역산 작성됨 (SoR: 코드 우선)

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 개별 뉴스 나열만으로는 사용자가 한 주간의 개발 트렌드/흐름을 파악하기 어렵다 |
| **Solution** | 관리자가 기준일을 지정해 트리거하면, 최근 7일(기준일 포함) 테크 뉴스를 모아 LLM이 요약/트렌드·패턴/개발자 관점 3가지로 분석하고, 결과를 저장해 관리자 미리보기 + 사용자 홈 화면에 노출한다 |
| **Function/UX Effect** | 사용자는 홈 화면에서 매주 갱신되는 "이번 주 개발 Trend" 섹션을 통해 개별 기사보다 상위 레벨의 트렌드 요약을 접할 수 있다. 관리자는 노출 토글로 공개 여부를 직접 통제한다 |
| **Core Value** | 기존 `DailyKnowledgeGenerationService`(일간 지식 생성)와 별도 파이프라인으로, 주간 단위 종합 인사이트라는 새로운 콘텐츠 유형을 제공한다 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 개별 뉴스 나열만으로는 주간 트렌드를 파악하기 어려움 → AI가 7일치 뉴스를 종합 요약 |
| **WHO** | 인증된 사용자(홈 화면 노출), 관리자(생성/노출 관리) |
| **RISK** | 소스 뉴스가 0건이면 생성 실패, LLM 응답 실패 시 예외 노출, 서버 측 날짜 검증 부재로 임의 기준일 생성 가능 |
| **SUCCESS** | 관리자가 기준일 지정 후 생성하면 요약/트렌드/개발자 관점 3분류가 생성되고, 노출 토글로 사용자 화면 반영 여부를 제어할 수 있다 |
| **SCOPE** | 생성/조회/노출토글만 포함. 자동 스케줄, 사용자용 과거 이력 조회는 범위 밖 |

---

## 1. Overview

### 1.1 Purpose

관리자가 수동으로 트리거하면 최근 7일 테크 뉴스를 종합 분석해 사용자에게 "이번 주 개발 Trend"를 제공한다.

### 1.2 Background

`docs/MVP_SCOPE.md` §2.1(사용자 화면), §2.4(관리자 기능), §2.6(AI 생성 파이프라인)에 이미 구현 완료 항목으로 기재되어 있던 기능이나, 대응하는 Plan/Design 문서가 없어 `docs/MVP_SCOPE.md` §4(문서 부채)에서 최우선 사후 문서화 대상으로 지정되었다. Design 문서가 먼저 작성되었고(2026-08-03, Codex 교차검증 완료), 본 Plan 문서는 그 뒤를 이어 역산 작성한다.

### 1.3 Related Documents

- `docs/MVP_SCOPE.md` §2.1, §2.4, §2.6, §4
- `docs/02-design/features/weekly-ai-insight.md` (구현 상세, Known Gaps 포함)
- `docs/01-plan/features/codex-collab-workflow.md` (본 문서 작성에 적용된 Claude+Codex 협업/역산 문서화 절차)

---

## 2. Scope

### 2.1 In Scope

- [x] 관리자 화면에서 기준일 지정 후 주간 인사이트 수동 생성/재생성
- [x] 최근 7일 소스 뉴스 기반 LLM 요약(summary/trendAnalysis/developerView 3분류)
- [x] 관리자 노출 토글(visible on/off)
- [x] 관리자 미리보기(최신 1건) + 최근 5건 이력 조회
- [x] 사용자 홈 화면에 최신 공개(visible=true) 인사이트 1건 노출

### 2.2 Out of Scope

- 자동 생성 스케줄 (수동 트리거만 지원 — `docs/MVP_SCOPE.md` Not Yet Implemented)
- 사용자용 과거 이력 조회/목록 API (최신 1건만 노출)
- 프롬프트 인젝션 방지 로직 (뉴스 요약·날짜·출처·제목·URL 전체가 필터링 없이 프롬프트에 포함됨)
- 동시 생성 요청에 대한 원자적 upsert/lock 처리

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 관리자는 기준일(`referenceDate`, 미지정 시 오늘)을 기준으로 최근 7일 뉴스를 모아 주간 인사이트를 생성할 수 있다 | High | Done |
| FR-02 | 소스 뉴스가 0건이면 생성이 실패하고 관리자 화면에 에러 메시지가 노출된다 | High | Done |
| FR-03 | 같은 기간(`weekStartDate`+`weekEndDate`)으로 재생성 시 기존 row를 갱신하며, 기존 `visible` 값은 유지된다 | High | Done |
| FR-04 | 관리자는 특정 인사이트의 사용자 노출 여부를 토글할 수 있다 | High | Done |
| FR-05 | 사용자 홈 화면은 `visible=true`인 가장 최근 기간의 인사이트 1건만 노출한다 | High | Done |
| FR-06 | 관리자 화면은 최신 인사이트(공개 여부 무관) 미리보기와 최근 5건 이력을 함께 보여준다 | Medium | Done |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method | Status |
|----------|----------|--------------------|--------|
| Traceability | 기능은 Plan(본 문서) → Design(`docs/02-design/features/weekly-ai-insight.md`) → 코드로 추적 가능해야 함 | 문서-코드 상호 참조 확인 | Done (사후) |
| Security | 생성/토글 엔드포인트는 관리자 인증이 필요하고 CSRF 토큰을 포함해야 함 | `SecurityConfig` + 템플릿 `_csrf` 확인 | Done |
| Data Integrity | LLM 생성 결과는 XSS 없이 안전하게 렌더링되어야 함 | 템플릿 `th:text` 이스케이프 확인 | Done |

> **Not Yet Met** (Design 문서 §8 Known Gaps 참조): 서버 측 날짜 범위 검증, race condition 방지, OpenAI 호출 timeout/retry, 트랜잭션 내부 외부 호출 분리, `sourceNewsCount` 의미 일치, LLM 에러의 사용자 메시지 사용 — 후속 개선 후보로 이관.

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 관리자가 기준일 지정 후 생성하면 3분류 인사이트가 저장된다
- [x] 노출 토글로 사용자 화면 반영 여부가 즉시 통제된다
- [x] `WeeklyAiInsightServiceTest`에 핵심 시나리오(신규 생성/기간 갱신/노출 토글) 유닛 테스트 존재
- [ ] 소스 뉴스 0건, 존재하지 않는 id 토글, LLM 예외 전파 케이스 테스트 커버 (미충족 — 후속 작업 후보)

### 4.2 Quality Criteria

- [x] 레이어드 아키텍처 준수 (Controller → Service → Repository, DTO 분리)
- [ ] Design 문서 §8에 기재된 Known Gaps는 별도 개선 과제로 관리 필요 (본 Plan 범위 밖)

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 소스 뉴스 0건으로 생성 실패 | Low | Medium | `IllegalStateException` → 관리자 화면 flash 메시지로 안내 (구현됨) |
| 같은 기간 동시 생성 요청 시 race condition | Medium | Low | 현재 미대응 — 후속 작업 후보로 Design 문서에 기재 |
| OpenAI 응답 지연 시 DB 트랜잭션 장시간 점유 | Medium | Low | 현재 미대응 — 외부 호출을 트랜잭션 밖으로 분리하는 리팩터링 후속 검토 필요 |
| `docs/sql/`에 대응 스키마 파일 부재로 환경 간 스키마 드리프트 | Medium | Medium | `OracleSchemaMigrationRunner`가 SoR 역할 — 별도 SQL 문서 보강 필요 |

---

## 6. Next Steps

1. [ ] 본 Plan + 기존 Design 문서 기준으로 Gap 분석(`docs/03-analysis/weekly-ai-insight.md`) 수행해 일치율 확인
2. [ ] Gap 분석 결과를 바탕으로 완료 보고서(`docs/04-report/weekly-ai-insight.md`) 작성
3. [ ] Design 문서 §8 Known Gaps 중 우선순위 높은 항목(서버 측 날짜 검증, LLM 에러 메시지 미사용 등)을 별도 개선 Plan으로 분리

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-05 | 사후 기획 문서 초안 작성 (기존 구현 및 Design 문서 기준 역산) | Claude (대화 기반) |
