# Unit 7: 스케줄러 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 2 (화면 없음)

## ① 목적

관리자가 설정한 예약 조건(cron 표현식, DB 저장)에 따라 뉴스 크롤링과 일일 지식 생성을 자동 실행한다.

## ② 관련 파일

- `ScheduledCrawlingExecutor`, `ScheduledGenerationExecutor` (매분 폴링 진입점)
- `CrawlScheduleService`, `GenerationScheduleService` (실행 조건 판단 + 상태 갱신)
- Entity: `CrawlSchedule`, `GenerationSchedule` (cron 표현식, `enabled`, `lastExecutedAt` 등 저장)
- `TechNewsCrawlingService.executeScheduledCrawling()`, `DailyKnowledgeGenerationService.executeScheduledGeneration()` (Unit 12·13이 다루는 서비스를 그대로 호출 — 교차 참조)

## ③ 진입 엔드포인트

없음 (화면/API 없음). `@Scheduled(cron = "0 * * * * *")`로 애플리케이션 기동 중 매분 자동 실행. 예약 조건 자체는 `admin/crawling.html`(Unit 12), 생성 화면(Unit 13)에서 설정.

## ④ 핵심 호출 흐름

1. 매분: `isExecutionDue(now)` 체크 — `schedule.enabled=false`면 즉시 skip
2. **DB에 저장된 cron 표현식**을 매번 파싱(`CronExpression.parse`)해 "마지막 실행 시각(`lastExecutedAt`, 없으면 `now-1분`) 기준 다음 실행 시각"이 현재 시각을 넘지 않았는지로 실행 여부 판단 — Spring의 `@Scheduled(cron=...)` 자체는 고정 1분 폴링만 담당하고, **실제 사용자 지정 주기는 이 자체 로직이 처리**
3. 조건 충족 시 실제 작업(크롤링/생성) 실행 → 성공하면만 `markExecuted(now)`로 `lastExecutedAt` 갱신 (실패하면 다음 폴링에서 재시도됨)

## ⑤ 데이터/외부 연동

- 실제 크롤링/생성 작업 자체는 Unit 12(`TechNewsCrawlingService`)·Unit 13(`DailyKnowledgeGenerationService`)에 위임 — 이 unit은 "언제 실행할지"만 담당

## ⑥ 인증·트랜잭션·캐시

- 인증 없음(내부 스케줄러, HTTP 요청 아님)
- `markExecuted()`만 `@Transactional`, `isExecutionDue()`는 트랜잭션 어노테이션 없음(단순 조회이나 `getOrCreateSchedule()`이 없으면 생성까지 하므로 쓰기 가능성 있음 — 정밀화 시 확인 필요)
- 캐시 없음

## ⑦ 화면 요약

없음 (화면 없는 unit)

## ⑧ 패턴 특이사항

- **`CrawlScheduleService`와 `GenerationScheduleService`의 `isExecutionDue`/`markExecuted` 로직이 거의 동일하게 중복 구현됨** — 공통 추상화 없이 크롤링용/생성용이 각각 별도 클래스로 복붙에 가깝게 존재
- Spring 네이티브 cron이 아니라 "매분 폴링 + DB cron 재해석" 방식 — 실제 예약 주기가 1분보다 촘촘할 수 없고, cron 파싱을 매분 새로 하는 구조라 표현식이 많아지면(현재는 2개뿐) 비효율 소지

## ⑨ 알아둘 점 / 리스크

- 크롤링/생성 실행 결과가 실패해도 예외를 던지지 않고 `ExecutionResult.isSuccess()`로만 판단 — 실패 시 로그만 남고 별도 알림/재시도 정책 없음
- `markExecuted`가 실행 "성공" 시에만 갱신되므로, 계속 실패하는 경우 매분마다 재시도가 발생할 수 있음(무한 재시도 방지 로직 없음 — 정밀화 시 확인 필요)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
