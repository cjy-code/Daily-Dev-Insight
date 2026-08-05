# 전체 페이지/기능 소급 문서화 이니셔티브 Planning Document

> **Summary**: 이미 구현되어 있으나 문서가 없는 dailyDevInsight 전체 페이지/기능을 기획·기능설계·화면설계·처리흐름도 4종 세트로 소급 문서화하는 프로젝트 단위 계획
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **Status**: Draft — Codex 검증 1회 반영 완료 (v0.2), 사용자 승인 대기

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 코드는 100개 Java 파일·약 10,171줄 규모로 이미 구현되어 있으나, `docs/02-design/`에는 `weekly-ai-insight`(기능설계)와 `admin-crawling`(화면설계) 2건만 존재해 나머지 대부분의 기획 의도·화면 구성·처리 흐름을 코드 없이는 파악할 수 없다 |
| **Solution** | 전체 화면/기능을 17개 단위(unit)로 나누고, 각 unit마다 기획→기능설계(흐름도 포함)→화면설계 3개 문서를 소급 작성한다. 시간이 걸리더라도 완전한 문서화를 우선순위로 둔다 (사용자 명시적 결정) |
| **Function/UX Effect** | 사용자(개발자)나 새 세션이 코드를 처음부터 읽지 않아도 각 기능의 목적·설계·화면·흐름을 문서만으로 파악 가능해짐 |
| **Core Value** | 향후 신규 기능은 정상적인 PDCA(기획→설계→구현) 흐름을 탈 수 있는 기반이 마련되고, 기존 기능 수정 시에도 회귀 리스크를 줄일 수 있다 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 문서 부재로 인해 기존 코드의 의도·제약·리스크가 코드를 직접 읽어야만 파악 가능한 상태이며, 사용자가 "시간이 걸려도 완전한 문서화"를 명시적 목표로 결정함 |
| **WHO** | dailyDevInsight 개발자(jychoi), 향후 이 프로젝트에 참여하는 Claude/Codex 세션 |
| **RISK** | 관리자 영역(엔드포인트 36개)의 범위 과소평가, 문서 작성 중 실제 코드 동작과의 불일치 발견 시 별도 버그 트래킹 필요, 장기 작업으로 인한 중도 우선순위 변경 |
| **SUCCESS** | 17개 unit 전체에 대해 기획+기능설계(흐름도 포함)+화면설계(해당 시) 문서가 존재하고, 각 문서가 Codex 교차검증을 1회 이상 거친다 |
| **SCOPE** | 기존 구현된 기능의 소급 문서화만 다룸. 신규 기능 추가나 코드 리팩터링은 범위 밖 (문서화 중 발견된 이슈는 `docs/02-design/features/{unit}.md`의 Known Gaps 섹션에 기록만 하고 별도 후속 작업으로 분리) |

---

## 1. Overview

### 1.1 Purpose

`docs/MVP_SCOPE.md` §4(문서 부채)에 나열된 미문서화 영역 전체를 대상으로, `weekly-ai-insight`에 적용했던 것과 동일한 소급 문서화 절차(코드 우선 SoR → 문서 역산 → Codex 교차검증)를 프로젝트 전체로 확장한다.

### 1.2 Background

- `weekly-ai-insight` 1개 기능에 대해 이미 Plan+Design 문서 소급 작성 및 Codex 교차검증(`codex exec -s read-only`) 완료 사례 있음 (`docs/02-design/features/weekly-ai-insight.md` v0.2)
- 사용자가 대화 중 "시간이 들어도 문서화가 우선"이라고 명시적으로 우선순위를 정함 — 점진적/부분적 문서화가 아니라 전체 완전 문서화가 목표
- 이 문서 자체도 `docs/01-plan/features/codex-collab-workflow.md`에 정의된 Claude(문서화)+Codex(검증) 역할 분리 절차를 따른다

### 1.3 Related Documents

