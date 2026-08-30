# 오늘의 개발 트렌드 생성 이력 중복 적재 (Daily Trend Generation Duplicate History) Planning Document

> **Summary**: 관리자 `게시물 관리 > 오늘의 개발 트렌드`에서 `트렌드 생성` 버튼 클릭 시, 제출 중복 방지 로직이 없어 한 번 클릭에 이력이 여러 건 쌓이는 문제. 원인 진단까지 완료, 실제 수정은 보류
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-30
> **Status**: Fixed (같은 날 세션에서 클라이언트/서버 양쪽 방어 로직 구현 완료, 컴파일 확인)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 사용자가 관리자 화면에서 "오늘의 개발 트렌드 이력을 보니 한 번 클릭했는데 왜 여러 개가 찍히는지" 질문 → 코드 조사 + 실제 로컬 DB(Oracle, Docker) 이력 데이터로 원인 확인 |
| **WHO** | 관리자 콘솔에서 트렌드를 수동 생성하는 운영자(사용자 본인) |
| **RISK** | 이력 테이블(`daily_trend_generation_history`)에 노이즈성 row가 계속 쌓임 — 이력 화면 가독성 저하. 실제 트렌드 데이터(`daily_trend_insight`)는 `trendDate` upsert라 중복 생성은 안 되지만, 중복 클릭 시 불필요한 LLM API 호출이 두 번 발생해 **비용/쿼터 낭비** 가능성 있음 (OpenAI API 사용 시) |
| **SUCCESS** | (수정 시점에 재정의) 버튼 클릭 후 응답이 올 때까지 재클릭이 서버에 두 번째 요청으로 전달되지 않음 — 이력에 의도한 시도 건수만큼만 기록됨 |
| **SCOPE** | 이 문서는 **원인 진단 결과 기록까지만** 담당. 실제 코드 수정(버튼 비활성화/로딩 표시, 서버 측 방어 등)은 다음 세션에서 진행 |

---

## 1. Overview

### 1.1 진단 결과

**증거** — 로컬 Docker(Oracle+Redis) + `bootRun`으로 띄운 실제 앱의 `/admin/crawling` 페이지, `daily_trend_generation_history` 실데이터:

```
2026-08-30 03:40:49  MANUAL  대상일 2026-08-30  FAILED
2026-08-30 03:40:53  MANUAL  대상일 2026-08-30  FAILED   ← 4초 뒤 동일 조건으로 한 번 더
```

같은 대상일에 대해 4초 간격으로 `MANUAL` 이력이 2건 — 클릭 한 번이 의도치 않게 두 번 제출된 정황과 일치.

**코드 레벨 원인** 3가지가 겹쳐서 발생:

1. `backend/src/main/resources/templates/admin/crawling.html:403-412`의 `.daily-trend-generate-form`은 순수 `<form method="post">` + `<button type="submit">`이고, 제출 시 버튼을 비활성화하거나 로딩 상태를 표시하는 JS가 전혀 없음. `admin.js` 전체를 뒤져봐도 이 폼에 바인딩된 스크립트가 없음 — 크롤링 수동 실행 폼(`manualForm`, `admin.js:1994` 부근)이나 cron 설정 폼(`admin.js:1034` 부근)엔 유사한 제출 가드/확인 로직이 있는데 이 폼만 빠져 있음
2. `DailyTrendInsightService.generateDailyTrend()`(`backend/src/main/java/com/dailydevinsight/admin/service/DailyTrendInsightService.java:129-169`)는 호출될 때마다 성공/실패 여부와 무관하게 무조건 `saveGenerationHistory()`로 새 이력 row를 저장 — 같은 대상일 재시도에 대한 멱등/디바운스 처리가 없음
3. `SecurityConfig`가 커스터마이징한 CSRF 토�큰 저장소가 없어 Spring Security 기본값(세션 단위 재사용 가능 토큰)을 사용 — 두 번째 제출도 CSRF 검증을 그대로 통과해 에러 없이 조용히 이력만 중복됨

즉: LLM 호출로 응답이 느릴 수 있는데 로딩 표시가 없어 사용자가 재클릭하거나, 더블클릭이 그대로 두 번의 POST로 이어지고, 서버는 이를 막을 장치가 없어 이력이 그대로 중복 적재되는 구조.

### 1.2 부수 관찰 (별도 이슈, 참고만)

- 같은 이력 테이블에 `SCHEDULED` 트리거가 1분 간격(`03:40:00`, `03:41:00`, `03:42:00`, `03:43:00`)으로 연속 실패 기록되어 있음 — cron이 매분 도는 것처럼 보이는 이상 패턴. 이번 "클릭 중복" 이슈와는 원인이 다를 가능성이 높아 **본 문서 범위 밖**으로 분리. 다음 세션에서 `ScheduledGenerationExecutor`/cron 설정을 별도로 확인 필요
- 이번 로컬 재현 환경은 `OPENAI_API_KEY` 미설정 상태라 모든 시도가 `FAILED`로 기록됨 — 중복 제출 자체는 API 키 유무와 무관한 클라이언트/서버 로직 문제

---

## 2. Scope (수정 시점에 확정할 것)

### 2.1 다뤄볼 후보

- [x] `.daily-trend-generate-form` 제출 시 버튼 비활성화 + 로딩 상태 표시 (`admin.js`에 `bindDailyTrendGenerateSubmitGuard()` 추가, `initializeAdminPage()`에 등록)
- [x] 서버 측 방어 — `DailyTrendInsightService`에 `ConcurrentHashMap` 기반 대상일별 진행 중 락(`inProgressTrendDates`) 추가. 동일 대상일 재요청이 겹치면 LLM 호출/이력 저장 없이 즉시 `IllegalStateException`으로 거부(관리자 화면에 에러 메시지로 노출)
- [ ] (부수 관찰) `SCHEDULED` 트리거가 1분 간격으로 도는 것처럼 보이는 cron 설정 확인 — 이번 수정 범위 밖, 별도 이슈로 남겨둠

### 2.2 Out of Scope (이번 진단 시점 기준)

- 실제 코드 수정 — 다음 세션에서 별도로 진행
- OpenAI API 키 설정 등 로컬 개발 환경 구성 (이번 진단과 무관)

---

## 3. Next Steps

1. [x] `.daily-trend-generate-form`에 다른 폼과 동일한 수준의 제출 가드(버튼 비활성화/로딩 표시) 추가 — 2026-08-30
2. [x] 서버 측 중복 방지 추가 — 대상일별 진행 중 락으로 동시/재요청 즉시 거부 — 2026-08-30
3. [ ] `SCHEDULED` 1분 간격 이상 패턴 별도 확인 — 미착수, 별도 이슈로 분리

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-30 | 최초 작성 — 실 DB 이력 기반 원인 진단 완료, 수정은 다음 세션으로 연기 | Claude |
| 0.2 | 2026-08-30 | 같은 날 세션에서 수정 완료 — 클라이언트 제출 가드(`admin.js`) + 서버 진행 중 락(`DailyTrendInsightService`) 구현 | Claude |
