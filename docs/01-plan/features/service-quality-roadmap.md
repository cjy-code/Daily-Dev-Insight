# 서비스 품질 개선 로드맵 Planning Document

> **Summary**: 회원가입 개방(외부 확장) 전에, 기존 기능의 성능/스케일/정합성/UX 결함을 findings 백로그 기반으로 우선순위화하여 먼저 개선하는 계획
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **Status**: Draft (사용자 승인 대기, 착수 전)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 방향은 확정됨 — "회원가입을 열어 외부로 확장할 것이지만, 서비스 품질이 우선"(사용자 결정, 2026-08-06). 지금 상태로 회원가입을 열면 데이터/사용자 수 증가에 비례해 성능·운영 리스크가 커지는 지점이 이미 여러 곳 확인됨 |
| **WHO** | dailyDevInsight 운영자(jychoi), 향후 회원가입 개방 후 실사용자, 각 Phase를 담당할 개발/보안 세션 |
| **RISK** | Phase 1(성능/스케일)을 방치한 채 회원가입을 열면: 대시보드/통계 실시간 풀스캔 쿼리 부하 증가, 관리자 목록 "최근 30건 고정"으로 회원 30명 초과 시 관리 불가, 캐시 일괄무효화로 인한 비효율 확대 |
| **SUCCESS** | Phase 1 완료 시점을 회원가입 개방의 선행 조건으로 삼음. Phase 2~3은 회원가입 개방 전후 어느 시점에 해도 무방. Phase 4(확장)는 회원가입 개방과 함께 순차 진행 |
| **SCOPE** | `docs/03-analysis/full-documentation-initiative-code-findings.md`의 F-01~F-27 중 서비스 품질/확장성에 해당하는 항목을 재우선순위화하고, 신규 확장 후보(Phase 4)를 정리한다. **이 문서 자체는 우선순위 결정과 계획 수립만 담당하며, 실제 코드 수정은 각 Phase 착수 시점에 별도로 진행한다** |

---

## 1. Overview

### 1.1 목적

`full-documentation-initiative`(소급 문서화)를 통해 확보한 findings 백로그(27건)와 17개 unit compact card의 "패턴 특이사항/알아둘 점" 섹션을 다시 "서비스 품질" 관점으로 훑어, 회원가입 개방 전/후로 나눠 실행 순서를 정한다.

### 1.2 배경

- 2026-08-06 대화에서 기획자 관점 서비스 평가를 진행, "회원가입 미개방"이 가장 큰 구조적 갭으로 확인됨
- 이어서 사용자가 방향을 확정: 회원가입은 결국 열되, **서비스 품질 개선이 먼저**
- 기존 findings 문서(F-01~F-27)는 "코드 이슈 백로그"로만 존재했고 서비스 품질/확장 관점의 우선순위가 없었음 — 본 문서가 그 우선순위를 부여함

### 1.3 관련 문서

- `docs/03-analysis/full-documentation-initiative-code-findings.md` (F-01~F-27 원본 출처)
- `docs/MVP_SCOPE.md` §3 (범위 밖 기능 목록), §4 (문서 부채)
- `docs/02-design/features/*.md` (각 unit compact card — "패턴 특이사항"/"알아둘 점" 섹션)
- `docs/01-plan/features/auth-security.md` (`NoOpPasswordEncoder` 교체가 회원가입 개방의 선행 조건임을 이미 명시)

---

## 2. Scope

### 2.1 Phase 구성 (In Scope)

**Phase 1 — 성능/스케일 대비 (회원가입 개방 전 선행 조건)** — **[2026-08-06 완료]**

| 항목 | 내용 | 출처 | 상태 |
|---|---|---|---|
| P1-1 | 관리자 대시보드/통계 실시간 전체 집계 + 중복 쿼리 캐시 적용 | F-12, `admin-dashboard.md`, `admin-stats.md` | ✅ 완료 — `adminStats`/`adminContentViewStats`/`adminBookmarkStats` 캐시(TTL 5분) 적용 |
| P1-2 | 관리자 게시물/회원 목록, 마이페이지 활동내역의 "최근 30건 고정" → 페이지네이션 도입 | `admin-posts-management.md`, `admin-members.md`, `mypage.md` | ✅ 완료 — 페이지당 20건(사용자 확정) |
| P1-3 | 캐시 무효화 전략 점검(현재 `allEntries=true` 일괄 무효화만 존재) | `cache-policy.md`, F-04 | ✅ 완료 — 점검 결과 현행 유지가 타당하다고 판단(근거는 `cache-policy.md` 참조), 신규 캐시 3종에 대한 evict만 추가 |
| P1-4 | 댓글 트리 조립(전체 로드 후 메모리 구성) 성능 재검토 | F-16, `insight-comment.md` | ✅ 완료 — 알고리즘은 이미 O(n)이었고, 실제 병목은 누락된 DB 인덱스로 확인되어 인덱스 추가로 해결 |

