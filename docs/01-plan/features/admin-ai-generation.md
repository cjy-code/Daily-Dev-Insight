# AI 생성 관리 Planning Document

> **Summary**: 관리자가 일일 개발 지식 콘텐츠를 자동(예약)/수동으로 AI 생성하고, 프롬프트 템플릿을 관리하며, 미리보기 후 저장하는 워크플로우
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **Status**: **Implemented (사후 문서화, 2차 사이클 정밀화)** — SoR: 코드 우선

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 매일 새로운 개발 지식 콘텐츠를 사람이 직접 쓰지 않고 LLM으로 생성해 운영 부담을 줄임 |
| **WHO** | 관리자만(`/admin/**`) |
| **RISK** | LLM/이미지 생성 외부 API 의존, 생성 실패 시 부분 실패(텍스트는 되고 이미지만 실패 등) 처리 방식 불명확 |
| **SUCCESS** | 관리자가 즉시 생성하거나, 미리보기로 검토 후 저장하는 두 경로 모두 정상 동작하고 캐시가 함께 갱신됨 |
| **SCOPE** | 지식 콘텐츠 생성(자동/수동/미리보기), 프롬프트 템플릿 CRUD, 생성 예약 조건 저장. 뉴스 크롤링(Unit 12)·게시물 사후 수정(Unit 15)은 범위 밖 |

---

## 1. Overview

### 1.1 목적

`DailyKnowledgeGenerationService`가 LLM 텍스트 생성 + 이미지 생성을 조합해 일일 지식 콘텐츠를 만든다. 관리자는 즉시 발행하거나, 별도 창(compose)에서 미리보고 수정 후 발행할 수 있다.

### 1.2 배경

1차 사이클(compact card)에서 "두 생성 플로우가 왜 공존하는지 불명"으로 열어뒀던 질문이 이번 정밀화에서 해소됨: `admin/generation.html`의 수동 생성 폼에 버튼 2개가 있고, 하나는 `POST /admin/generate`(즉시 발행), 다른 하나는 `admin.js`가 같은 입력값을 쿼리스트링으로 넘겨 `/admin/generation/compose`를 **새 창으로 열어**(`window.open`) 미리보기 후 저장하는 경로다. 둘 다 실제로 연결된 의도된 진입점이었다.

### 1.3 관련 파일

| 레이어 | 파일 |
|--------|------|
| Controller | `AdminPageController` (11개 엔드포인트) |
| Service | `DailyKnowledgeGenerationService`, `PromptTemplateService`, `GenerationScheduleService`, `GenerationHistoryService` |
| 외부 연동 | `LlmGenerationClient`, `ImageGenerationClient`(`ObjectProvider`로 선택적 주입 — 미설정 시 이미지 생성 스킵 가능성) |
| Entity | `PromptTemplate`, `GenerationSchedule`, `GenerationHistory`, `DailyKnowledge` |
| Template/JS | `admin/generation.html`+`admin.js`(즉시 생성 폼 + compose 새창 오픈), `admin/generation-compose.html`+`admin-generation-compose.js`(미리보기/저장) |

---

## 2. Scope

### 2.1 In Scope

- [x] 예약(스케줄) 생성 — 동일 대상일 데이터 존재 시 중복 정책(`allowDuplicate`)에 따라 스킵/재생성
- [x] 수동 즉시 생성(`/admin/generate`)
- [x] 수동 미리보기(`/admin/generate/preview`) → 이미지 재생성(`/preview/image-refresh`) → 저장(`/admin/generate/save`)
- [x] 프롬프트 템플릿 CRUD + 단일 활성 템플릿 정책
- [x] 생성 성공/스킵/실패 이력 기록

### 2.2 Out of Scope

- 이미지 생성 API 자체의 상세 프롬프트 엔지니어링 로직
- 생성된 콘텐츠의 사후 수정(Unit 15 게시물 관리 영역)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 예약 생성은 대상일에 기존 데이터가 있고 `allowDuplicate=false`면 스킵하고 이력만 남긴다 | High | Done |
| FR-02 | 수동 즉시 생성은 검증 후 바로 저장되고 캐시가 무효화된다 | High | Done |
| FR-03 | 미리보기는 DB에 저장하지 않고 LLM/이미지 생성 결과만 반환하며, 같은 대상일의 기존 결과가 있으면 비교용으로 함께 보여준다 | High | Done |
| FR-04 | 저장은 미리보기(또는 관리자가 수정한) 텍스트를 그대로 받아 저장한다(재생성 안 함) | High | Done |
| FR-05 | 프롬프트 템플릿은 활성 템플릿이 없으면 생성 자체가 불가능하다 | Medium | Done |
| FR-06 | 생성 성공/스킵/실패가 각각 이력으로 구분 기록된다 | Medium | Done |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Consistency | 생성 성공 시 홈 화면 관련 캐시(`insightsByDate`/`insightsByRange`/`weeklyTop10`/`weeklyTop5`)가 함께 무효화되어야 함 |
| Resilience | LLM 실패는 `LlmClientException`으로 구분 처리되어야 함(사용자 메시지 별도 존재) |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 즉시 생성/미리보기/저장 3개 경로가 코드로 구현되어 있고 실제 화면과 연결됨을 확인
- [x] 캐시 무효화 대상이 4개 캐시로 확인됨
- [ ] 이미지 생성 부분 실패(텍스트는 성공, 이미지만 실패) 시 동작 확정 — **미완료**

### 4.2 Quality Criteria

- [x] 레이어드 아키텍처 준수
- [ ] `ImageGenerationClient`가 `ObjectProvider`로 선택 주입되는 이유(빈이 없을 수 있는 상황) 확인 — **미완료**

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 이미지 생성 API 장애 시 전체 생성이 실패하는지, 텍스트만 저장되는지 불명확 | Medium | Medium | Design 정밀화에서 코드 흐름 재확인 |
| 컴포즈 새창이 팝업 차단 브라우저 설정에서 동작 안 할 수 있음 | Low | Low | 별도 대응 없음, 알려진 제약으로만 기록 |
| 즉시 생성과 미리보기-저장 두 경로가 검증 로직을 각각 구현해 한쪽만 수정되고 다른 쪽이 누락될 위험 | Medium | Medium | 공통 검증 메서드 재사용 여부 Design에서 확인 |

---

## 6. Next Steps

1. [ ] Design 문서 정밀 확장 (진행 중)
2. [ ] Screen 문서 작성 (즉시생성 폼 vs compose 새창 UX 비교 포함)
3. [ ] Gap 분석
4. [ ] 완료 보고서
5. [ ] Codex 교차검증 — Unit 2·8 선례에 따라 검증 전 판정은 잠정으로 취급

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | 2차 사이클 정밀 기획 문서 초안 작성 — 즉시생성/미리보기 두 플로우 공존 이유 규명 | Claude (대화 기반) |
