# 일일 지식 본문 마크다운 렌더링 (Knowledge Detail Markdown Rendering) Planning Document

> **Summary**: `DailyKnowledge.detail`을 LLM이 마크다운 문법으로 작성하게 하고, 상세 화면에서 이스케이프된 단일 문단이 아니라 제목/불릿/굵은 글씨가 실제로 렌더링되는 구조로 표시한다
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-14
> **Status**: Approved (2026-08-14 사용자 승인 — 구현 착수)

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | `insight-detail.html`이 `detail`을 `<p th:text="${detail.detail}">` 한 줄로만 출력해, LLM이 아무리 구조화된 내용을 생성해도 화면에서는 줄바꿈만 살아있는 밋밋한 텍스트 덩어리로 보인다 |
| **Solution** | LLM 프롬프트에 제한된 마크다운 문법을 쓰도록 지시하고, 서버에서 마크다운 → HTML 변환 + 새니타이징을 거친 `detailHtml`을 별도로 생성해 `KNOWLEDGE` 타입 상세 화면에서만 안전하게 렌더링한다 |
| **Function/UX Effect** | 사용자는 일일 지식 상세 화면에서 소제목/불릿/강조된 핵심 문구를 시각적으로 구분해서 읽을 수 있다 |
| **Core Value** | 원본 텍스트(`detail` CLOB)는 그대로 유지하면서 표시 레이어만 추가하는 최소 침습적 구조로, 기존 데이터 호환성과 뉴스 상세 화면 회귀 위험을 동시에 낮춘다 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 사용자가 오늘(2026-08-14) 실제 생성 결과를 검토하며 "노션처럼 정리된 결과값"을 요청. Claude 1차 검토 후 `codex exec -s read-only`로 교차검증 수행 |
| **WHO** | 일일 지식 상세 페이지 방문자(렌더링 결과 열람), 관리자(마크다운 포함 콘텐츠 생성/검수) |
| **RISK** | `InsightDetailService.buildDetailResponse()`가 `KNOWLEDGE`와 `NEWS` 두 타입을 같은 메서드/템플릿으로 처리하므로, 마크다운 변환을 조건 없이 적용하면 뉴스 상세 화면이 회귀할 수 있음(Codex 지적) → **KNOWLEDGE 타입에만 조건부 적용으로 확정** |
| **SUCCESS** | 새로 생성된 일일 지식 게시물의 상세 화면에서 마크다운 문법(`##`, `-`, `**`)이 실제 HTML 요소로 렌더링되고, 임의 HTML/스크립트 삽입 시도는 새니타이징으로 걸러진다 |
| **SCOPE** | `KNOWLEDGE` 타입 상세 화면의 `detail` 렌더링 방식 변경 + 관련 프롬프트 문구 추가까지만 포함. `TechNews` 상세, 관리자 미리보기 화면(`generation-compose.html`)의 textarea 편집 UX는 변경하지 않는다 |

---

## 1. Overview

### 1.1 Purpose

LLM이 생성하는 일일 지식 게시물 본문을 마크다운으로 작성하게 하고, 상세 화면에서 실제 서식(소제목/리스트/강조)으로 렌더링해 가독성을 높인다.

### 1.2 Background

- 코드 확인 결과 `insight-detail.html:57`은 `th:text`로 `detail` 전체를 이스케이프된 단일 `<p>`에 출력하며, 관련 CSS(`insight-detail.css:134`)는 `white-space: pre-wrap`만 적용돼 있어 줄바꿈 외 서식은 전혀 표현되지 않는다
- `build.gradle`에 `jsoup 1.17.2`가 이미 의존성으로 존재(현재는 RSS 크롤링 파싱에만 사용) — 새니타이징에 재사용 가능해 신규 의존성은 마크다운 파서 하나만 필요
- 대안으로 검토했던 "본문을 intro/concept/example/pitfall/summary 등 별도 컬럼으로 분리" 방식은 Codex 검토에서 엔티티·DB 마이그레이션·LLM DTO·미리보기 저장 요청·관리자 UI·Mock 구현까지 전부 바뀌는 과설계로 판정되어 **기각**
- 클라이언트 JS 렌더링(marked.js+DOMPurify)도 현재 SSR(Thymeleaf) 구조와 맞지 않아(렌더링 시점/서버-클라이언트 결과 불일치) **기각** — 서버사이드 변환으로 확정