**Phase 2 — 대댓글 UX 결함 (정책 결정 후 즉시 수정 가능)**

| 항목 | 내용 | 출처 | 상태 |
|---|---|---|---|
| P2-1 | 다단 중첩 허용 여부 정책 결정 후 서버·프론트 반영 | F-05 | ✅ **구현·검증 완료**(2026-08-07) — 다단 중첩 허용 확정, CSS depth cap으로 화면 레이아웃 대응. `insight-comment.md` §⑩ |
| P2-2 | 부모 댓글 삭제 시 자식 표시 방식(placeholder) 개선 | F-15 | ✅ **구현·검증 완료**(2026-08-07) — 삭제된 댓글은 항상 placeholder(자식 유무 무관)로 표시, 원본 비노출. `insight-comment.md` §⑩ |
| P2-3 | 대댓글 SSR 미표시(JS 의존) 개선 여부 결정 | F-17 | ✅ **구현·검증 완료**(2026-08-07) — Thymeleaf 재귀 fragment로 SSR도 전체 트리 렌더링. `insight-comment.md` §⑩ |

**Phase 3 — 정합성/일관성 (우선순위 낮음, 여유 있을 때)**

| 항목 | 내용 | 출처 | 상태 |
|---|---|---|---|
| P3-1 | 대시보드 지표 정의 불일치("게시물 수" vs "조회수" 집계 대상 콘텐츠 타입 차이) | `admin-dashboard.md` | 미정 |
| P3-2 | 예외 처리 스타일 통일(REST/Admin/마이페이지 3갈래) | F-03 | ✅ **구현·검증 완료**(2026-08-07) — REST 변경 없음, Admin은 `executeAdminAction` 헬퍼로 21개 통합, 마이페이지는 GET 방어용 `@ExceptionHandler` 추가. `exception-handling-policy.md` |
| P3-3 | 로그아웃 로직 단일화(2~3곳 중복) | F-02, F-22 | 미정 |
| P3-4 | 회원 role/status 서버 측 enum 검증 추가 | F-08, F-20 | 미정 |
| P3-5 | 탈퇴 시 댓글 정리 정책 결정 | F-09 | ✅ 정책 결정 완료(2026-08-07, 사용자 승인) — **현행 유지(보존)**. 탈퇴해도 댓글 내용·작성자 표시 그대로 유지. 코드 변경 불필요, 정책만 문서에 명문화 |

**Phase 4 — 확장 (회원가입 개방과 함께 순차 진행)**

| 항목 | 내용 | 출처 |
|---|---|---|
| P4-1 | 회원가입/계정 생성 | `MVP_SCOPE.md` §3 |
| P4-2 | 비밀번호 찾기·재설정 | `MVP_SCOPE.md` §3 |
| P4-3 | `NoOpPasswordEncoder` → 실제 인코더 교체(BCrypt 등), 기존 평문 비밀번호 마이그레이션 | `auth-security.md` Next Steps |
| P4-4 | 주간 AI 인사이트 사용자용 목록/과거 이력 노출 | `MVP_SCOPE.md` §3 |
| P4-5 | 좋아요/북마크 이력 보존(추천/개인화 기반 데이터 확보) | `insight-like-bookmark.md` |
| P4-6 | 댓글 수정 기능 | `MVP_SCOPE.md` §3 |
| P4-7 | 스케줄러 실패 재시도/알림 정책 | `scheduler.md` |

### 2.2 Out of Scope

