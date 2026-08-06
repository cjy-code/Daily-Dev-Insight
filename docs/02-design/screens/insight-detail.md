# [화면] 인사이트 상세 (`insight-detail.html`)

> **Project**: dailyDevInsight | **Date**: 2026-08-06
> **Status**: 2차 사이클 정밀화 (§2.4 승격 후보, 1차 compact card `docs/02-design/features/insight-detail.md`에서 확장)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 지식/뉴스 콘텐츠의 본문을 보여주고, 좋아요·북마크·댓글로 사용자가 반응할 수 있는 유일한 화면 |
| **WHO** | 인증된 사용자 (`/insights/{type}/{id}`는 user 필터체인 하위, 로그인 필요) |
| **RISK** | 좋아요/북마크/댓글 액션은 클라이언트 JS가 전부 처리 — JS 로드 실패 시 상호작용 불가(SSR 텍스트만 노출) |
| **SUCCESS** | 상세 콘텐츠 표시 + 조회수 1회 반영 + 좋아요/북마크 토글 + 댓글(무제한 depth) 작성/삭제가 새로고침 없이 동작 |
| **SCOPE** | `insight-detail.html` + `insight-detail.js` 렌더링/상호작용만. 서버 로직 상세는 Design 문서(`docs/02-design/features/insight-detail.md`) 참조 |

---

## 1. Overview

### 1.1 렌더링 방식 — SSR 초기 페인트 + 클라이언트 즉시 재조회(이중 렌더링)

1. 서버(`InsightPageController.insightDetail()`)가 세션 기준 조회수 증가 여부를 직접 판단한 뒤 `InsightDetailService.getInsightDetail()`로 상세+집계+최상위 댓글을 조회해 Thymeleaf로 최초 HTML을 렌더링
2. 스크립트가 로드되는 즉시(별도 `DOMContentLoaded` 대기 없이) `insight-detail.js`의 `initDetailPage()`가 실행되어 `GET /api/insights/{type}/{id}`(캐시된 `getEngagementOnly`)를 호출 — **[Codex 검증 정정] 이 응답으로 좋아요/북마크/댓글 영역 전부가 아니라, 집계 수치(`renderEngagement`)와 댓글 목록(`renderComments`)만 다시 그림. 이후의 좋아요/북마크 클릭 자체는 이 재조회를 다시 트리거하지 않고 응답 카운트만 부분 갱신함(§3 참조)**
3. **[Codex 검증 정정] SSR 단계에서는 대댓글이 전혀 표시되지 않음** — 아래 §2 참조. 최초 페인트와 JS 재조회 후 화면이 다른 것은 "잠깐 보였다 대체"가 아니라 **"최상위 댓글만 보이던 화면 → 대댓글까지 포함한 전체 트리로 확장"**되는 것에 가까움

### 1.2 관련 파일

- `templates/insight-detail.html`, `static/js/insight-detail.js`, `static/css/insight-detail.css`
- Design 문서: `docs/02-design/features/insight-detail.md`

---

## 2. 화면 구성

| 영역 | 내용 | 렌더링 주체 |
|---|---|---|
| 헤더 | 제목, 요약, 썸네일(있으면), 출처+발행일 | SSR (JS가 다시 안 그림) |
| 반응 패널 | 조회수/좋아요/북마크/댓글 수 + 좋아요·북마크 버튼 + 원문 링크(있으면) | SSR 최초 표시(`#lists.size(detail.comments)`로 **최상위 댓글 수만** 계산) → **JS가 재귀 합산한 정확한 값으로 덮어씀**(`renderEngagement`) |
| 본문 | 상세 텍스트 | SSR (JS가 다시 안 그림) |
| 댓글 섹션 | 댓글 입력 폼 + 댓글 목록 | **[Codex 검증 정정] SSR은 `th:each="comment : detail.comments"`로 최상위 댓글만 반복 — `comment.replies`(대댓글)는 서버 템플릿에서 전혀 출력되지 않음.** JS 재조회 후에야 `renderComments`가 대댓글까지 포함한 전체 트리를 그림 |

### 2.1 댓글 트리 렌더링

**SSR(최초 페인트)**: 최상위 댓글만 노출, 대댓글은 보이지 않음. 각 최상위 댓글에 답글 버튼과 빈 `reply-form-slot`만 존재.

