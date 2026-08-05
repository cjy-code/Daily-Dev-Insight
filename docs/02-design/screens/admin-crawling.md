# [화면] 관리자 - 크롤링 관리 (`admin/crawling.html`)

> **Summary**: 뉴스 크롤링의 조건 프리셋 관리, 수동/예약 실행, 주간 AI 인사이트 생성, 실행 이력 조회를 한 화면에서 처리하는 관리자 탭 페이지
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **Status**: **Implemented (사후 문서화)** — 코드가 이미 구현되어 있으며, 본 문서는 실제 구현을 기준으로 작성됨 (SoR: 코드 우선)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 크롤링 조건/실행/예약/이력이 각각 흩어져 있으면 관리자가 맥락을 잃기 쉬움 → 탭 하나로 통합 관리 |
| **WHO** | 관리자만 접근 (`/admin/**`) |
| **RISK** | 크롤링 실행은 외부 RSS 호출을 동반하므로 실패/타임아웃 가능, 예약 설정은 잘못 저장 시 스케줄러가 의도치 않게 반복 실행됨 |
| **SUCCESS** | 관리자가 프리셋을 적용/직접입력해 미리보기 후 저장 실행할 수 있고, 예약을 켜고 끌 수 있으며, 실행 이력과 오류를 확인할 수 있다 |
| **SCOPE** | 이 문서는 화면 구조·상호작용·API 매핑만 다룸. 크롤링 로직 자체(`TechNewsCrawlingService` 내부 동작)와 주간 AI 인사이트 생성 로직 상세는 `docs/02-design/features/weekly-ai-insight.md` 참고 |

---

## 1. Overview

### 1.1 목적

관리자가 뉴스 크롤링을 조건별로 반복 재사용(프리셋)하고, 즉시 실행(수동)하거나 주기적으로 실행(예약)하도록 설정하며, 실행 결과를 이력으로 추적한다. 같은 화면에서 주간 AI 인사이트 생성/노출 제어도 처리한다.

### 1.2 진입 경로

- `GET /admin/crawling` (`AdminPageController.crawlingPage`, `AdminPageController.java:188`)
- 좌측 `fragments/adminNav`에서 "크롤링" 메뉴로 진입 (`currentMenu = "crawling"`)

### 1.3 관련 파일

| 레이어 | 파일 |
|--------|------|
| Template | `templates/admin/crawling.html` |
| CSS | `static/css/admin.css` |
| JS | `static/js/admin.js` (크롤링 관련 바인딩: `bindCrawlPresetModal`, `bindCrawlScheduleToggleUi`, `bindCrawlPresetApply`, `bindCrawlPresetLoad`, `bindCrawlPreviewModal` 등) |
| Controller | `admin/controller/AdminPageController.java` (`/admin/crawling/**`, `/admin/weekly-insight/**`) |
| Service | `admin/service/TechNewsCrawlingService`, `CrawlConditionPresetService`, `CrawlScheduleService`, `CrawlHistoryService`, `WeeklyAiInsightService` |
| DTO | `admin/dto/CrawlRunForm`, `CrawlScheduleForm`, `CrawlPresetForm`, `CrawlPreviewResponse`, `CrawlPreviewItem`, `CrawlExecutionResult` |
| Entity | `admin/entity/CrawlConditionPreset`, `CrawlSchedule`, `CrawlHistory`, `entity/WeeklyAiInsight` |

---

## 2. 화면 구성

탭 5개로 구성 (`.admin-tab-nav` + `data-tab-panel`, 클릭 시 `admin.js`가 `hidden` 속성 토글). 최초 진입 시 활성 탭은 **조건 프리셋**.

| 탭 | `data-tab-panel` | 요약 |
|----|-------------------|------|
| 조건 프리셋 | `preset` | 재사용 가능한 크롤링 조건 CRUD (수정만 모달, 삭제 UI 없음) |
| 수동 실행 | `manual` | 즉시 크롤링 실행 (미리보기 → 저장 실행) |
| 예약 설정 | `schedule` | Cron 기반 자동 실행 on/off + 조건 편집 |
| 주간 AI 인사이트 | `weeklyInsight` | 최근 7일 뉴스 기반 AI 요약 생성/노출 토글 |
| 크롤링 이력 | `history` | 수동/예약 실행 결과 테이블 (정렬 가능) |

### 2.1 조건 프리셋 (`preset`)

- 목록 테이블: ID / 프리셋 이름 / 키워드 조건(`keywordMatchType`) / 소스 / 작업(수정)
- `+` 버튼(`#openCrawlPresetCreateModal`) → 생성 모달(`#crawlPresetModalOverlay`) 오픈, 폼 초기화
- 각 행 "수정" 버튼(`data-crawl-preset-edit`) → 같은 모달을 편집 모드로 오픈, `data-*` 속성으로 기존 값 채움
- 저장: `POST /admin/crawling/presets` (`saveCrawlPreset`, 생성/수정 겸용 — `presetId` hidden 필드로 구분)
- **삭제 기능 없음** — UI/컨트롤러 모두에 delete 엔드포인트 미존재