- 이 문서에서 각 Phase 항목의 **실제 코드 구현에 착수하지 않음** — 본 문서는 우선순위/순서 결정만 담당
- 회원가입(P4-1) 자체의 상세 기획(가입 플로우, 이메일 인증 여부 등)은 별도 Plan 문서로 분리
- Phase 3의 정책 결정이 필요한 항목(F-03 예외처리 스타일, F-09 탈퇴 댓글 정책)은 이 문서에서 결정하지 않고, 별도 기획 세션에서 결정 후 반영

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | Phase 1(P1-1~P1-4)은 회원가입 개방(P4-1) 착수 전 완료되어야 한다 | High | Pending |
| FR-02 | Phase 2(대댓글 UX)는 정책 결정 후 순서 무관하게 착수 가능하다 | Medium | Pending |
| FR-03 | Phase 3(정합성)은 여유 있는 시점에 진행하며 회원가입 개방의 선행 조건이 아니다 | Low | Pending |
| FR-04 | Phase 4는 P4-3(비밀번호 인코더 교체)이 P4-1(회원가입)보다 먼저 완료되어야 한다 | High | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Traceability | 각 Phase 항목은 findings 백로그(F-XX) 또는 unit compact card로 근거 추적 가능해야 함 |
| Scalability | Phase 1 완료 기준: 회원/게시물 수가 현재의 10배가 되어도 관리자 화면이 정상 동작해야 함 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] Phase 1(P1-1~P1-4) 완료 (2026-08-06)
- [ ] Phase 4의 P4-3(비밀번호 인코더 교체)이 P4-1(회원가입) 착수 전 완료
- [ ] Phase 2, 3은 각 항목별 정책 결정 완료 여부와 무관하게 백로그로 추적 가능한 상태 유지

### 4.2 Quality Criteria

- [x] Phase 1 완료 후 회귀 테스트 확인 (2026-08-06, JDK 21, `./gradlew test --rerun`) — 14클래스 64건 전부 통과, 실패/에러 0건. **단, 이건 회귀 여부 확인이며 실제 부하 증가 시나리오(§3.2 Scalability 기준: 10배 규모) 검증은 별도로 미실시**
- [ ] Phase 4 착수 전 Phase 1 완료 여부를 체크리스트로 확인

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Phase 1을 건너뛰고 회원가입을 먼저 열면 사용자 증가 시 관리자 화면이 마비될 수 있음 | High | Medium | FR-01로 순서를 강제, 회원가입 착수 전 체크리스트 확인 |
| `NoOpPasswordEncoder` 교체 없이 회원가입을 열면 실사용자 비밀번호가 평문으로 저장됨 | High | High(현재 상태 유지 시) | FR-04로 순서 강제, `auth-security.md`의 기존 경고와 연결 |
| Phase 3(정책 결정 필요 항목)이 우선순위가 낮다는 이유로 무기한 방치될 수 있음 | Low | Medium | 정기적으로(예: Phase 4 진행 중 마일스톤마다) 재검토 |

---

## 6. Next Steps