- `docs/MVP_SCOPE.md` (특히 §4 문서 부채)
- `docs/01-plan/features/codex-collab-workflow.md` (협업 절차 원본)
- `docs/02-design/features/weekly-ai-insight.md` (선례)

---

## 2. Scope

### 2.1 문서 산출물 정의

| 문서 종류 | 저장 위치 | 내용 |
|---|---|---|
| 기획 | `docs/01-plan/features/{unit}.md` | WHY/WHO, 요구사항, 성공기준 |
| 기능 설계 | `docs/02-design/features/{unit}.md` | 데이터 모델, API/엔드포인트, 비즈니스 로직, 에러 처리, 처리흐름도(Mermaid) |
| 화면 설계 | `docs/02-design/screens/{unit}.md` | 레이아웃, 컴포넌트, 상태별 화면(빈 상태/에러/로딩) — **화면이 있는 unit만 해당** |

### 2.2 대상 Unit 목록 (실제 코드 조사 기준, 총 17개)

**Phase 1 — 사용자 화면 (6 units, 화면+기능+흐름도 모두 작성)**

| # | Unit | 관련 파일 |
|---|------|-----------|
| 1 | 홈/인사이트 목록 | `index.html`, `InsightPageController`, `DailyInsightRestController`, `DailyInsightService` |
| 2 | 인사이트 상세 | `insight-detail.html`, `InsightPageController`, `InsightDetailRestController`, `InsightDetailService` |
| 3 | 좋아요/북마크 | `InsightDetailRestController`, `InsightDetailService`, `InsightLike`/`InsightBookmark` entity, `InsightLikeRepository`/`InsightBookmarkRepository`, `InsightToggleResponseDTO`, `MyPageService`(활동내역 연동) |
| 4 | 댓글/대댓글 | `InsightDetailRestController`, `InsightDetailService`(parentCommentId 기반 트리 조립), `InsightComment` entity, `InsightCommentRepository`, `InsightCommentDTO`/`InsightCommentRequestDTO` |
| 5 | 로그인/로그아웃 | `views/login.html`(실사용, `templates/login.html`은 미사용 추정 — 착수 시 재확인), `LoginController`, `AuthController` |
| 6 | 마이페이지 (프로필/비밀번호/탈퇴/활동내역) | `mypage/*.html`, `MyPageController` (엔드포인트 8개) |

**Phase 2 — 백엔드 인프라 (4 units, 화면 없음 — 기획+기능설계+흐름도만)**

| # | Unit | 관련 파일 |
|---|------|-----------|
| 7 | 스케줄러 | `ScheduledCrawlingExecutor`, `ScheduledGenerationExecutor`, 관련 `GenerationScheduleService`/`CrawlScheduleService`류, Entity/Repository 포함 |
| 8 | 인증/보안 체계 | `SecurityConfig` (⚠️ `NoOpPasswordEncoder` 사용 중 — 문서화 시 명시), `AuthService`, `CustomUserDetailsService` |
| 9 | 캐시 정책 | `RedisCacheConfig` |
| 10 | DB 스키마 전체 | `OracleSchemaMigrationRunner`(528줄), 전체 Entity/`docs/sql/*` 대조 |

**Phase 3 — 관리자 (관리자 업무 6 units + 관리자 인증 1 unit = 7 units, `AdminPageController` 36개 엔드포인트 + `LoginController`의 관리자 로그인)**

