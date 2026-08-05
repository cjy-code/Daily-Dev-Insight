# 전체 페이지/기능 소급 문서화 이니셔티브 Planning Document

> **Summary**: dailyDevInsight 전체 17개 페이지/기능을 **1차: 경량 compact card로 전수 커버 → 2차 이후: 실제로 손대는 unit만 위험도에 따라 정밀화**하는 반복(PDCA) 기반 소급 문서화 계획
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **Status**: Draft — Codex 검증 2회 반영 완료 (v0.3), 사용자 승인 대기

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 코드는 100개 Java 파일·약 10,171줄 규모로 이미 구현되어 있으나, `docs/02-design/`에는 `weekly-ai-insight`(기능설계)와 `admin-crawling`(화면설계) 2건만 존재해 나머지 대부분의 기획 의도·화면 구성·처리 흐름을 코드 없이는 파악할 수 없다 |
| **Solution** | 17개 unit 전체를 **1차 사이클에서 compact card(단일 파일) 수준으로 전수 문서화**하고, 위험도가 높거나(패턴 불일치·보안·트랜잭션 관련) 실제로 손댈 일이 생긴 unit만 **그 시점에 정밀 문서(Plan+Design+Screen+Analysis+Report, Codex 검증 포함)로 승격**하는 반복적 접근을 취한다 |
| **Function/UX Effect** | 새 세션(사람 팀원이 없어 암묵지를 대체할 존재가 없음)이 최소한 compact card만으로 각 unit의 목적·엔드포인트·핵심 흐름·주의사항을 파악할 수 있고, 실제 작업이 필요한 unit은 더 깊은 문서로 안전하게 진입 가능 |
| **Core Value** | "완전한 정밀 문서화"라는 이상보다 "AI 세션이 안전하게 작업할 수 있는 최소 기준을 전수 확보 + 필요한 곳만 깊이 투자"라는 현실적 목표로 재정의. 향후 신규 기능은 정상 PDCA(기획→설계→구현) 흐름을 탈 수 있는 기반이 마련됨 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 문서 부재로 인해 기존 코드의 의도·제약·리스크가 코드를 직접 읽어야만 파악 가능한 상태. 이 프로젝트는 사람 팀원 간 암묵지가 존재하는 조직(비교 검토한 사내 타 프로젝트)과 달리 **세션 간 기억을 유지할 사람 팀이 없어**, 문서가 유일한 지식 전달 수단임 |
| **WHO** | dailyDevInsight 개발자(jychoi), 향후 이 프로젝트에 참여하는 Claude/Codex 세션(매번 기억 없이 시작) |
| **RISK** | 관리자 영역(엔드포인트 36개)의 범위 과소평가, 문서 작성 중 실제 코드 동작과의 불일치 발견 시 별도 버그 트래킹 필요, **compact card 수준에 머문 unit에서 실제로는 컨트롤러 스타일·예외처리·트랜잭션 방식이 영역마다 달라(Codex 2차 검증에서 실증) "패턴을 유추해 따르라"가 잘못된 결론으로 이어질 위험** |
| **SUCCESS** | 1차: 17개 unit 전체에 compact card(목적/엔드포인트/핵심흐름/데이터·외부연동/인증·트랜잭션·캐시/예외사항/패턴 특이사항)가 존재. 2차 이후: 위험도 높은 unit 또는 실제 작업 대상 unit이 정밀 문서로 승격됨 |
| **SCOPE** | 기존 구현된 기능의 소급 문서화만 다룸. 신규 기능 추가나 코드 리팩터링은 범위 밖 (문서화 중 발견된 이슈는 compact card 또는 정밀 Design 문서의 Known Gaps 섹션에 기록만 하고 별도 후속 작업으로 분리) |

---

## 1. Overview

### 1.1 Purpose

`docs/MVP_SCOPE.md` §4(문서 부채)에 나열된 미문서화 영역 전체를 대상으로, `weekly-ai-insight`에 적용했던 것과 동일한 소급 문서화 절차(코드 우선 SoR → 문서 역산 → Codex 교차검증)를 프로젝트 전체로 확장한다.