1. [x] Phase 1 착수 여부 확정 및 개발 세션 분리 착수 — 완료 (2026-08-06, Claude 기반 개발 세션). **향후 개발은 Codex가 담당, Claude는 검증·문서화 담당으로 역할 분리 확정**
2. [x] Phase 2 정책 결정(F-05 다단 중첩 허용 여부, F-15 삭제된 부모 표시 방식, F-17 SSR 개선 여부) — 완료 (2026-08-07, PM 세션): F-05 **다단 중첩 허용**, F-15 **Placeholder 표시**, F-17 **SSR 개선 착수**. Phase 2 전 항목 정책 결정 완료
3. [x] Phase 3 정책 결정(F-03 예외처리 통일 방향, F-09 탈퇴 댓글 정책) — 완료 (2026-08-07, PM 세션): F-03 **전면 통일**, F-09 **현행 유지(보존)**
4. [x] Phase 2/3에서 결정된 구현 항목(F-05/F-15/F-17/F-03) 전부 Codex에 인계·구현·검증 완료(2026-08-07) — F-05/F-15/F-17: `codex-collab-workflow.md` §7~7.1, `insight-comment.md`; F-03: `exception-handling-policy.md`. Phase 2/3 정책 결정 항목 전체 구현 완료(P3-1/P3-3/P3-4는 애초에 정책 결정 불요 항목으로 백로그 유지)
5. [ ] Phase 4 착수 전 Phase 1 완료 여부 재확인
6. [ ] 본 로드맵 진행 상황은 이 문서의 각 Phase 체크박스로 추적, 주요 변경은 Version History에 기록
7. [ ] 역할 세분화(F-03 설계 단계 분리, Phase 4 보안 리뷰 게이트, 부하 QA 세션) — 2026-08-07 PM 세션에서 검토했으나 지금 확정하지 않기로 결정. **Phase 4 착수 시점에 재논의**
8. **[보류, 2026-08-07 PM 세션]** 아래 5건은 사용자 결정으로 지금 진행하지 않는다 — 재개 시점은 별도 지시 대기:
   - Phase 4 착수 준비(회원가입 개방, `NoOpPasswordEncoder` 교체 등)
   - 부하 QA(10배 규모 시나리오 검증)
   - P3-1(대시보드 지표 정의 불일치)/P3-3(로그아웃 단일화)/P3-4(role/status 검증) 일반 백로그
   - Codex `workspace-write` 샌드박스 근본 원인 조사
   - **`admin/` 패키지 구조 재편**(계층형 `admin/controller`·`admin/service`·`admin/dto` 평면 구조 → 페이지/기능 단위 하위패키지로 전환). 근거: `admin/service` 26개 파일이 통계·크롤링·AI생성·프롬프트 구분 없이 평면 배치, `admin/controller`는 파일 1개(`AdminPageController.java`)에 전 admin 화면 엔드포인트 집중. `CLAUDE.md` 컨벤션 문서 갱신 + 40개+ 파일 import 경로 수정이 필요한 중간 규모 리팩터링이라 별도 설계 문서 필요(보류 사유: Codex 토큰 소진, 2026-08-07)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | 초안 작성 — 기획자 관점 서비스 평가(대화 기반) + findings 백로그(F-01~F-27) + 17개 unit compact card를 재우선순위화하여 Phase 1~4로 구성 | Claude (대화 기반) |
| 0.2 | 2026-08-06 | Phase 1(P1-1~P1-4) 구현 완료 반영 — 관련 unit 문서(`admin-dashboard.md`, `admin-stats.md`, `admin-posts-management.md`, `admin-members.md`, `mypage.md`, `cache-policy.md`, `insight-comment.md`) 갱신 | Claude |
| 0.3 | 2026-08-06 | Phase 1 회귀 테스트 검증 완료 반영(JDK 21, 14클래스 64건 전부 통과) — §4.2, §6 체크박스 갱신. 부하 시나리오 검증은 미실시로 명시. 향후 개발=Codex/검증·문서화=Claude 역할 분리 확정 기록 | Claude (검증) |
| 0.4 | 2026-08-07 | Phase 2/3 정책 결정 세션(PM) 반영 — F-05 다단 중첩 허용, F-17 SSR 재귀 렌더링 개선 착수, F-03 예외 처리 전면 통일, F-09 탈퇴 댓글 현행 유지(보존)로 확정. §2.1 Phase 2/3 표에 상태 컬럼 추가, §6 Next Steps 갱신(구현은 Codex 인계 대기) | Claude (PM 세션 진행) |
| 0.5 | 2026-08-07 | F-15(부모 댓글 삭제 시 자식 표시) 정책 결정 추가 반영 — **Placeholder 표시**로 확정. Phase 2 전 항목(P2-1~P2-3) 정책 결정 완료 | Claude (PM 세션 진행) |
| 0.6 | 2026-08-07 | Phase 2 F-05/F-15/F-17 구현·검증 완료 반영 — Codex 파이프라인 첫 파일럿 성공(`danger-full-access`), 68 tests 통과 확인. §2.1 Phase 2 상태 갱신, §6 Next Steps #4 갱신 | Claude (검증) |
| 0.7 | 2026-08-07 | Phase 3 F-03 구현·검증 완료 반영 — Codex 파이프라인 두 번째 파일럿(`exception-handling-policy.md`), 71 tests 통과 확인. Phase 2/3 정책 결정 항목 전체 구현 완료 | Claude (검증) |
| 0.8 | 2026-08-07 | 잔여 작업 4건(Phase 4 준비, 부하 QA, P3-1/P3-3/P3-4 백로그, workspace-write 원인 조사) 전부 보류 결정 반영 — §6 Next Steps #8 추가 | Claude (PM 세션 진행) |
| 0.9 | 2026-08-07 | `admin/` 패키지 구조 재편(계층형→기능별) 항목을 보류 목록에 추가(Codex 토큰 소진으로 지금 진행 안 함) | Claude (PM 세션 진행) |