### 2.2 수동 실행 (`manual`)

- 화면 최초 진입 시 첫 번째 프리셋이 자동으로 선택되어 폼에 즉시 적용됨 (`bindInitialManualCrawlPreset`, `admin.js:1335`). 이후 다른 프리셋으로 바꿀 때만 "적용" 버튼(`data-crawl-preset-apply`) 클릭이 필요함
- 입력 필드: 대상 날짜, 소스 이름/URL, 최대 수집 건수, 포함 키워드(최대 5, AND/OR 연산자 개별 지정), 제외 키워드(최대 5), 대상 도메인, 연결/응답 타임아웃, 재시도 횟수
- "저장 실행" 클릭 → `admin.js`의 `bindCrawlPreviewModal`이 폼 제출을 가로채 먼저 미리보기 요청
  - `POST /admin/crawling/preview` (JSON, `@ResponseBody`) → `CrawlPreviewResponse` 반환 → `#crawlPreviewModalOverlay`에 제목/URL 목록 표시
  - 미리보기 모달에서 "저장 실행" 확인 시 실제 폼이 `POST /admin/crawling/run`으로 제출됨 (`runManualCrawling`)
  - 실행 중에는 `#crawlRunProgressModalOverlay`가 경과 시간을 표시 (진행률 아님, 단순 타이머)
- 완료 후 `redirect:/admin/crawling` + flash 메시지(`adminMessage`/`adminError`)

### 2.3 예약 설정 (`schedule`)

- 서비스가 고정 ID `1` 레코드만 조회/생성/갱신함 (`crawlScheduleService.getOrCreateSchedule()`, `CrawlScheduleService.java:21,44`). DB 제약으로 강제되는 건 아니고 애플리케이션 규약임
- "예약 실행" 토글(`enabled`) 켜짐 여부에 따라 카드에 `is-active`/`is-inactive` 클래스, 상태 뱃지(⚡ACTIVE/⏸INACTIVE)
- "중복 저장 허용" 토글(`allowDuplicate`) — 동일 URL 뉴스도 재저장 허용
- 비활성 상태에서도 크롤링 조건 입력 필드는 실제로는 `disabled` 처리되지 않고 `aria-disabled`+`is-soft-disabled` 클래스만 적용됨(`#crawlScheduleSoftDisabledGuide` 안내문구로만 안내, `bindCrawlScheduleToggleUi`, `admin.js:718`). 단, **"중복 저장 허용" 토글만은 비활성 시 실제로 `disabled=true`** 처리됨 (`admin.js:729`)
- Cron 표현식, 소스, 키워드/도메인 조건, 타임아웃/재시도 — 수동 실행과 동일한 필드 셋
- 저장: `POST /admin/crawling/schedule` (`updateCrawlSchedule`)
- 실제 자동 실행은 `ScheduledCrawlingExecutor`가 담당 (이 화면은 설정만)

### 2.4 주간 AI 인사이트 (`weeklyInsight`)

- 상세 데이터 모델/생성 로직은 `docs/02-design/features/weekly-ai-insight.md` 참고. 이 섹션은 **화면 요소만** 기술
- 상단: 기준일(`referenceDate`, 기본 오늘, 미래 날짜 선택 불가 `th:max`) + "주간 분석 생성"/"재생성" 버튼 → `POST /admin/weekly-insight/generate`
- 최신 1건 카드(`weeklyAiInsight`): 분석 기간, 분석 뉴스 건수, 노출 상태 뱃지 + 요약/트렌드/개발자 관점 3분할 미리보기
- 노출 토글 버튼 → `POST /admin/weekly-insight/{id}/toggle-visible`
- 데이터 없으면 "아직 생성된 주간 AI 인사이트가 없습니다" 안내
- 하단 이력 테이블(`weeklyAiInsightList`): 분석 기간 / 뉴스 수 / 노출 상태 (읽기 전용, 재노출·삭제 액션 없음)

### 2.5 크롤링 이력 (`history`)

- 컬럼: 일시, 트리거(`MANUAL`/`SCHEDULED` 뱃지, `TechNewsCrawlingService.java:81,174`), 대상일, 상태(SUCCESS/RUNNING/SKIPPED/FAILED 색상 구분), 소스, 요청/수집/신규저장 건수, 오류
- 컬럼 헤더 클릭 시 클라이언트 사이드 정렬 (`data-history-sort-key`, `admin.js`가 `data-history-*` 속성 기반으로 정렬 — 서버 재조회 없음)
- 오류 메시지가 길면 셀에는 트리거 텍스트만, 클릭 시 `#historyErrorModalOverlay`로 전체 오류 표시
- 데이터 없으면 "크롤링 이력이 없습니다"

---

## 3. 공용 컴포넌트