| # | Unit | 관련 파일 | 엔드포인트 수 |
|---|------|-----------|---|
| 11 | 관리자 대시보드 | `admin/dashboard.html` | 2 |
| 12 | 크롤링 관리 (스케줄/이력/프리셋 + 주간 AI 인사이트 생성·토글) | `admin/crawling.html` — **화면설계는 `docs/02-design/screens/admin-crawling.md`가 이미 존재, 검토·보완 위주** | 7 (크롤링 5 + 주간AI 2) |
| 13 | AI 생성 관리 (자동/수동, 프롬프트 템플릿, 이미지 생성, 업로드 저장 포함) | `admin/generation.html`, `admin/generation-compose.html`, `OpenAiImageGenerationClient`, `WebResourceConfig`/`/uploads/**` | 11 |
| 14 | 회원 관리 | `admin/members.html` | 2 |
| 15 | 게시물 관리 (지식/뉴스, 썸네일 업로드 포함) | `admin/posts-knowledge.html`, `admin/posts-news.html` — **`admin/posts.html`은 `/admin/posts/knowledge`로 리다이렉트되어 실제 렌더링 안 됨, 미사용으로 표시** | 11 |
| 16 | 통계 (조회수/북마크) | `admin/stats-views.html`, `stats-bookmarks.html` | 3 |
| 17 | 관리자 로그인 | `views/admin-login.html`, `LoginController`(관리자 로그인 처리), `SecurityConfig` | (관리자 인증 체인, `AdminPageController` 소속 아님) |

> `weekly-ai-insight`(기존 완료)는 위 목록에서 제외됨. `/hello`(`hello.html`, `InsightPageController`)는 코드 주석상 "테스트용" 명시 화면이라 **Out of Scope**로 유지 (2.3절 참조).

### 2.3 Out of Scope

- 문서화 과정에서 발견된 버그/설계 결함의 즉시 수정 (Known Gaps로 기록만, 수정은 별도 작업)
- 신규 기능 추가
- 기존 코드 리팩터링
- `/hello`(`hello.html`) — 코드 주석에 "테스트용 Hello 페이지"로 명시된 개발용 화면, 제품 기능 아님
- `templates/login.html`, `templates/admin/posts.html` — 코드 조사 결과 실제 컨트롤러에서 반환되지 않는 것으로 추정되는 템플릿 (각 unit 착수 시 재확인 후 미사용 확정되면 문서에서 제외 명시)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 각 unit은 실제 코드를 SoR로 삼아 기획/설계 문서를 역산 작성한다 | High | Pending |
| FR-02 | 화면이 있는 unit은 화면설계 문서를 추가로 작성한다 | High | Pending |
| FR-03 | 기능설계 문서에는 Mermaid 기반 처리흐름도를 포함한다 | High | Pending |
| FR-04 | 모든 문서는 작성 후 `codex exec -s read-only`로 최소 1회 교차검증을 받는다 | High | Pending |
| FR-05 | Codex 검증에서 발견된 불일치는 Version History에 반영 이력으로 기록한다 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Traceability | 모든 문서는 Plan → Design(→Screen) 상호 참조가 있어야 함 |
| Consistency | 문서 포맷은 `weekly-ai-insight.md` 선례를 템플릿으로 따름 |
| Safety | Codex는 `read-only` 샌드박스로만 실행, 검증 목적 외 파일 수정 없음 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 17개 unit 전체에 대해 Plan + Design(+Screen) 문서 존재
- [ ] 각 문서 Codex 교차검증 1회 이상 완료 및 반영
- [ ] `docs/MVP_SCOPE.md` §4 문서 부채 목록에서 완료 항목 갱신

### 4.2 Quality Criteria

- [ ] 각 문서가 실제 코드와 100% 일치 (Codex 검증으로 확인)
- [ ] Known Gaps/보안 이슈(예: `NoOpPasswordEncoder`)는 후속 작업 후보로 명시적으로 남김

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 관리자 영역(36개 엔드포인트) 범위 과소평가로 일정 초과 | Medium | High | Phase 3 착수 시 unit을 더 세분화할 수 있음을 미리 명시 |
| 장기 작업 중 우선순위 변경/중단 | Medium | Medium | Phase 단위로 독립적으로 완료 가능하게 설계 — 중단해도 이미 완료된 Phase는 자산으로 남음 |
| 소급 문서화 중 실제 버그 발견 | Low | Medium | 즉시 수정하지 않고 Known Gaps로 기록, 별도 Plan으로 분리 |
| Codex 검증 자체의 사각지대(같은 LLM 계열의 공유 blind spot) | Low | Low | 중요 반영 사항은 사용자 승인을 거침 (`CLAUDE.md` Anchor Discipline 원칙) |

