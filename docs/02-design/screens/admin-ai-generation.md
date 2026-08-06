# [화면] 관리자 - AI 생성 관리 (`admin/generation.html`, `admin/generation-compose.html`)

> **Project**: dailyDevInsight | **Date**: 2026-08-06
> **Status**: 2차 사이클 정밀화 (§2.4 승격 후보)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 관리자가 일일 지식 콘텐츠 생성을 직접 트리거하고, 필요하면 검토 후 발행할 수 있어야 함 |
| **WHO** | 관리자만 |
| **RISK** | compose 새창이 팝업 차단 시 무반응할 수 있음 |
| **SUCCESS** | 두 화면 모두 정상 동작하고, compose에서 저장 성공 시 부모 창(generation.html)에 반영됨 |
| **SCOPE** | 두 화면의 UI 구성과 상호작용만. 서버 로직은 Design 문서 참조 |

---

## 1. Overview — 화면 2개, 창 2개

`admin/generation.html`(메인 관리 화면, 같은 탭)과 `admin/generation-compose.html`(미리보기 전용, **새 창/팝업으로 오픈**)이 한 세트로 동작한다.

---

## 2. `admin/generation.html` 구성

| 영역 | 내용 |
|---|---|
| 프롬프트 템플릿 관리 | 목록, 활성화/토글/삭제, 신규 등록 폼(모달) |
| 수동 생성 폼 | 대상일/카테고리/톤/난이도 입력 + **[Codex 검증 정정] 버튼은 "지금 생성" 하나뿐**(`type="button"`, 폼 submit 아님). 클릭하면 **항상** `admin.js`가 `window.open()`으로 compose 새창을 연다 — `POST /admin/generate`를 직접 호출하는 UI 경로는 존재하지 않음(§4 참조) |
| 예약 생성 폼 | cron 조건 저장(`POST /admin/schedule`) |
| 생성 이력 | 최근 이력 목록(성공/스킵/실패 구분 표시로 추정) |

### 2.1 "지금 생성" 버튼 클릭 시 프론트 검증

`admin.js`가 `window.open()` 호출 **전에** 다음을 프론트에서 먼저 검증:
- 활성 프롬프트 템플릿 존재 여부(`hasActivePromptTemplate()`) — 없으면 `alert()`로 차단, compose 창 자체를 안 엶
- 대상일/카테고리/톤/난이도 4개 필드 전부 입력 여부

이 검증들은 **서버 측(`validatePreviewRequest` 등)에도 동일한 취지의 검증이 별도로 존재**(중복 구현). 다만 **[Codex 검증 발견] 미리보기는 LLM 호출을 먼저 하고 활성 템플릿 조회가 그 다음이라, 프론트 검증을 우회해 API를 직접 호출하면 활성 템플릿 없이도 LLM 비용이 먼저 발생할 수 있음**.

---

## 3. `admin/generation-compose.html` 구성

| 영역 | 내용 |
|---|---|
| 프롬프트 미리보기 | **[Codex 검증 정정] 프롬프트 본문과 이미지 프롬프트 템플릿은 `textarea`로 관리자가 직접 수정 가능**(활성 템플릿이 없을 때만 `disabled`) — "읽기 전용에 가까움"이라던 최초 서술은 부정확 |
| "LLM 생성" 버튼 | `POST /admin/generate/preview` 호출(수정된 프롬프트가 있으면 그 내용으로 호출) |
| 생성 결과 영역 | 제목/요약/본문 **전부 `readonly` 입력 필드** — 관리자가 직접 텍스트를 고쳐 쓸 수 없음, 오직 "LLM 생성"을 다시 눌러 재생성만 가능(단, 위 프롬프트는 수정 가능하므로 "프롬프트를 고쳐서 재생성"은 가능) |
| 이미지 미리보기 | 이미지 새로고침 버튼(`POST .../preview/image-refresh`) |
| 이전 결과 비교 영역 | 같은 대상일의 기존 콘텐츠가 있으면 나란히 표시(모두 `disabled`, 참고용) |
| 하단 액션 | "취소"(창 닫기로 추정) / "저장"(`POST /admin/generate/save`) |

### 3.1 저장 후 동작

**[Codex 검증 완료]** `admin-generation-compose.js`는 저장 성공 시 ① `window.opener`가 살아있으면 `location.reload()`로 부모 창(`generation.html`)을 새로고침 → ② compose 창을 `window.close()`. **별도 `postMessage`나 결과 데이터 전달은 없음** — 부모 창은 전체 재조회(reload)로만 최신 상태를 반영받는다. 팝업이 차단되면 이 전체 플로우가 시작조차 안 됨(브라우저 팝업 차단 설정에 의존하는 리스크).

---

## 4. 확인된 제약/특이사항 (코드 기준, 미검증 스펙 아님)

- 생성 결과 필드가 `readonly`라 관리자가 LLM 결과를 직접 미세 조정(오타 수정 등)할 방법은 없음 — 다만 프롬프트 자체는 수정 가능하므로 "프롬프트를 고쳐 재생성"으로 우회 가능
- **[Codex 검증 정정] "지금 생성" 버튼은 하나뿐이며 결과는 항상 compose 새창** — 즉시 발행(`/admin/generate`)으로 이어지는 UI 경로는 없음. 화면 설계 관점에서는 "미리보기·검토가 유일한 콘텐츠 발행 경로"로 봐야 함
- 저장 성공 후 부모창은 reload만 될 뿐 "무엇이 바뀌었는지"에 대한 별도 피드백은 compose 창이 이미 닫힌 뒤라 전달되지 않음

---

## 검증 범위 선언 (Audit)

- 브라우저 실제 렌더링/팝업 차단 동작은 확인 안 함(정적 코드 정독 + Codex 교차검증 기준)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-06 | 최초 작성 (2차 사이클, Unit 13 화면설계 정밀화) — compose 새창 연결 방식, readonly 필드 제약 확인 |
| 0.2 | 2026-08-06 | **Codex 검증으로 핵심 정정** — 버튼이 2개가 아니라 1개이며 항상 compose를 여는 것으로 정정, 프롬프트 textarea가 실제로는 수정 가능함을 확인, 저장 후 window.opener 동작(reload+close, postMessage 없음) 확정 | 
