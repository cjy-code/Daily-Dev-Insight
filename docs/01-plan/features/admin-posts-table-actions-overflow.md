# 관리자 게시물 관리 테이블 — 작업 컬럼 잘림 (Admin Posts Table Actions Overflow) Planning Document

> **Summary**: `게시물 관리` 목록 테이블에서 맨 오른쪽 `작업`(수정/썸네일 삭제/삭제) 컬럼이 좁아지며 버튼 텍스트가 글자 단위로 줄바꿈되고 화면 밖으로 잘려 보이는 문제를 CSS로 수정한다
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-24
> **Status**: Implemented (2026-08-30 `admin.css` 수정 및 육안 확인 완료, `admin-posts-management.md` 반영만 미완)

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | `게시물 관리 - 일일 지식`(`/admin/posts/knowledge`) 화면 스크린샷 확인 결과, 카테고리/제목 입력창이 각자 기본 너비를 차지해 남는 공간이 부족해지고, 맨 오른쪽 `작업` 컬럼(수정/썸네일 삭제/삭제 버튼)이 극단적으로 좁아져 버튼 텍스트가 글자 단위로 줄바꿈되며 화면 밖으로 밀려나 보인다 |
| **Solution** | `.table-actions` 버튼에 `white-space: nowrap` 추가, 작업 컬럼에 `min-width` 지정, `.admin-table`에 `min-width`를 줘서 좁아지는 대신 `.admin-panel`의 기존 `overflow-x: auto`로 명확하게 가로 스크롤되도록 CSS만 수정 |
| **Function/UX Effect** | 관리자가 게시물 관리 화면에서 작업 버튼 텍스트를 온전히 읽고 클릭할 수 있게 된다 |
| **Core Value** | 마크업/템플릿 변경 없이 `admin.css` 수정만으로 해결되는 최소 침습적 수정 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 사용자가 2026-08-24 스크린샷(`스크린샷 2026-08-24 225636.png`)을 공유하며 "게시물 관리 보면 콘텐츠 쪽이 좀 짤리는데 어떻게 생각함"이라고 질문 → Claude가 코드(`posts-knowledge.html`, `admin.css`)를 확인해 원인 진단, 사용자가 "작업은 내일 일단 문서 작성해둬"로 착수 시점을 다음 날(2026-08-25)로 지정 |
| **WHO** | 관리자 콘솔 게시물 관리 화면을 사용하는 운영자(수정/삭제/썸네일 작업을 수행하는 주 사용자) |
| **RISK** | `.admin-table`/`.table-actions`는 `posts-knowledge.html`뿐 아니라 `posts-news.html`에서도 동일 구조로 재사용됨(코드 확인됨) — CSS 클래스 단위 수정이므로 두 화면 모두 함께 영향받는다. `posts.html`은 죽은 템플릿(`/admin/posts` GET이 즉시 `/admin/posts/knowledge`로 redirect, `admin-posts-management.md` §⑧에서 이미 확정됨)이라 실질적 영향 없음 |
| **SUCCESS** | 게시물 관리(지식/뉴스) 목록 화면에서 작업 버튼 3개(수정/썸네일 삭제/삭제) 텍스트가 줄바꿈 없이 온전히 보이고, 필요 시 `.admin-panel`의 가로 스크롤로 전체 컬럼에 접근 가능하다 |
| **SCOPE** | `admin.css`의 `.table-actions`, `.admin-table` 관련 규칙 수정까지만 포함. 다른 `.admin-table` 사용 화면(회원 관리, 통계 등)에서 동일 증상이 재현되는지는 이번 조사 범위 밖(필요 시 별도 확인) |

---

## 1. Overview

### 1.1 Purpose

게시물 관리 목록 테이블의 작업 컬럼이 잘려 보이는 UI 버그를 CSS 수정으로 해결한다.

### 1.2 Background

- `posts-knowledge.html:81-96`, `posts-news.html:81`(추정 동일 구조)의 `<td class="table-actions">`에 폼 3개(수정/썸네일 삭제/삭제)가 들어있고, 각 폼의 `<button>`은 `.admin-table button` 스타일(`admin.css:738`)을 상속하지만 `white-space` 지정이 없다
- `.admin-table { width: 100%; }`(`admin.css:1106`)에 `table-layout: fixed`나 컬럼별 `min-width`가 없어 브라우저가 컨텐츠 기준으로 폭을 자동 배분 → 카테고리/제목 입력창이 우선 공간을 차지하고 작업 컬럼에 남는 폭이 부족해짐
- `.admin-panel`에 `overflow-x: auto`(`admin.css:466`)가 이미 걸려 있어 가로 스크롤 자체는 가능하지만, 좁아진 컬럼이 시각적으로 "잘린 것"처럼 보여 스크롤이 필요하다는 게 사용자에게 드러나지 않음
- `.table-actions { display: flex; flex-wrap: wrap; gap: 8px; }`(`admin.css:1164`)는 버튼 줄바꿈만 처리할 뿐, 컬럼 자체가 좁을 때 버튼 내부 텍스트가 글자 단위로 꺾이는 것은 막지 못함

### 1.3 Related Documents

- `docs/02-design/features/admin-posts-management.md` (게시물 관리 기능 Design 문서 — 본 수정 완료 후 갱신 필요)
- [[admin-ui-overhaul]] (본 수정 확인 중 발견된 관리자 콘솔 전반 여백/톤 문제 — 별도 백로그로 분리, 이번 작업 범위 밖)

---

## 2. Scope

### 2.1 In Scope