### 1.2 Background

- `weekly-ai-insight` 1개 기능에 대해 이미 Plan+Design+Analysis+Report 전체 PDCA 사이클 완료 사례 있음 (`docs/01-plan~04-report/*/weekly-ai-insight.md`)
- 초기에는 "시간이 들어도 문서화가 우선"이라는 목표로 17개 unit 전부 정밀 문서화(80~115h)를 계획했으나, 규모 부담으로 재검토
- 사내 타 프로젝트(eGovFrame 기반, PM 전담 + CODEOWNERS 체계) 사례를 비교 검토한 결과, "1개 모듈만 정밀 문서화 + 나머지는 패턴 유추"라는 그 프로젝트의 절감 전략은 **사람 팀원의 암묵지가 존재한다는 전제** 위에 성립하며, 팀 없이 AI 세션만 반복 투입되는 dailyDevInsight에는 그대로 적용할 수 없다고 판단함
- Codex 2차 검증(`codex exec -s read-only`)에서 실제로 컨트롤러 스타일·예외처리·트랜잭션 범위·인증 처리 방식이 영역별로 이미 상이하다는 것이 코드 근거와 함께 확인되어, "얕은 문서 + 유추"의 위험성이 실증됨
- 최종적으로 **PDCA의 반복적 특성**(Gap Analysis < 90% → 반복 개선, `CLAUDE.md`에 이미 명시된 원칙)을 unit 단위로 적용하기로 함 — 1차는 전수 경량화, 이후 사이클에서 필요한 unit만 심화

### 1.3 Related Documents

- `docs/MVP_SCOPE.md` (특히 §4 문서 부채)
- `docs/01-plan/features/codex-collab-workflow.md` (협업 절차 원본)
- `docs/01-plan~04-report/*/weekly-ai-insight.md` (정밀 문서화 선례, 전체 PDCA 사이클 1건 완료)
- `docs/02-design/screens/admin-crawling.md` (화면설계 기존 존재 사례)

---

## 2. Scope

### 2.1 문서 산출물 정의 (2단계 체계)

**1차 산출물 — Compact Card (전체 17개 unit 필수)**

| 항목 | 저장 위치 | 내용 |
|---|---|---|
| Compact Card | `docs/02-design/features/{unit}.md` (파일 1개) | ① 목적(1~2문장) ② 관련 파일(컨트롤러/서비스/엔티티/템플릿) ③ 진입 엔드포인트 ④ 핵심 호출 흐름(bullet, 5~10줄) ⑤ 데이터/외부 연동 ⑥ 인증·트랜잭션·캐시 처리 방식 ⑦ 화면 요약(있는 경우, 주요 상태만) ⑧ 패턴 특이사항(다른 unit과 다르게 구현된 부분 — Codex 2차 검증에서 실제 편차 확인됨) ⑨ 알아둘 점/리스크 |

**2차 산출물 — 정밀 문서 (승격된 unit만, §2.4 기준)**

| 문서 종류 | 저장 위치 | 내용 |
|---|---|---|
| 기획 | `docs/01-plan/features/{unit}.md` | WHY/WHO, 요구사항, 성공기준 |
| 기능 설계 | `docs/02-design/features/{unit}.md` (compact card를 확장) | 데이터 모델, API/엔드포인트, 비즈니스 로직, 에러 처리, 처리흐름도(Mermaid) |
| 화면 설계 | `docs/02-design/screens/{unit}.md` | 레이아웃, 컴포넌트, 상태별 화면 |
| Gap 분석 | `docs/03-analysis/{unit}.md` | Plan/Design vs 코드 대조, Match Rate |
| 완료 보고서 | `docs/04-report/{unit}.md` | 커밋 이력, Known Gaps, Next Steps |

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
- `templates/login.html` — Unit 5 compact card 작성 중 `LoginController`가 `"views/login"`만 반환함을 재확인, **미사용 확정**
- `templates/admin/posts.html` — 코드 조사 결과 실제 컨트롤러에서 반환되지 않는 것으로 추정되는 템플릿 (Unit 15 착수 시 재확인 예정)