### 1.3 Related Documents

- `docs/02-design/features/knowledge-detail-markdown-rendering.md` (본 기능 Design 문서, 후속 작성)
- `docs/02-design/features/insight-detail.md` (기존 상세 화면 Design 문서 — 본 기능으로 확장되는 대상)

---

## 2. Scope

### 2.1 In Scope

- [ ] `OpenAiLlmGenerationClient`의 일일 지식 생성 시스템 지시문에 제한된 마크다운 문법 사용 규칙 추가 (허용: `##`/`###` 소제목, `-` 불릿, `**굵게**`, 코드 블록 \`\`\`; 허용 외 문법은 쓰지 않도록 명시)
- [ ] 서버사이드 마크다운 → HTML 변환 서비스 신설 (`org.commonmark` 사용)
- [ ] jsoup 기반 허용 태그 화이트리스트 새니타이징 (`h2`,`h3`,`p`,`ul`,`ol`,`li`,`strong`,`em`,`code`,`pre`,`br` 등만 통과)
- [ ] `InsightDetailResponseDTO`에 `detailHtml` 필드 추가
- [ ] `InsightDetailService.buildDetailResponse()`에서 `contentType == KNOWLEDGE`일 때만 `detailHtml` 계산, `NEWS`는 기존과 동일하게 `null`/미사용
- [ ] `insight-detail.html`에서 `detailHtml`이 있으면 `th:utext`로 렌더링, 없으면(뉴스이거나 과거 평문 데이터) 기존 `th:text` 경로로 폴백
- [ ] `insight-detail.css`에 새로 등장하는 `h2`/`h3`/`ul`/`li`/`strong`/`code`/`pre` 스타일 추가
- [ ] `build.gradle`에 `org.commonmark:commonmark` 의존성 추가

### 2.2 Out of Scope

- `TechNews` 상세 화면의 마크다운 지원 (뉴스 요약은 크롤링 원문이라 대상 아님)
- 관리자 미리보기(`generation-compose.html`) 화면에 렌더링 미리보기 추가 — 지금은 textarea 그대로 유지, 필요해지면 후속 과제
- 과거에 이미 생성된 평문 `detail` 데이터에 대한 소급 마크다운 변환 — 신규 생성분부터만 마크다운 문법이 포함되며, 과거 데이터는 마크다운 문법이 없어도 변환 자체는 안전하게 통과(일반 텍스트는 CommonMark가 단락으로만 처리)하므로 별도 마이그레이션 불필요
- 표/체크리스트 등 확장 마크다운 문법 (CommonMark 기본 문법 범위로 한정, 필요해지면 flexmark로 교체 검토)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 일일 지식 생성 시 LLM이 제한된 마크다운 문법으로 `detail`을 작성해야 한다 | High | Pending |
| FR-02 | `KNOWLEDGE` 상세 화면은 `detail`을 마크다운 렌더링한 HTML로 표시해야 한다 | High | Pending |
| FR-03 | 렌더링된 HTML은 허용 목록에 없는 태그/속성(스크립트, 이벤트 핸들러 등)을 포함해서는 안 된다 | High | Pending |
| FR-04 | `NEWS` 상세 화면은 본 기능 적용 이전과 동일하게 동작해야 한다(회귀 없음) | High | Pending |
| FR-05 | 마크다운 문법이 없는 평문 `detail`(과거 데이터)도 오류 없이 렌더링되어야 한다 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Security | LLM이 생성한 텍스트를 신뢰하지 않고 반드시 새니타이징을 거친 후에만 `th:utext`로 출력한다 |
| Backward Compatibility | 기존 `detail` 컬럼 형식/데이터는 변경하지 않는다(표시 레이어만 추가) |
| Traceability | Plan(본 문서) → Design → 코드로 추적 가능해야 함 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 새로 생성한 일일 지식 게시물 상세 화면에서 소제목/불릿/강조가 실제 HTML로 렌더링된다
- [ ] `<script>`, `onerror` 등 악의적 태그/속성이 포함된 마크다운 입력이 새니타이징으로 제거됨을 테스트로 확인한다
- [ ] 기존 뉴스 상세 화면과 과거 평문 지식 게시물 상세 화면이 회귀 없이 동일하게 표시된다

### 4.2 Quality Criteria

- [ ] 레이어드 아키텍처 준수 (Service에 변환 로직, Controller/템플릿은 결과만 사용)
- [ ] 마크다운 변환/새니타이징 유닛 테스트 추가

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 마크다운 변환을 조건 없이 적용해 뉴스 상세 화면 회귀 | High | Medium | `contentType == KNOWLEDGE`일 때만 변환 적용(FR-04) |
| 새니타이징 화이트리스트 누락으로 XSS 통과 | High | Low | jsoup `Safelist`를 최소 허용 태그로 명시적으로 구성, 유닛 테스트로 스크립트/이벤트 핸들러 삽입 시나리오 검증 |
| LLM이 지시한 마크다운 문법 외의 문법(표 등)을 임의로 생성 | Low | Medium | CommonMark 기본 파서는 미지원 문법을 일반 텍스트로 처리하므로 깨지지 않음. 프롬프트에도 허용 문법을 명시적으로 제한 |

---

## 6. Key Decisions

| 항목 | 결정 | 근거 | 확정 시점 |
|------|------|------|-----------|
| 저장 방식 | 원본 마크다운을 기존 `detail` CLOB에 그대로 저장, 표시용 `detailHtml`은 조회 시점에 별도 생성 | 데이터 원본 보존, 향후 다른 렌더링 방식으로 바꿔도 재변환만 하면 됨 | 2026-08-14 (Codex 검토 반영) |
| 마크다운 파서 | CommonMark (필요 시 flexmark로 교체 검토) | 기본 문법(제목/리스트/굵게)만 필요, 가장 가벼움 | 2026-08-14 (Codex 검토 반영) |
| 새니타이징 | 기존 의존성 jsoup 재사용 | 신규 의존성 최소화 | 2026-08-14 (Codex 검토 반영) |
| 렌더링 위치 | 서버사이드(SSR) 변환, 클라이언트 JS 라이브러리 사용 안 함 | 기존 Thymeleaf SSR 구조와 일관성 유지 | 2026-08-14 (Codex 검토 반영) |
| 적용 범위 | KNOWLEDGE 타입에만 적용, NEWS는 기존 유지 | `InsightDetailService`가 두 타입을 공유해 무조건 적용 시 뉴스 회귀 위험 | 2026-08-14 (Codex 검토 반영) |
| 필드 분리 대안 | 채택하지 않음 | 엔티티/DB/DTO/UI 전반을 바꿔야 하는 과설계로 판정 | 2026-08-14 (Codex 검토 반영) |

### Codex 교차검증 이력

- **1차 (2026-08-14)**: `codex exec -s read-only`로 Claude의 초기 4단계 방향(마크다운 지시+파서+새니타이징+CSS)을 검토. 방향은 타당하다고 판정하되, (1) jsoup 기존 의존성 재사용 가능, (2) `detail` 원본은 그대로 두고 `detailHtml` 별도 생성, (3) `InsightDetailService`가 KNOWLEDGE/NEWS를 공유하므로 조건부 적용 필요, (4) 필드 분리 대안과 클라이언트 JS 대안은 이 프로젝트 규모에 과설계/부적합이라는 4가지를 지적. 전부 본 문서에 반영

---

## 7. Next Steps

1. [x] 본 Plan 승인 — Codex 교차검증 반영 완료(2026-08-14)
2. [ ] `docs/02-design/features/knowledge-detail-markdown-rendering.md` 작성
3. [ ] 구현 → 유닛 테스트 → 실제 생성 결과로 렌더링 확인

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-14 | 최초 작성 — 사용자 요청("노션처럼 정리된 결과값") 및 Codex 교차검증 결과를 반영해 서버사이드 마크다운 렌더링 방식으로 확정 | Claude |
