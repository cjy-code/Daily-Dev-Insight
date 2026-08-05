# Unit 2: 인사이트 상세 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 1 / §2.4 리스크 기반 승격 1차 후보 (2차 사이클에서 Plan+Design+Screen+Analysis+Report로 정밀화 예정)
> **Unit 3(좋아요/북마크), Unit 4(댓글/대댓글)는 이 카드를 기준 문서로 delta card로 연결됨**

## ① 목적

지식/뉴스 콘텐츠 상세 조회 + 조회수 집계 + 좋아요/북마크/댓글(대댓글 포함) 상호작용을 하나의 컨트롤러·서비스가 처리한다.

## ② 관련 파일

- `InsightPageController.insightDetail()` (화면 렌더링), `InsightDetailRestController` (`/api/insights/{type}/{id}/**`, 상호작용 API)
- `InsightDetailService` (모든 로직의 실제 처리자)
- Entity: `InsightLike`, `InsightBookmark`, `InsightComment`
- Repository: `InsightLikeRepository`, `InsightBookmarkRepository`, `InsightCommentRepository`, `DailyKnowledgeRepository`, `TechNewsRepository`, `UserRepository` (총 6개 저장소 사용)
- DTO: `InsightDetailResponseDTO`, `InsightToggleResponseDTO`, `InsightCommentDTO`, `InsightCommentRequestDTO`
- `templates/insight-detail.html`

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/insights/{type}/{id}` | 화면 렌더링 (MVC, `InsightPageController`) + 조회수 증가(세션당 1회) |
| GET | `/api/insights/{type}/{id}` | 집계 상태만 조회 (REST, 캐시됨) |
| POST | `/api/insights/{type}/{id}/likes/toggle` | 좋아요 토글 |
| POST | `/api/insights/{type}/{id}/bookmarks/toggle` | 북마크 토글 |
| POST | `/api/insights/{type}/{id}/comments` | 댓글/대댓글 등록 |
| DELETE | `/api/insights/{type}/{id}/comments/{commentId}` | 본인 댓글 삭제(소프트 삭제) |

## ④ 핵심 호출 흐름

1. `{type}`은 `InsightContentType.from()`으로 `KNOWLEDGE`/`NEWS` 중 해석, 잘못된 값이면 400
2. 화면 진입 시 세션 키(`insight:viewed:{type}:{id}`)로 조회수 중복 증가 방지 — 세션 없으면 최초 1회만 `incrementViewCount()` (엔티티별 UPDATE 쿼리) 후 `getEngagementOnly` 캐시 전체 clear
3. 좋아요/북마크: `findByContentTypeAndContentIdAndUserId` 존재 여부로 insert/delete 토글, 이후 count 재조회해 응답
4. 댓글: 부모 댓글 있으면 존재/삭제여부/동일 콘텐츠 검증(`validateParentCommentId`) 후 저장. 댓글 목록은 `parentCommentId` 기준으로 메모리에서 트리 조립(재귀 쿼리 아님)
5. 댓글 삭제는 물리 삭제가 아니라 `isDeleted` 플래그 처리(`markDeleted()`)

## ⑤ 데이터/외부 연동

- `userId`는 로그인 `Authentication.getName()`(이메일 등 loginUserId)을 `UserRepository.findByUserId()`로 내부 PK(`Long`)로 변환해서 사용 — 로그인 안 되어 있으면 401
- 외부 API 연동 없음

## ⑥ 인증·트랜잭션·캐시

- 인증: 좋아요/북마크/댓글은 `resolveUserId()`에서 `loginUserId` 비어있으면 즉시 401 — 로그인 필수
- 트랜잭션: **클래스 전체가 `@Transactional`(쓰기, readOnly 아님)** — Unit 1의 `DailyInsightService`(클래스 전체 `readOnly`)와 다름, 조회 메서드(`getInsightDetail`, `getEngagementOnly`)도 쓰기 트랜잭션 안에서 실행됨
- 캐시: `getEngagementOnly`만 `@Cacheable`(Redis), 나머지 쓰기 계열 메서드는 전부 `@CacheEvict(allEntries = true)` — **좋아요 하나 눌러도 이 캐시의 전체 엔트리가 삭제됨** (특정 콘텐츠만 evict 하는 게 아님)

## ⑦ 화면 요약

- 상세 콘텐츠(제목/요약/본문/썸네일/출처/발행일/조회수) + 좋아요·북마크 버튼(카운트 포함) + 댓글 목록(대댓글 들여쓰기) + 댓글 입력창

## ⑧ 패턴 특이사항 (다른 unit과 다르게 구현된 부분)

- **예외 처리 스타일이 Admin 영역과 다름**: 이 서비스는 `ResponseStatusException`으로 직접 HTTP 상태코드(400/401/403/404)를 던지고 컨트롤러는 별도 catch를 안 함. 반면 `AdminPageController`는 광범위한 `catch (Exception)`으로 잡아 flash 메시지로 전환 — 같은 "사용자 액션 실패" 상황을 서로 다른 방식으로 처리
- **트랜잭션 범위가 Unit 1과 다름**: `DailyInsightService`는 클래스 전체 `readOnly`, 이 서비스는 조회까지 포함해 클래스 전체가 쓰기 트랜잭션
- **캐시 무효화가 세밀하지 않음**: 콘텐츠 1건에 대한 변경(좋아요 등)이 `CACHE_INSIGHT_ENGAGEMENT` 캐시의 **모든 콘텐츠 엔트리**를 지움(`allEntries = true`) — 트래픽이 늘면 캐시 효율이 떨어질 수 있는 구조
- 소스 코드 내 한글 주석 일부가 인코딩 깨짐(mojibake) 상태로 저장되어 있음 — 기능에는 영향 없으나 문서 작성 시 원본 대신 코드 로직으로만 판단함

## ⑨ 알아둘 점 / 리스크

- 좋아요/북마크는 엔티티 자체가 삭제되는 방식이라 "해제했다가 다시 좋아요" 이력은 보존되지 않음 (`MVP_SCOPE.md`에 이미 명시)
- 댓글은 소프트 삭제(`isDeleted`)라 원본 데이터는 DB에 남음 — 별도 하드 삭제/보존 정책 문서 없음
- **§2.4 승격 후보로 지정됨** — 2차 사이클에서 Plan/Design/Screen/Analysis/Report로 정밀화 예정 (이유: 6개 저장소·캐시·인증이 얽힌 가장 복잡한 사용자 영역이며 Unit 3·4가 여기 의존)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
