# Unit 13: AI 생성 관리 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3 / §2.4 리스크 기반 승격 1차 후보 (가장 복잡한 관리자 흐름 — 2차 사이클에서 정밀화 예정)

## ① 목적

일일 개발 지식 콘텐츠를 AI로 생성(자동 예약 + 수동)하고, 프롬프트 템플릿을 관리하며, 텍스트 미리보기 후 저장하는 2단계(preview→save) 워크플로우를 제공.

## ② 관련 파일

- `AdminPageController` (`/admin/generation`, `/admin/generate*`, `/admin/prompts*`, `/admin/schedule` — 총 11개 엔드포인트)
- `DailyKnowledgeGenerationService` (핵심 로직, 700줄 이상)
- `LlmGenerationClient`(텍스트, weekly-ai-insight와 인터페이스 공유), `OpenAiImageGenerationClient`(썸네일 이미지)
- `GenerationScheduleService`(Unit 7과 연동), `PromptTemplateService`, `GenerationHistoryService`
- `templates/admin/generation.html`(목록/스케줄/프롬프트), `admin/generation-compose.html`(수동 생성 작성 화면) + `admin-generation-compose.js`
- `WebResourceConfig`/`/uploads/**` — 생성된 이미지 저장·서빙 경로 (`uploads/knowledge/{date}`)

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/admin/generation` | 생성 이력/스케줄/프롬프트 관리 화면 |
| GET | `/admin/generation/compose` | 수동 생성 작성 화면 |
| POST | `/admin/prompts` | 프롬프트 템플릿 생성 |
| POST | `/admin/prompts/{id}/activate` | 특정 템플릿 활성화(단일 활성 템플릿 정책으로 추정) |
| POST | `/admin/prompts/{id}/toggle-active` | 템플릿 활성/비활성 토글 |
| POST | `/admin/prompts/{id}/delete` | 템플릿 삭제 |
| POST | `/admin/generate` | (구) 즉시 생성 — `executeManualGeneration` |
| POST | `/admin/generate/preview` | 미리보기 생성(저장 안 함) |
| POST | `/admin/generate/preview/image-refresh` | 미리보기의 썸네일 이미지만 재생성 |
| POST | `/admin/generate/save` | 미리보기 결과를 확정 저장 |
| POST | `/admin/schedule` | 자동 생성 예약 조건 저장 (Unit 7이 폴링) |

## ④ 핵심 호출 흐름

1. **두 가지 생성 경로가 공존**: `/generate`(즉시 실행+저장, 구형 플로우로 추정) vs `/generate/preview` → `/generate/save`(미리보고 확정하는 2단계 플로우, `generation-compose.html`이 사용) — 두 경로가 병존하는 이유는 코드 주석에 없음
2. 미리보기(`previewManualGeneration`): 텍스트+이미지 프롬프트를 렌더링해 LLM/이미지 클라이언트 호출, **DB 저장 없이** 결과만 반환
3. 이미지만 재생성(`refreshPreviewImage`): 텍스트는 유지하고 이미지만 다시 생성 — 미리보기 화면에서 이미지 재시도 버튼 대응으로 추정
4. 저장(`saveManualGenerationFromPreview`): 미리보기에서 승인한 내용을 실제로 `DailyKnowledge`로 저장 + 이력(`GenerationHistory`) 기록
5. 예약 생성(`executeScheduledGeneration`, Unit 7이 트리거): 미리보기 단계 없이 바로 생성+저장, 성공/스킵/실패 각각 별도 이력 저장 메서드(`saveSuccessHistory`/`saveSkippedHistory`/`saveFailureHistory`)

## ⑤ 데이터/외부 연동

- **LLM 텍스트 생성**: `LlmGenerationClient`(mock/openai 전환, weekly-ai-insight와 동일 인터페이스 계열이지만 메서드는 별개)
- **이미지 생성**: `OpenAiImageGenerationClient` — 별도 외부 API 호출, 생성 실패 시 처리 방식은 정밀화 시 확인 필요
- 업로드 저장: `uploads/knowledge/{date}/` 로컬 파일시스템(런타임 생성물, git 미추적 — `.gitignore`에 이미 반영됨)

## ⑥ 인증·트랜잭션·캐시

- 인증: `/admin/**`, 관리자 권한 필요
- 캐시 무효화: 생성 성공 시 `CACHE_WEEKLY_TOP10` 등을 `@CacheEvict`(Unit 9에서 확인된 사용처) — 새 지식이 주간 TOP 순위에 영향을 주므로 관련 캐시를 비움
- 트랜잭션 범위는 메서드별로 상이한 것으로 보임(정밀화 시 클래스/메서드 단위 확인 필요)

## ⑦ 화면 요약

- `generation.html`: 생성 이력 목록, 예약 조건 설정, 프롬프트 템플릿 CRUD(활성 템플릿 표시)
- `generation-compose.html`: 카테고리/톤/난이도 선택 → 미리보기 → (이미지 재생성 가능) → 저장, JS(`admin-generation-compose.js`)가 미리보기↔저장 단계를 클라이언트에서 조율

## ⑧ 패턴 특이사항

- **동일 목적(콘텐츠 생성)에 대해 "즉시 실행"(`/generate`)과 "미리보기 후 저장"(`/generate/preview`+`/generate/save`) 두 플로우가 공존** — 어느 게 현재 주력 UI인지, 다른 하나는 레거시인지 화면 확인 없이는 판단 어려움 (정밀화 시 `generation.html`이 실제로 `/generate`를 호출하는지 확인 필요)
- 이 unit이 이 프로젝트에서 **가장 많은 외부 연동(LLM 텍스트 + 이미지 API)과 가장 많은 캐시 무효화 지점**을 가진 영역

## ⑨ 알아둘 점 / 리스크

- **§2.4 승격 후보로 지정됨** — 2차 사이클에서 정밀화 예정. 우선 확인 대상: (1) `/generate` vs `/generate/preview`+`/generate/save` 두 플로우의 실제 사용처, (2) 이미지 생성 실패 시 텍스트 저장 여부(부분 실패 처리), (3) 미리보기 데이터의 임시 보관 방식(세션? DB 임시 저장?)
- 프롬프트 템플릿의 "활성화"(`activate`)와 "토글"(`toggle-active`)이 별도 엔드포인트로 존재 — 의미 차이가 이름만으로는 불명확(activate는 단일 활성 강제, toggle은 단순 on/off로 추정되나 확인 필요)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
