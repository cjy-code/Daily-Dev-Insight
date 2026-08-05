# Unit 12: 크롤링 관리 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3 / **화면설계는 `docs/02-design/screens/admin-crawling.md`(기존 존재, 코드 기준 상세 문서) 참조 — 여기서는 중복 서술하지 않음**

## ① 목적

RSS 기반 테크 뉴스 크롤링을 수동 실행/예약 실행하고, 크롤링 조건 프리셋을 관리한다. 같은 화면(`admin/crawling.html`)에 주간 AI 인사이트 생성 UI도 포함되어 있으나 그건 기존에 별도 문서화됨(`docs/01-plan~04-report/*/weekly-ai-insight.md`).

## ② 관련 파일

- `AdminPageController` (`/admin/crawling` GET 1 + POST 4 = 5개, 주간 AI 인사이트 POST 2개는 별도 문서)
- `TechNewsCrawlingService`, `CrawlScheduleService`, `CrawlHistoryService`, `CrawlConditionPresetService`
- Entity: `CrawlSchedule`, `CrawlHistory`, `CrawlConditionPreset`
- `templates/admin/crawling.html` — **화면 구성 상세는 기존 문서(`docs/02-design/screens/admin-crawling.md`) 참조**

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/admin/crawling` | 화면 렌더링(크롤링 이력, 예약 설정, 프리셋, 주간AI 인사이트 함께 표시) |
| POST | `/admin/crawling/run` | 수동 크롤링 즉시 실행 |
| POST | `/admin/crawling/schedule` | 크롤링 예약 조건(cron 등) 저장 — Unit 7 스케줄러가 이 조건을 폴링 |
| POST | `/admin/crawling/preview` | 크롤링 조건 미리보기(실행 없이 결과 예측 추정 — 정밀화 시 확인) |
| POST | `/admin/crawling/presets` | 크롤링 조건 프리셋 저장/관리 |

## ④ 핵심 호출 흐름 (요약)

수동 실행(`/run`)은 `TechNewsCrawlingService`를 즉시 호출해 결과를 이력(`CrawlHistory`)에 남김. 예약 실행(`/schedule`)은 조건만 `CrawlSchedule`에 저장하고 실제 실행은 Unit 7(`ScheduledCrawlingExecutor`)이 매분 폴링해서 처리 — **이 화면 자체는 트리거만 하고 실행 로직은 소유하지 않음**. 상세 조건/중복정책은 기존 화면설계 문서 §5(확인된 제약) 참조.

## ⑤~⑦ 데이터/화면

기존 문서(`docs/02-design/screens/admin-crawling.md`) §2~5에 상세 기술됨 — 여기서 재작성하지 않고 참조로 대체.

## ⑧ 패턴 특이사항

- **한 화면(`admin/crawling.html`)에 서로 다른 두 기능(크롤링 관리 + 주간 AI 인사이트)이 같이 존재** — Unit 경계(§2.4)와 실제 화면 경계가 일치하지 않는 사례. 문서는 기능 단위로 분리했지만 화면은 하나
- 예약 실행 조건 저장(`/schedule`)과 실제 실행(Unit 7)이 물리적으로 다른 클래스에 있어, 이 unit만 보면 "예약이 실제로 언제 실행되는지"가 안 보임 — Unit 7과 반드시 같이 봐야 전체 그림이 완성됨

## ⑨ 알아둘 점 / 리스크

- 기존 화면설계 문서가 "코드 기준, 미검증 스펙 아님"이라는 검증 범위 선언을 이미 포함하고 있음 — 이 compact card도 같은 전제를 따름 (실제 브라우저 동작 미검증)
- 정밀화(2차 사이클) 시에는 이 카드보다 기존 화면설계 문서 쪽을 기준 삼아 Plan/Analysis/Report만 보완하는 방향이 효율적

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card, 화면설계는 기존 문서 참조로 대체) |
