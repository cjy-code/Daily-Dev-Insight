# Unit 15: 게시물 관리 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3

## ① 목적

지식(`DailyKnowledge`)·뉴스(`TechNews`) 게시물의 메타데이터 수정, 삭제, 썸네일 업로드/삭제를 관리자가 직접 처리.

## ② 관련 파일

- `AdminPageController` (`/admin/posts/**`, 11개 엔드포인트 — 지식/뉴스 각각 대칭 구조)
- `AdminManagementService` (update/delete/thumbnail 관련 메서드들)
- `templates/admin/posts-knowledge.html`, `admin/posts-news.html` — **`admin/posts.html`은 `/admin/posts` GET이 `/admin/posts/knowledge`로 즉시 redirect하므로 실제 렌더링되지 않음(코드로 확정 재확인됨)**
- 업로드 저장: `uploads/knowledge/{date}/`, `uploads/news/{date}/`

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/admin/posts` | `/admin/posts/knowledge`로 redirect |
| GET | `/admin/posts/knowledge` | 지식 게시물 목록 |
| GET | `/admin/posts/news` | 뉴스 게시물 목록 |
| POST | `/admin/posts/knowledge/{id}/update` | 지식 게시물 카테고리/제목 수정 |
| POST | `/admin/posts/knowledge/{id}/delete` | 지식 게시물 삭제 |
| POST | `/admin/posts/knowledge/{id}/thumbnail` | 지식 썸네일 업로드 |
| POST | `/admin/posts/knowledge/{id}/thumbnail/delete` | 지식 썸네일 삭제 |
| POST | `/admin/posts/news/{id}/update` | 뉴스 출처/제목 수정 |
| POST | `/admin/posts/news/{id}/delete` | 뉴스 삭제 |
| POST | `/admin/posts/news/{id}/thumbnail` | 뉴스 썸네일 업로드 |
| POST | `/admin/posts/news/{id}/thumbnail/delete` | 뉴스 썸네일 삭제 |

## ④ 핵심 호출 흐름

지식/뉴스 각각 **update(제한된 필드만: 지식=카테고리+제목, 뉴스=출처+제목 — 본문/요약은 이 화면에서 수정 불가)** → delete(하드 삭제로 추정, soft-delete 필드 확인 안 됨) → thumbnail 업로드(`MultipartFile`)/삭제가 대칭 구조로 반복. 모든 액션이 `try/catch(Exception)` → `adminMessage`/`adminError` flash 후 동일 목록으로 redirect(PRG 패턴).

### 처리 흐름도

```mermaid
flowchart TD
    A["GET /admin/posts/knowledge 또는 /news"] --> B["목록 조회"]
    B --> C["목록 화면 렌더링"]

    D["POST .../{id}/update"] --> E["AdminManagementService.update*Post()\n(제한된 필드만)"]
    F["POST .../{id}/delete"] --> G["AdminManagementService.delete*Post()"]
    H["POST .../{id}/thumbnail"] --> I["MultipartFile 업로드 → uploads/{type}/{date}/ 저장"]
    J["POST .../{id}/thumbnail/delete"] --> K["썸네일 파일/경로 삭제"]

    E --> L{"성공?"}
    G --> L
    I --> L
    K --> L
    L -->|예| M["adminMessage flash"]
    L -->|아니오(Exception)| N["adminError flash"]
    M --> C
    N --> C

    L -.캐시 무효화 여부.-> O["미확인 — Unit 1 홈 캐시(insightsByRange 등)\n갱신되는지 정밀화 시 확인 필요"]
```

## ⑤ 데이터/외부 연동

- 파일 업로드는 로컬 파일시스템(`uploads/{type}/{date}/`)에 저장 — S3 등 외부 스토리지 연동 없음
- 삭제된 게시물이 Unit 3(좋아요/북마크)·Unit 4(댓글) 데이터에 미치는 영향(연쇄 삭제 여부)은 이번 조사 범위 밖 — 정밀화 또는 실제 삭제 작업 전 확인 필요

## ⑥ 인증·트랜잭션·캐시

- 인증: 관리자 권한 필요
- 예외 처리: Admin 표준 패턴(광범위 catch + flash) — Unit 14와 동일
- 캐시: 게시물 수정/삭제가 Unit 1의 홈 캐시(`insightsByRange` 등)에 영향을 줄 수 있는데 이 컨트롤러 메서드들에 `@CacheEvict`가 있는지는 미확인 — **정밀화 시 우선 확인 대상** (없다면 게시물 수정 후 홈 화면에 최대 5분(캐시 TTL) 동안 구 데이터가 노출될 수 있음)

## ⑦ 화면 요약

- 지식/뉴스 각각 목록 테이블 + 인라인 수정 폼 + 썸네일 업로드 UI

## ⑧ 패턴 특이사항

- 지식/뉴스가 완전히 대칭 구조(엔드포인트 4개씩)이지만 **수정 가능 필드가 다름**(지식=카테고리/제목, 뉴스=출처/제목) — 콘텐츠 타입별 스키마 차이가 반영된 것으로 자연스러움
- `admin/posts.html`이 실제로는 안 쓰이는 죽은 템플릿으로 최종 확인됨 (`full-documentation-initiative.md` §2.3 Out of Scope 항목 확정)

## ⑨ 알아둘 점 / 리스크

- **캐시 무효화 여부 미확인** — 정밀화 우선순위 후보
- 삭제가 하드 삭제인지, 삭제된 게시물을 참조하는 좋아요/북마크/댓글/조회 이력이 고아 데이터로 남는지 확인 안 됨

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) — `admin/posts.html` 미사용 최종 확정 |
| 0.2 | 2026-08-06 | 처리 흐름도(Mermaid) 추가 |