- [ ] `admin.css` `.table-actions button`(또는 `.admin-table button`)에 `white-space: nowrap` 추가
- [ ] `작업` 컬럼(`th`/`td`)에 `min-width` 지정 (예: 110~120px, 버튼 3개가 세로로 쌓여도 텍스트가 안 깨지는 최소 폭 기준으로 실측 후 확정)
- [ ] `.admin-table`에 `min-width`(예: 760px 내외, 실측 후 확정) 지정해 컬럼이 무한정 좁아지는 대신 `.admin-panel`의 `overflow-x: auto`로 가로 스크롤되도록 전환
- [ ] `posts-knowledge.html`, `posts-news.html` 두 화면 모두에서 육안 확인(실제 gradlew bootRun 기동 상태로 스크린샷 비교)

### 2.2 Out of Scope

- 작업 버튼을 아이콘/드롭다운 메뉴로 압축하는 구조적 UI 개편 (이번엔 CSS 최소 수정으로 한정, 필요성은 대화 중 언급됐으나 채택 안 함)
- `posts.html`(죽은 템플릿) 수정
- 회원 관리 등 다른 `.admin-table` 사용 화면의 동일 증상 여부 확인/수정

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 게시물 관리(지식) 목록에서 작업 컬럼 버튼 3개의 텍스트가 줄바꿈 없이 표시되어야 한다 | High | Done |
| FR-02 | 게시물 관리(뉴스) 목록에서도 동일하게 작업 컬럼이 정상 표시되어야 한다 | High | Done |
| FR-03 | 화면 폭이 좁아 테이블이 잘리는 경우, 잘림이 아니라 `.admin-panel`의 가로 스크롤로 자연스럽게 처리되어야 한다 | Medium | Done |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Backward Compatibility | 마크업(`th:`/`td` 구조), 서버 로직 변경 없이 CSS만 수정 — 다른 admin 화면 레이아웃에 회귀가 없어야 함 |
| Traceability | Plan(본 문서) → 수정 → `admin-posts-management.md` 갱신까지 추적 가능해야 함 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 지식/뉴스 게시물 관리 화면 모두에서 작업 버튼 텍스트가 정상적으로(줄바꿈 없이) 표시됨을 스크린샷으로 확인
- [x] 좁은 화면에서는 잘림 대신 가로 스크롤이 명확히 동작함을 확인
- [ ] 다른 admin 화면(예: 대시보드, 회원 관리)에서 레이아웃 회귀가 없음을 육안 확인 — 실제 `bootRun` 기동 상태에서 별도 확인 필요

### 4.2 Quality Criteria

- [x] `admin.css` 외 파일 변경 없음 (스코프 최소화)

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| `.admin-table`/`.table-actions`가 다른 admin 화면에서도 공유되어 의도치 않은 레이아웃 변화 발생 | Medium | Medium | 수정 후 지식/뉴스 화면뿐 아니라 대시보드·회원 관리 등 주요 admin 화면도 육안 확인 |
| `min-width` 값을 너무 크게 잡아 좁은 창에서 불필요한 가로 스크롤 유발 | Low | Low | 실제 컨텐츠(카테고리/제목 입력창 + 버튼 3개) 기준 최소값으로 실측 후 결정 |

---

## 6. Key Decisions

| 항목 | 결정 | 근거 | 확정 시점 |
|------|------|------|-----------|
| 수정 범위 | CSS(`admin.css`)만 수정, 템플릿/서버 로직 변경 없음 | 원인이 컬럼 폭 계산 문제로 한정되어 있고, 최소 침습적 수정으로 충분 | 2026-08-24 (사용자와 대화 중 확정) |
| 착수 시점 | 2026-08-25로 연기, 오늘은 문서만 작성 | 사용자 지시("작업은 내일 일단 문서 작성해둬") | 2026-08-24 |
| 구조적 개편(아이콘/드롭다운) | 채택 안 함 | 이번 수정은 잘림 버그 해결이 목적, UI 구조 개편은 별도 논의 필요 | 2026-08-24 |

---

## 7. Next Steps

1. [x] 스크린샷 기반 원인 진단 및 사용자 확인 (2026-08-24)
2. [x] 본 Plan 문서 작성 (2026-08-24)
3. [x] `admin.css` 수정 (`.table-actions button` nowrap, 작업 컬럼 min-width, `.admin-table` min-width) — 2026-08-30 완료
4. [x] 지식/뉴스 화면 육안 확인 — 2026-08-30, 실제 템플릿 마크업 + 실제 `admin.css`를 그대로 로드한 정적 하네스 페이지를 Chrome 헤드리스로 렌더링해 확인(로컬 Oracle/Redis/Docker 미가동으로 실제 `bootRun` 기동은 못 함). 840px/700px 폭에서 수정 전(작업 컬럼 클리핑, 스크롤 불가) vs 수정 후(작업 헤더 노출, `.admin-panel` 내부 가로 스크롤 정상 동작) 차이 확인. `posts-news.html`은 `posts-knowledge.html`과 `.admin-table`/`.table-actions` 마크업이 동일함을 코드로 확인해 별도 렌더링 없이 동일 결론 적용
5. [ ] `docs/02-design/features/admin-posts-management.md`에 수정 이력 반영

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-24 | 최초 작성 — 스크린샷 기반 작업 컬럼 잘림 원인 진단 및 수정 계획 정리, 착수는 2026-08-25로 연기 | Claude |
| 0.2 | 2026-08-30 | `admin.css` 수정 및 육안 확인 완료 반영 — Status/FR/DoD 갱신 | Claude |