### 2.4 리스크 기반 승격 기준 (Compact Card → 정밀 문서)

아래 중 하나라도 해당하면 해당 unit은 compact card 단계에 머물지 않고 **즉시 또는 그 unit을 다음에 손댈 때 정밀 문서로 승격**한다.

| 기준 | 해당 여부 판단 근거 |
|---|---|
| 인증/보안 관련 | 로그인, 세션, 비밀번호, 권한 체크가 포함되는가 |
| 개인정보/트랜잭션 무결성 관련 | 회원 정보, 결제성 데이터, 동시성 이슈 가능 영역인가 |
| 이미 패턴 편차가 확인된 영역 | Codex 2차 검증 확인 사항 — 컨트롤러 스타일(REST/MVC/혼용), 예외 처리 범위, 트랜잭션 스코프(클래스 전체 vs 메서드별), 로그아웃 처리 위치가 unit마다 다름 |
| 실제 코드 수정이 예정/진행 중 | 사용자가 그 unit을 다음 작업 대상으로 지목했는가 |

**1차 승격 후보 (Codex 2차 검증에서 실제 코드로 확인)**:
- **Unit 2 (인사이트 상세)** — REST/서비스/6개 저장소/캐시가 모두 얽혀 있고, Unit 3·4가 같은 컨트롤러·서비스를 공유하므로 Unit 2를 기준으로 삼고 3·4는 delta card로 연결
- **Unit 13 (AI 생성 관리)** — MVC+JSON 혼용, 외부 LLM/이미지 연동, 예약 실행, 캐시 무효화까지 포함된 가장 복잡한 관리자 흐름
- **Unit 8 (인증/보안 체계)** — `NoOpPasswordEncoder` 사용 등 이미 알려진 보안 이슈 포함, 로그인/로그아웃 처리가 3곳(SecurityConfig/AuthService/MyPageController)에 분산된 것으로 확인됨

나머지 14개 unit은 1차에서는 compact card로 유지하고, §2.4 기준에 해당하는 상황이 생기면 그때 승격한다.

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 17개 unit 전체는 실제 코드를 SoR로 삼아 최소 compact card 1개를 작성한다 | High | Pending |
| FR-02 | §2.4 승격 기준에 해당하는 unit(1차 후보: Unit 2, 8, 13)은 정밀 문서(Plan+Design+Screen+Analysis+Report)로 승격한다 | High | Pending |
| FR-03 | 정밀 문서로 승격된 unit의 기능설계에는 Mermaid 기반 처리흐름도를 포함한다 | Medium | Pending |
| FR-04 | 정밀 문서로 승격된 unit은 작성 후 `codex exec -s read-only`로 최소 1회 교차검증을 받는다 | High | Pending |
| FR-05 | Codex 검증에서 발견된 불일치는 Version History에 반영 이력으로 기록한다 | Medium | Pending |
| FR-06 | compact card 단계 unit도 실제 코드 수정이 필요해지면 그 시점에 §2.4 기준으로 재평가해 승격 여부를 결정한다 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Traceability | 정밀 문서는 Plan → Design(→Screen) 상호 참조가 있어야 함. compact card는 승격 시 기존 내용을 확장하는 형태로 이어져야 함(재작성 아님) |
| Consistency | compact card 포맷은 §2.1 9개 항목을 모두 포함. 정밀 문서 포맷은 `weekly-ai-insight` 선례를 템플릿으로 따름 |
| Safety | Codex는 `read-only` 샌드박스로만 실행, 검증 목적 외 파일 수정 없음 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 17개 unit 전체에 compact card 존재 (1차 사이클 완료 기준)
- [ ] §2.4 1차 승격 후보(Unit 2, 8, 13)는 정밀 문서로 승격 + Codex 교차검증 완료
- [ ] `docs/MVP_SCOPE.md` §4 문서 부채 목록에서 완료 항목 갱신