**JS 재조회 후(사실상 즉시)**: `buildCommentHtml(comment, depth)`가 **재귀 호출**로 `comment.replies` 배열을 얼마든지 깊게 렌더링 — **depth 상한 없음**. `depth > 0`인 항목도 답글 버튼/입력 폼이 depth 0과 동일하게 노출됨 → **대댓글에 또 답글을 달 수 있는 UI가 실제로 존재**(Design 문서 §8, findings F-05 — 서버(`InsightDetailService.validateParentCommentId`)와 JS 양쪽 모두 Codex 검증으로 재확인됨)
- 답글 폼은 클릭 시 `[data-reply-slot]`에 동적 삽입/제거(토글), 별도 라우팅 없이 같은 페이지 내 DOM 조작

### 2.2 빈 상태 / 에러 상태

| 상황 | 처리 |
|---|---|
| 댓글 0건 | `renderComments`가 `<li class="comment-empty">등록된 댓글이 없습니다.</li>` 삽입 |
| 초기 `GET /api/insights/{type}/{id}` 실패 | `.catch()`가 댓글만 빈 배열로 재렌더링 — **집계 수치(조회수 등)는 SSR 값 그대로 남고 갱신 안 됨** (에러 시 부분적으로만 복구) |
| 좋아요/북마크/댓글 액션 실패 | `window.alert(error.message)` — 서버가 던진 메시지(`ResponseStatusException`의 reason)를 그대로 노출 |
| 썸네일 없음/로드 실패 | `onerror`로 `/images/default-thumb.svg` 대체 |

---

## 3. 액션 → API 매핑

| UI 액션 | 호출 | 성공 시 갱신 |
|---|---|---|
| 페이지 로드 | `GET /api/insights/{type}/{id}` | 집계+댓글 전체 재렌더링 |
| 좋아요 버튼 클릭 | `POST .../likes/toggle` | 버튼 active 상태 + 카운트만 갱신(댓글 재렌더링 안 함) |
| 북마크 버튼 클릭 | `POST .../bookmarks/toggle` | 버튼 active 상태 + 카운트만 갱신 |
| 댓글 등록 | `POST .../comments` (`parentCommentId: null`) | 응답으로 받은 전체 상태로 `renderFromState` 재호출(집계+댓글 전부 다시 그림) |
| 답글(대댓글) 등록 | `POST .../comments` (`parentCommentId: <부모ID>`) | 동일하게 `renderFromState` 전체 재호출 |
| 댓글/답글 삭제 | `DELETE .../comments/{commentId}` (`window.confirm` 확인 후) | 동일하게 `renderFromState` 전체 재호출 |

> 좋아요/북마크는 "부분 갱신"(카운트만), 댓글 관련은 "전체 재렌더링" — 두 종류 액션이 갱신 범위가 다름(성능상 의도적 차이로 추정, 명시적 근거는 없음).

---

## 4. 확인된 제약/특이사항 (코드 기준, 미검증 스펙 아님)

- CSRF 토큰은 `<meta name="_csrf">`에서 읽어 모든 POST/DELETE 요청 헤더에 첨부(`buildCsrfHeaders`)
- 댓글 수 표시(`data-comment-count`)는 **대댓글까지 재귀 합산**(`countTotalComments`) — 최상위 댓글 개수가 아니라 전체 트리 노드 수
- 좋아요/북마크 실패 시에도 버튼 클릭 자체는 막지 않음(로딩 상태/중복 클릭 방지 로직 없음) — 짧은 시간 연속 클릭 시 서버에 중복 요청이 갈 수 있음(서버는 toggle이라 최종 상태는 correct하지만 중간 카운트가 깜빡일 수 있음)

---

## 검증 범위 선언 (Audit)

- 브라우저 실제 렌더링/네트워크 탭 확인은 하지 않음 — HTML/JS 정적 코드 정독 기준
- `insight-detail.css`는 레이아웃 스타일만 담당할 것으로 보여 별도 분석 생략

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-06 | 최초 작성 (2차 사이클, Unit 2 화면설계 정밀화) — 이중 렌더링 구조, 댓글 무제한 depth UI 확인 |