---

## 6. 상세 작업 리스트 (Task List)

| 순서 | Phase | Unit | 작업 유형 | 예상 소요 |
|---|---|---|---|---|
| 1 | 1 | 홈/인사이트 목록 | 기획+기능설계(흐름도)+화면설계 | 4~6h |
| 2 | 1 | 인사이트 상세 | 기획+기능설계(흐름도)+화면설계 | 4~6h |
| 3 | 1 | 좋아요/북마크 | 기획+기능설계(흐름도)+화면설계 | 4~6h |
| 4 | 1 | 댓글/대댓글 | 기획+기능설계(흐름도)+화면설계 | 4~6h |
| 5 | 1 | 로그인/로그아웃 | 기획+기능설계(흐름도)+화면설계 | 3~5h |
| 6 | 1 | 마이페이지 | 기획+기능설계(흐름도)+화면설계 (8 엔드포인트, 5 화면) | 6~8h |
| 7 | 2 | 스케줄러 | 기획+기능설계(흐름도) (실행기 2개 + Service/Entity/Repository 포함) | 5~7h |
| 8 | 2 | 인증/보안 체계 | 기획+기능설계(흐름도) | 3~4h |
| 9 | 2 | 캐시 정책 | 기획+기능설계(흐름도) | 3~4h |
| 10 | 2 | DB 스키마 전체 | 기획+기능설계(흐름도) (528줄 마이그레이션 러너, 전체 Entity 대조) | 6~9h |
| 11 | 3 | 관리자 대시보드 | 기획+기능설계(흐름도)+화면설계 | 3~5h |
| 12 | 3 | 크롤링 관리 (7 엔드포인트, 화면설계는 기존 문서 보완 위주) | 기획+기능설계(흐름도)+화면설계(보완) | 8~12h |
| 13 | 3 | AI 생성 관리 (11 엔드포인트, 화면 2개, 업로드 포함) | 기획+기능설계(흐름도)+화면설계 | 10~14h |
| 14 | 3 | 회원 관리 | 기획+기능설계(흐름도)+화면설계 | 3~5h |
| 15 | 3 | 게시물 관리 (11 엔드포인트, 썸네일 업로드 포함) | 기획+기능설계(흐름도)+화면설계 | 8~10h |
| 16 | 3 | 통계 | 기획+기능설계(흐름도)+화면설계 | 3~5h |
| 17 | 3 | 관리자 로그인 | 기획+기능설계(흐름도)+화면설계 | 3~4h |

**합계 예상**: 약 80~115시간 (풀타임 10~14일 / 파트타임 4~5주) — Codex 검증에서 최초 추정치(72~92h)가 관리자 영역(Unit 12/13/15)과 DB 스키마(Unit 10)의 실제 규모 대비 과소평가되었음이 확인되어 상향 조정함

---

## 7. Next Steps

1. [x] 본 문서를 `codex exec -s read-only`로 검증 (범위 누락, 우선순위 타당성, 시간 추정 현실성 점검)
2. [x] 검증 결과를 Version History에 반영 (v0.2)
3. [ ] 사용자 승인 후 Phase 1 Unit 1(홈/인사이트 목록)부터 착수

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-05 | 초안 작성 — 전체 17 unit 목록, Phase 구분, 작업 리스트, 시간 추정 | Claude (대화 기반) |
| 0.2 | 2026-08-05 | Codex 검증 반영 — Unit 1/2/17 관련 파일 보완, Unit 3/4 관련 클래스 구체화, Phase 3 unit 개수 정정(6→7), 관리자 36개 엔드포인트 귀속표 추가, `admin-crawling.md` 기존 화면설계 존재 반영, `/hello`·`login.html`·`admin/posts.html` Out of Scope 명시, 시간 추정 상향(72~92h → 80~115h) | Claude (Codex `codex exec -s read-only` 검증 결과 반영) |