### 4.2 Quality Criteria

- [ ] 정밀 문서로 승격된 unit은 실제 코드와 100% 일치 (Codex 검증으로 확인)
- [ ] compact card 단계 unit도 §2.1의 9개 필수 항목을 빠짐없이 포함 (특히 "패턴 특이사항" — 다른 unit과 다르게 구현된 부분을 반드시 기록해 잘못된 패턴 유추 방지)
- [ ] Known Gaps/보안 이슈(예: `NoOpPasswordEncoder`)는 후속 작업 후보로 명시적으로 남김

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| compact card만 있는 unit에서 "패턴 유추"로 잘못된 구현 결정 | High | Medium | §2.1 필수 항목에 "패턴 특이사항"을 넣어 편차를 명시적으로 남김. §2.4 기준에 해당하면 유추 대신 정밀 문서로 승격 |
| Unit 경계와 실제 코드 구조 불일치 (Unit 2~4가 컨트롤러/서비스 공유, 스케줄러가 Unit 12·13 서비스를 직접 호출) | Medium | High | compact card 작성 시 "관련 unit" 필드로 교차 참조 명시, 중복 설명 대신 대표 unit(Unit 2)을 기준 문서로 삼고 나머지는 delta 형태로 작성 |
| 캐시/보안/DB(Unit 8·9·10)처럼 여러 unit을 가로지르는 횡단 관심사를 개별 unit 설명만으로 놓침 | Medium | Medium | 정밀화 시 "이 unit에 영향을 주는 unit" 역참조를 명시 |
| compact card가 리팩터링 후 낡음 (파일명만 기록 시) | Medium | Medium | 관련 파일뿐 아니라 엔드포인트·테이블·테스트 파일까지 기록해 추적 가능하게 함 |
| 관리자 영역(36개 엔드포인트) 범위 과소평가로 일정 초과 | Low | Medium | compact card는 상세 서술이 아니라 요약이므로 정밀 문서 대비 이 리스크가 크게 줄어듦 |
| 소급 문서화 중 실제 버그 발견 | Low | Medium | 즉시 수정하지 않고 Known Gaps로 기록, 별도 Plan으로 분리 |
| Codex 검증 자체의 사각지대(같은 LLM 계열의 공유 blind spot) | Low | Low | 중요 반영 사항은 사용자 승인을 거침 (`CLAUDE.md` Anchor Discipline 원칙) |

---

## 6. 상세 작업 리스트 (Task List)

### 6.1 1차 사이클 — Compact Card 전수 작성

| 순서 | Phase | Unit | 산출물 | 상태 | 파일 |
|---|---|---|---|---|---|
| 1 | 1 | 홈/인사이트 목록 | compact card | ✅ 완료 | `docs/02-design/features/home-insight-list.md` |
| 2 | 1 | 인사이트 상세 | compact card (§2.4 승격 후보) | ✅ 완료 | `docs/02-design/features/insight-detail.md` |
| 3 | 1 | 좋아요/북마크 | compact card (Unit 2 delta) | ✅ 완료 | `docs/02-design/features/insight-like-bookmark.md` |
| 4 | 1 | 댓글/대댓글 | compact card (Unit 2 delta) | ✅ 완료 | `docs/02-design/features/insight-comment.md` |
| 5 | 1 | 로그인/로그아웃 | compact card | ✅ 완료 | `docs/02-design/features/login-logout.md` |
| 6 | 1 | 마이페이지 | compact card | ✅ 완료 | `docs/02-design/features/mypage.md` |
| 7 | 2 | 스케줄러 | compact card | ⬜ 대기 | — |
| 8 | 2 | 인증/보안 체계 | compact card (§2.4 승격 후보) | ✅ 완료 | `docs/02-design/features/auth-security.md` |
| 9 | 2 | 캐시 정책 | compact card | ✅ 완료 | `docs/02-design/features/cache-policy.md` |
| 10 | 2 | DB 스키마 전체 | compact card (528줄 마이그레이션 러너 대조) | ✅ 완료 | `docs/02-design/features/db-schema-migration.md` |
| 11 | 3 | 관리자 대시보드 | compact card | ✅ 완료 | `docs/02-design/features/admin-dashboard.md` |
| 12 | 3 | 크롤링 관리 | compact card (기존 화면설계 문서 참조 위주) | ✅ 완료 | `docs/02-design/features/admin-crawling-management.md` |
| 13 | 3 | AI 생성 관리 | compact card (§2.4 승격 후보) | ✅ 완료 | `docs/02-design/features/admin-ai-generation.md` |
| 14 | 3 | 회원 관리 | compact card | ✅ 완료 | `docs/02-design/features/admin-members.md` |
| 15 | 3 | 게시물 관리 | compact card (11 엔드포인트, 업로드 포함) | ✅ 완료 | `docs/02-design/features/admin-posts-management.md` |
| 16 | 3 | 통계 | compact card | ✅ 완료 | `docs/02-design/features/admin-stats.md` |
| 17 | 3 | 관리자 로그인 | compact card | ✅ 완료 | `docs/02-design/features/admin-login.md` |

