# 인사이트 상세(좋아요·북마크·댓글) Planning Document

> **Summary**: 지식/뉴스 콘텐츠 상세 조회, 조회수 집계, 좋아요·북마크·댓글(대댓글 포함) 상호작용 기능
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **Status**: **Implemented (사후 문서화, 2차 사이클 정밀화)** — 코드가 이미 구현되어 있으며, 1차 사이클 compact card(`docs/02-design/features/insight-detail.md` 등)를 거쳐 정밀 문서로 승격됨 (SoR: 코드 우선)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 콘텐츠 소비 후 사용자가 반응(좋아요/북마크/댓글)할 수 있는 유일한 진입점이 필요 |
| **WHO** | 인증된 사용자만 (좋아요/북마크/댓글은 로그인 필수, 401 응답) |
| **RISK** | 6개 저장소·캐시·인증이 얽혀 있고, `Unit 3(좋아요/북마크)`·`Unit 4(댓글)`가 이 unit의 컨트롤러·서비스를 그대로 공유해 변경 파급 범위가 큼 |
| **SUCCESS** | 상세 조회 시 조회수가 세션당 1회 반영되고, 좋아요/북마크 토글과 댓글(대댓글 포함) 작성/삭제가 새로고침 없이 정상 동작 |
| **SCOPE** | 지식/뉴스 상세 조회 + 좋아요/북마크 토글 + 댓글/대댓글 작성·삭제. 콘텐츠 자체의 생성/수정(관리자 영역)은 범위 밖 |

---

## 1. Overview

### 1.1 목적

사용자가 홈에서 콘텐츠를 클릭하면 상세 화면으로 진입해 본문을 읽고, 좋아요·북마크로 반응하며, 댓글로 의견을 남길 수 있게 한다.

### 1.2 배경

`docs/MVP_SCOPE.md` §2.1(인사이트 상세)·§2.2(참여 기능)에 이미 구현 완료로 기재된 기능. 1차 사이클(compact card, 2026-08-05)에서 3개 unit(2/3/4)으로 나눠 가볍게 문서화했으나, Unit 2가 §2.4 리스크 기반 승격 기준(6개 저장소 결합, Unit 3·4의 의존 대상)에 해당해 2차 사이클에서 정밀 문서로 승격되었다.

### 1.3 관련 파일

| 레이어 | 파일 |
|--------|------|
| Controller | `InsightPageController`(화면), `InsightDetailRestController`(API, `/api/insights/{type}/{id}/**`) |
| Service | `InsightDetailService` |
| Entity | `InsightLike`, `InsightBookmark`, `InsightComment` |
| Repository | `InsightLikeRepository`, `InsightBookmarkRepository`, `InsightCommentRepository`, `DailyKnowledgeRepository`, `TechNewsRepository`, `UserRepository` (6개) |
| DTO | `InsightDetailResponseDTO`, `InsightToggleResponseDTO`, `InsightCommentDTO`, `InsightCommentRequestDTO` |
| Template/JS | `templates/insight-detail.html`, `static/js/insight-detail.js` |
| 1차 사이클 문서 | `docs/02-design/features/insight-detail.md`(Unit 2), `insight-like-bookmark.md`(Unit 3), `insight-comment.md`(Unit 4) — 정밀화 후에도 3·4는 이 문서를 기준으로 참조 유지 |

---

## 2. Scope

### 2.1 In Scope

- [x] 지식/뉴스 콘텐츠 상세 조회 + 조회수 세션당 1회 증가
- [x] 좋아요/북마크 토글(엔티티 삭제 방식, 이력 미보존)
- [x] 댓글 작성/삭제(소프트 삭제), 대댓글 작성
- [x] 집계 캐시(90초 TTL) + 쓰기 액션 시 전체 무효화

### 2.2 Out of Scope

- 댓글 수정 기능 (미구현, MVP_SCOPE.md에 명시)
- 다단 대댓글(2단계 이상) 정책 결정 — **현재 코드는 막지 않고 있으나 이게 의도인지는 미결정** (findings F-05, 본 사이클 Next Steps에서 정책 확정 필요)
- 콘텐츠 자체의 CRUD(Unit 15 관리자 게시물 관리 영역)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 로그인 사용자가 지식/뉴스 상세를 조회하면 조회수가 세션당 1회만 증가한다 | High | Done |
| FR-02 | 좋아요/북마크는 토글 방식으로 동작하며 실시간 카운트를 반환한다 | High | Done |
| FR-03 | 댓글은 500자 이내로 작성 가능하고, 부모 댓글 지정 시 대댓글로 등록된다 | High | Done |
| FR-04 | 본인 댓글만 삭제 가능하며 소프트 삭제로 처리된다 | High | Done |
| FR-05 | 비로그인 사용자가 좋아요/북마크/댓글 액션을 시도하면 401로 거부된다 | High | Done |
| FR-06 (미결정) | 대댓글에 대한 추가 대댓글(다단 중첩) 허용 여부 | Medium | **정책 미결정** — 현재 코드는 허용하는 상태 |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|--------------------|
| Consistency | 좋아요/북마크/댓글 변경 시 집계 캐시가 최신 상태를 반영해야 함 | `@CacheEvict(allEntries=true)` 적용 확인됨(코드) |
| Traceability | Unit 3·4는 이 문서를 기준 삼아 파생 정보만 유지 | compact card의 "기준 문서" 참조 확인 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 좋아요/북마크/댓글 핵심 플로우가 코드로 구현되어 있고 Design/Screen 문서로 설명 가능
- [ ] FR-06(다단 대댓글) 정책 결정 및 필요 시 서버 검증 추가 — **미완료, 후속 작업**
- [ ] 캐시 무효화가 콘텐츠 1건 단위가 아니라 전체 단위(`allEntries=true`)인 것이 의도적 트레이드오프인지 확인 — **미완료**

### 4.2 Quality Criteria

- [x] 레이어드 아키텍처 준수
- [ ] 예외 처리 스타일(REST `ResponseStatusException`)이 프로젝트 전체 표준으로 확산될지 여부는 findings F-03에서 별도 논의

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 캐시 `allEntries=true` 무효화로 트래픽 증가 시 캐시 효율 저하 | Medium | Medium | 콘텐츠별 키 기반 무효화로 전환 검토 (후속 과제) |
| 다단 대댓글 UI가 실제로는 고려되지 않은 상태(들여쓰기 무한 반복 가능) | Low | Medium | 정책 결정 후 UI/서버 동시 대응 |
| Unit 3·4가 이 문서에 강하게 의존 — 이 unit 변경 시 파급 범위 큼 | Medium | Low | 변경 전 Unit 3·4 compact card 동시 검토 |

---

## 6. Next Steps

1. [ ] Design 문서(`docs/02-design/features/insight-detail.md`)를 정밀 버전으로 확장 (진행 중)
2. [ ] Screen 문서(`docs/02-design/screens/insight-detail.md`) 작성 완료
3. [ ] Gap 분석(`docs/03-analysis/insight-detail.md`) 수행
4. [ ] 완료 보고서(`docs/04-report/insight-detail.md`) 작성
5. [ ] Codex 교차검증
6. [ ] FR-06(다단 대댓글 정책) 사용자 의사결정 요청

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | 2차 사이클 정밀 기획 문서 초안 작성 (1차 compact card 기준 확장) | Claude (대화 기반) |