| 컴포넌트 | ID | 용도 |
|----------|----|----|
| 프리셋 생성/수정 모달 | `#crawlPresetModalOverlay` | 2.1 참고 |
| 크롤링 미리보기 모달 | `#crawlPreviewModalOverlay` | 2.2 참고 |
| 실행 진행 모달 | `#crawlRunProgressModalOverlay` | 2.2 참고 |
| 이력 오류 상세 모달 | `#historyErrorModalOverlay` | 2.5 참고 |
| 동적 입력 리스트 (`[data-dynamic-list]`) | - | 포함/제외 키워드·대상 도메인 입력에 공통 사용, `+`/`-`로 행 추가/삭제, `data-max-items`로 최대 개수 제한 |
| 프리셋 선택/적용 (`[data-crawl-preset-select]` / `[data-crawl-preset-apply]`) | - | 수동 실행/예약 설정 두 곳에서 동일 패턴 재사용 |

---

## 4. 액션 → API 매핑 요약

| 액션 | Method | Endpoint | 컨트롤러 메서드 |
|------|--------|----------|-----------------|
| 화면 진입 | GET | `/admin/crawling` | `crawlingPage` |
| 수동 실행 미리보기 | POST (JSON) | `/admin/crawling/preview` | `previewManualCrawling` |
| 수동 실행 저장 | POST | `/admin/crawling/run` | `runManualCrawling` |
| 예약 설정 저장 | POST | `/admin/crawling/schedule` | `updateCrawlSchedule` |
| 프리셋 생성/수정 | POST | `/admin/crawling/presets` | `saveCrawlPreset` |
| 주간 인사이트 생성/재생성 | POST | `/admin/weekly-insight/generate` | `generateWeeklyAiInsight` |
| 주간 인사이트 노출 토글 | POST | `/admin/weekly-insight/{id}/toggle-visible` | `toggleWeeklyAiInsightVisible` |

모든 POST(미리보기 제외)는 `redirect:/admin/crawling` + `RedirectAttributes` flash(`adminMessage`/`adminError`)로 결과를 알린다. 미리보기만 `@ResponseBody` JSON.

---

## 5. 확인된 제약/특이사항 (코드 기준, 미검증 스펙 아님)

- 프리셋은 생성/수정만 가능하고 **삭제 UI/API 없음**
- 예약은 **애플리케이션이 고정 ID 1 레코드 하나만 관리**하는 방식 (DB 제약은 아님) — 여러 개의 예약 스케줄을 동시에 운영하는 기능 없음
- 예약 비활성 시 조건 입력 필드는 시각적으로만 비활성화(`aria-disabled`)되고 실제 `disabled` 속성은 걸리지 않음 — "중복 저장 허용" 토글만 예외적으로 실제 비활성화됨
- 수동 실행 화면은 최초 진입 시 첫 프리셋을 자동 적용함 (예약 설정 탭은 자동 적용 없이 "적용" 버튼 클릭 필요 — 두 탭의 프리셋 적용 방식이 다름)
- 크롤링 이력은 최근 **20건**만 조회됨(`CrawlHistoryService.java:19`), 클라이언트 사이드 정렬도 이 20건 범위 내에서만 동작함 (전체 이력 대상 아님)
- 주간 AI 인사이트 생성 실패(소스 뉴스 0건, LLM 오류 등)의 사용자 피드백은 `adminError` 배너 하나뿐 — 원인별 세분화된 안내 없음

---

## 검증 범위 선언 (Audit)

- 본 문서는 **템플릿(html) + 컨트롤러(AdminPageController) + admin.js의 크롤링 관련 바인딩 함수 목록**만 읽고 작성함
- **검사하지 않은 범위**: `TechNewsCrawlingService`/`CrawlConditionPresetService`/`CrawlScheduleService`/`CrawlHistoryService` 내부 구현 로직, `admin.js` 각 함수의 상세 구현(함수 존재 여부와 바인딩 대상만 확인), CSS 실제 렌더링 결과, 브라우저 실동작 테스트
- 위 미검증 범위에서 이후 문제가 발견되면 새 이슈가 아니라 **스코프 누락**으로 취급할 것

### Codex 교차검증 결과 (2026-08-05)

`codex exec -s read-only`로 서비스/JS 상세 구현을 대조 검증함. Major 2건, Minor 2건 반영 완료(프리셋 자동 적용, 예약 비활성 시 필드 실제 비활성화 여부, 트리거 값 `SCHEDULED`, 예약 단일 레코드가 DB 제약이 아닌 애플리케이션 규약이라는 점). Codex가 지적한 상태 뱃지 이모지(⚡/⏸) 누락 주장은 `crawling.html:229`, `admin.js:725` 직접 확인 결과 **오탐으로 판정, 미반영** — Codex 실행 환경의 출력 인코딩 문제로 추정됨. Claude+Codex 합의만으로 검증 종료로 보지 않고 사용자에게 결과를 보고함.