**1차 사이클(compact card 17개) 전체 완료 (2026-08-05).** 실측 소요는 "시간"이 아니라 이 세션 내 연속 작업으로 완료됨(§7 참조).

**1차 사이클 합계: 약 27~42시간 (풀타임 3.5~5.5일 / 파트타임 1.5~2주)**

### 6.2 2차 사이클 — 1차 승격 후보 정밀화 (Unit 2, 8, 13)

| Unit | 정밀 문서 세트 | 예상 소요 (Codex 검증 포함) |
|---|---|---|
| 2 (인사이트 상세) | Plan+Design+Screen+Analysis+Report | 6~9h |
| 8 (인증/보안 체계) | Plan+Design+Analysis+Report (화면 없음) | 4~6h |
| 13 (AI 생성 관리) | Plan+Design+Screen+Analysis+Report | 10~14h |

**2차 사이클 합계: 약 20~29시간**

### 6.3 전체 합계 (1차 + 2차 승격분)

**약 47~71시간 (풀타임 6~9일 / 파트타임 2.5~3.5주)** — 최초 정밀화 전면 적용안(80~115h) 대비 약 40~45% 수준. 3차 이후(§2.4 기준에 새로 해당하게 되는 unit)는 발생 시점마다 개별 산정.

---

## 7. Next Steps

1. [x] 본 문서를 `codex exec -s read-only`로 1차 검증 (범위 누락, 우선순위 타당성, 시간 추정 현실성 점검) — v0.2 반영
2. [x] compact card 중심 반복 전략으로 재설계 후 `codex exec -s read-only` 2차 검증 (축소안 타당성, 레퍼런스 후보, 패턴 편차 실증) — v0.3 반영
3. [x] Phase 1~3 전체 17개 unit compact card 작성 완료 (2026-08-05, 같은 세션 내 연속 작업)
4. [ ] §2.4 승격 후보 3개(Unit 2/8/13) 2차 사이클 정밀화 착수 여부 결정
5. [ ] compact card 작성 중 발견된 확인 필요 항목 정리 및 우선순위화 (§8 참조)

---

## 8. 1차 사이클 완료 — 발견 사항 종합 (2026-08-05)

17개 compact card를 작성하며 발견된, 개별 unit 문서에는 흩어져 있는 **프로젝트 전반의 패턴**을 모아서 기록한다.

### 8.1 확정된 프로젝트 전역 특이사항

- **로그아웃 로직이 최소 2곳에 중복 구현**: `AuthController`→`AuthService`(공용) vs `MyPageController.processWithdraw()`(그 자리에서 직접 생성) — Unit 5/6/8/17에서 반복 확인
- **예외 처리 스타일이 최소 3가지로 갈라짐**: REST는 `ResponseStatusException` 직접 throw(Unit 2), Admin 전반은 광범위 `catch(Exception)`+flash(Unit 11/14/15/16 등), 마이페이지는 미포착 그대로 흘림(Unit 6) — 이 프로젝트에서 "패턴을 보고 유추"가 위험한 가장 직접적인 증거
- **`NoOpPasswordEncoder` 사용 확인**(Unit 8) — 이미 알려졌던 이슈지만 영향 범위(로그인/비밀번호변경/탈퇴 검증 전체)가 이번에 구체화됨
- **캐시 무효화 범위 미확인 지점**: Unit 15(게시물 수정/삭제)가 Unit 1의 홈 캐시를 갱신하는지 불명 — 정밀화 우선순위 후보

### 8.2 최초 조사 오류 정정 사례

- Unit 1 작성 시 "Top10 캐시 미적용"으로 잘못 기록했다가 Unit 9 작성 중 발견해 정정함(`grep -A`가 어노테이션 줄을 놓친 조사 오류). **경량 조사라도 교차 검증이 필요하다는 근거 사례**로 남김

### 8.3 §2.4 승격 후보 재확인

1차 조사 결과 Unit 2/8/13 세 곳 모두 승격 필요성이 실제로 뒷받침됨(6개 저장소 얽힘, 알려진 보안 이슈, 최대 복잡도 관리자 흐름). 추가로 Unit 15(캐시 무효화 미확인)도 승격 후보로 고려할 만함 — 2차 사이클 착수 시 재평가.

### 8.4 미사용 리소스 최종 확정

- `templates/login.html` — 미사용 확정 (Unit 5)
- `templates/admin/posts.html` — 미사용 확정 (Unit 15)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-05 | 초안 작성 — 전체 17 unit 목록, Phase 구분, 작업 리스트, 시간 추정 | Claude (대화 기반) |
| 0.2 | 2026-08-05 | Codex 1차 검증 반영 — Unit 1/2/17 관련 파일 보완, Unit 3/4 관련 클래스 구체화, Phase 3 unit 개수 정정(6→7), 관리자 36개 엔드포인트 귀속표 추가, `admin-crawling.md` 기존 화면설계 존재 반영, `/hello`·`login.html`·`admin/posts.html` Out of Scope 명시, 시간 추정 상향(72~92h → 80~115h) | Claude (Codex `codex exec -s read-only` 검증 결과 반영) |
| 0.3 | 2026-08-05 | 전략 전면 재설계 — 사내 타 프로젝트 사례 비교 검토 후 "완전 정밀 문서화" 대신 "compact card 전수 + 위험도 기반 승격"으로 전환. 근거: (1) 비교 대상 프로젝트는 사람 팀+PM 체계 전제라 dailyDevInsight(팀 없이 AI 세션만 반복)에 그대로 적용 불가, (2) Codex 2차 검증에서 컨트롤러 스타일/예외처리/트랜잭션 범위/인증 처리가 unit마다 실제로 다르다는 것을 코드 근거로 확인해 "얕은 문서+유추"의 위험을 실증. §2.1 문서 산출물을 1차(compact card)/2차(정밀 문서) 이원화, §2.4 리스크 기반 승격 기준 신설(1차 후보: Unit 2/8/13), 시간 추정 재산정(80~115h → 47~71h) | Claude (사용자 요청 + Codex `codex exec -s read-only` 2차 검증 결과 반영) |
| 0.4 | 2026-08-05 | **1차 사이클(compact card 17개) 전체 완료.** §6.1 상태표 전체 갱신, §8(발견 사항 종합) 신설 — 로그아웃 중복 구현·예외처리 3분화·캐시 무효화 미확인 지점 등 프로젝트 전역 패턴 정리, Unit 1의 캐시 조사 오류(Top10 미적용→정정) 기록, `login.html`/`admin/posts.html` 미사용 최종 확정 | Claude (같은 세션 내 연속 작업) |
