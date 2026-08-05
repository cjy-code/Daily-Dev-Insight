# 주간 AI 인사이트 (Weekly AI Insight) Design Document

> **Summary**: 최근 7일 테크 뉴스를 LLM으로 요약해 "이번 주 개발 Trend"로 노출하는 기능
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-03
> **Status**: **Implemented (사후 문서화)** — 코드가 이미 구현되어 있으며, 본 문서는 실제 구현을 기준으로 작성됨 (SoR: 코드 우선)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 개별 뉴스 나열만으로는 주간 트렌드를 파악하기 어려움 → AI가 7일치 뉴스를 종합 요약 |
| **WHO** | 인증된 사용자(홈 화면 노출), 관리자(생성/노출 관리) |
| **RISK** | 소스 뉴스가 0건이면 생성 실패, LLM 응답 실패 시 예외 노출 |
| **SUCCESS** | 관리자가 기준일 지정 후 생성하면 요약/트렌드/개발자 관점 3분류가 생성되고, 노출 토글로 사용자 화면 반영 여부를 제어할 수 있다 |
| **SCOPE** | 생성/조회/노출토글만 포함. 자동 스케줄, 사용자용 과거 이력 조회는 범위 밖 |

---

## 1. Overview

### 1.1 목적

관리자가 수동으로 트리거하면, 최근 7일(기준일 포함) 테크 뉴스를 모아 LLM에게 "요약(summary) / 트렌드·패턴(trendAnalysis) / 개발자 관점(developerView)" 3가지 관점으로 분석시키고, 결과를 저장해 관리자 미리보기 + 사용자 홈 화면에 노출한다.

### 1.2 배경

`docs/MVP_SCOPE.md` 2.1/2.6 절에 명시된 기능으로, 기존 `DailyKnowledgeGenerationService`(일간 지식 생성)와 별도의 파이프라인이다. 자동 스케줄 없이 관리자가 매번 수동으로 실행하는 구조.

### 1.3 관련 파일

| 레이어 | 파일 |
|--------|------|
| Entity | `entity/WeeklyAiInsight.java` |
| Repository | `repository/WeeklyAiInsightRepository.java` |
| Service | `admin/service/WeeklyAiInsightService.java` |
| DTO | `dto/WeeklyAiInsightViewDTO.java`, `admin/dto/GeneratedWeeklyInsightResult.java` |
| LLM 클라이언트 | `admin/service/LlmGenerationClient.java` (interface), `OpenAiLlmGenerationClient`, `MockLlmGenerationClient` |
| Controller | `admin/controller/AdminPageController.java` (`/admin/weekly-insight/**`), `controller/InsightPageController.java` (홈 화면 노출) |
| Schema | `config/OracleSchemaMigrationRunner.java` (`ensureWeeklyAiInsightTable`, `ensureWeeklyAiInsightSequence`) — **대응 SQL 문서 없음, 코드가 유일한 SoR** |
| Template | `templates/admin/crawling.html` (관리자), `templates/index.html` (사용자) |
| Test | `test/.../WeeklyAiInsightServiceTest.java` |

---

## 2. Data Model

### 2.1 Entity: `WeeklyAiInsight`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | Long | PK, Sequence(`seq_weekly_ai_insight`) | |
| `weekStartDate` | LocalDate | NOT NULL | 분석 시작일 (기준일 - 6일) |
| `weekEndDate` | LocalDate | NOT NULL | 분석 종료일 (기준일) |
| `summary` | String (CLOB) | NOT NULL | AI 요약 |
| `trendAnalysis` | String (CLOB) | NOT NULL | 트렌드/패턴 분석 |
| `developerView` | String (CLOB) | NOT NULL | 개발자 관점 |
| `sourceNewsCount` | Integer | NOT NULL | 분석에 사용된 뉴스 건수 |
| `visible` | Boolean (`is_visible`) | NOT NULL, 기본 `true` | 사용자 화면 노출 여부 |
| `createdAt` / `updatedAt` | LocalDateTime | NOT NULL | |

**유니크 인덱스**(named constraint 아님): `(week_start_date, week_end_date)` — 같은 기간 재생성 시 인덱스가 강제하는 게 아니라, **서비스 코드가 먼저 조회한 뒤 갱신**하는 애플리케이션 레벨 로직 (`updateAnalysis`). 따라서 동시에 같은 기간 생성 요청이 들어오면 선조회-저장 사이 race condition으로 unique index 충돌 가능성이 있음.

`visible=true` 기본값은 Java 필드 기본값이 아니라 신규 생성 경로(`createWeeklyAiInsight`)와 DB 컬럼 default에서 보장됨.

### 2.2 스키마 SoR 주의

테이블/시퀀스는 `docs/sql/`에 대응 파일이 없고 `OracleSchemaMigrationRunner`의 기동 시 자동 마이그레이션으로만 생성됨. 다른 테이블들은 `docs/sql/*_oracle.sql`로 남아있는 것과 불일치 — 후속 작업으로 `docs/sql/2026-05-08_weekly_ai_insight_oracle.sql` 보강 필요. 또한 이 유니크 인덱스는 **테이블을 새로 생성할 때만** 함께 생성되므로, 테이블은 이미 있는데 인덱스가 빠진 환경에서는 보정되지 않음.

---

## 3. 동작 명세 (Endpoints — 서버사이드 폼 기반, JSON API 아님)

| Method | Path | 설명 | 화면 |
|--------|------|------|------|
| `POST` | `/admin/weekly-insight/generate` | `referenceDate`(옵션, 미지정 시 오늘) 기준 최근 7일 뉴스로 생성/재생성. 결과는 `redirect:/admin/crawling` + flash message | `admin/crawling.html` |
| `POST` | `/admin/weekly-insight/{id}/toggle-visible` | 지정 인사이트의 노출 여부 반전. `redirect:/admin/crawling` + flash message | `admin/crawling.html` |
| — (내부 조회) | `findLatestVisibleInsight()` | 홈 화면(`/`, `/index` 둘 다)에 노출할 최신 **공개(visible=true)** 인사이트, 정렬 기준은 `weekEndDate DESC, id DESC` (생성/수정 시각 아님) | `index.html` |
| — (내부 조회) | `findLatestInsightForAdmin()` / `findRecentInsightsForAdmin()` | 관리자 미리보기(최신 1건, visible 무관) / 이력 목록(최근 5건, `findTop5By...`) | `admin/crawling.html` |

> **Fallback 주의**: `findLatestVisibleInsight()`는 "가장 최근 기간이면서 visible=true인" 행을 찾는다. 최신 기간의 인사이트를 숨겨도 **더 이전 기간의 visible 행이 있으면 그게 대신 노출**된다. 홈 섹션이 완전히 사라지는 조건은 "visible=true인 행이 하나도 없을 때"뿐이다.

### 3.1 생성 로직 (`generateWeeklyInsight`)

1. `weekEndDate` = `referenceDate` (없으면 오늘), `weekStartDate` = `weekEndDate - 6일`
2. `TechNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(start, end)`로 소스 뉴스 조회
3. 소스 뉴스가 0건이면 `IllegalStateException("최근 7일 기준으로 분석할 테크 뉴스가 없습니다.")` — **컨트롤러에서 catch 후 `adminError` flash로만 노출, 500 응답 아님**
4. 뉴스를 최대 40건(`MAX_PROMPT_NEWS_COUNT`)까지, 각 뉴스 요약은 350자(`MAX_SUMMARY_LENGTH`)로 잘라 프롬프트 구성 — **단, `sourceNewsCount`는 프롬프트에 넣은 40건이 아니라 조회된 전체 건수(`sourceNewsList.size()`)를 저장**한다. 즉 40건을 초과하면 "분석 뉴스 N건" 표시 수치와 실제로 LLM이 본 뉴스 수가 달라질 수 있음
5. `LlmGenerationClient.generateWeeklyInsight(prompt, start, end)` 호출 → `summary`/`trendAnalysis`/`developerView` 반환. **이 외부 LLM 호출이 `@Transactional` 메서드 안에서 이루어져, 응답이 느리면 DB 트랜잭션·커넥션이 그만큼 오래 점유된다**
6. 같은 기간(`weekStartDate`+`weekEndDate`) 기존 row가 있으면 **갱신**, 없으면 **신규 생성**. **갱신 시 기존 `visible` 값이 그대로 유지된다** — 숨김 처리된 인사이트를 재생성해도 자동으로 다시 노출되지 않음 (신규 생성일 때만 `visible=true` 기본값)
7. 저장 후 `WeeklyAiInsightViewDTO`로 변환해 반환. `toggleVisible()`은 `save()`를 명시 호출하지 않고 JPA dirty checking(트랜잭션 커밋 시 자동 반영)으로 저장됨

### 3.2 LLM 클라이언트 구현체 차이

| 구현체 | 동작 |
|--------|------|
| `MockLlmGenerationClient` | 항상 고정된 예시 문구 3종 반환 (프롬프트 내용 무시) — 로컬 개발/테스트용 |
| `OpenAiLlmGenerationClient` | 실제 OpenAI Chat Completions 호출. 코드블록 없는 raw JSON 응답을 프롬프트 지시만으로 요구(API의 JSON 응답 모드 강제 안 함), 세 필드 모두 비어있지 않아야 파싱 성공. API 키 누락/네트워크 오류 시 `LlmClientException` 발생 (`network_error` 등). 타임아웃/재시도 로직 없음 |

어느 구현체가 활성화되는지는 `llm.provider=mock|openai` 설정에 의존하며, **`application.yml` 기준 기본값은 `openai`**다 (mock이 기본이 아님) — 즉 API Key 설정 없이 첫 생성을 시도하면 실패한다.

---

## 4. UI/UX

### 4.1 관리자 화면 (`admin/crawling.html`)

- 기준일(`referenceDate`) 입력 폼 → 생성/재생성 버튼 (기존 인사이트 존재 여부에 따라 버튼 텍스트 변경). **`max=오늘` 제한은 HTML `<input type="date">` 속성일 뿐 서버 검증이 아님** — 폼을 우회해 직접 POST하면 미래/임의 과거 날짜가 그대로 허용됨. 날짜 형식 자체가 깨지면 컨트롤러 진입 전 400이 발생해 `adminError` flash 처리가 적용되지 않음
- 최신 인사이트 카드: 기간, 분석 뉴스 건수, 노출 상태 배지(`노출 중`/`숨김`), AI 요약/트렌드/개발자 관점 3섹션
- 노출 토글 버튼
- 최근 5건 이력 테이블
- 인사이트가 아예 없으면 `아직 생성된 주간 AI 인사이트가 없습니다.` 안내

### 4.2 사용자 화면 (`index.html`)

- `weeklyAiInsight != null`(=공개 상태의 최신 인사이트 존재)일 때만 "이번 주 개발 Trend" 섹션 노출
- 기간 + 분석 뉴스 건수, AI 요약/트렌드/개발자 관점 3블록 표시
- `visible=false`이거나 데이터가 없으면 섹션 자체가 렌더링되지 않음 (별도 빈 상태 문구 없음)

---

## 5. Error Handling

| 상황 | 처리 |
|------|------|
| 최근 7일 뉴스 0건 | `IllegalStateException` → 관리자 화면에 `adminError` flash 메시지, 500 응답 아님 |
| 존재하지 않는 `id`로 노출 토글 | `IllegalArgumentException("주간 AI 인사이트를 찾을 수 없습니다.")` → 동일하게 flash 메시지 처리 |
| LLM 호출 실패 (OpenAI) | `LlmClientException`은 사용자용 메시지(`getUserMessage()`)와 기술 메시지(`getMessage()`)를 분리해서 갖고 있지만, **`AdminPageController`는 `exception.getMessage()`(기술 메시지)를 그대로 flash에 노출** — `getUserMessage()`를 쓰지 않음. 컨트롤러는 예외 타입 구분 없이 공통 `catch (Exception)`으로 처리 |
| 사용자 화면 (정상 케이스) | `findLatestVisibleInsight()`가 조회 결과 없음일 때만 `null` 반환 → 섹션 미노출로 처리 |
| 사용자 화면 (DB/조회 오류) | **별도 catch가 없어 예외가 그대로 상위로 전파되고 홈 요청(`/`, `/index`) 전체가 실패한다** — "예외를 던지지 않는다"는 이전 설명은 부정확했음 |

---

## 6. Security Considerations

- 생성/토글 엔드포인트는 `/admin/**` 하위 — `SecurityConfig`의 관리자 인증 체인에 의해 보호됨 (관리자 권한 필요)
- 폼 제출에 CSRF 토큰(`_csrf`) 포함되어 있음 (템플릿 확인됨)
- 홈 화면(`/`, `/index`)도 완전한 공개 화면이 아니라 일반 사용자 인증이 필요함 (`SecurityConfig`)
- 프롬프트에 포함되는 외부 데이터는 뉴스 **요약뿐 아니라 날짜·출처·제목·URL까지 전부** 포함됨 — LLM에 그대로 전달되므로 프롬프트 인젝션 관점의 필터링이 이 전체 필드 범위에 걸쳐 없음 (현재 범위 밖으로 확인, 후속 검토 후보)
- LLM 생성 결과(`summary`/`trendAnalysis`/`developerView`)는 템플릿에서 `th:text`로 출력되어 HTML로 실행되지 않고 이스케이프됨 — XSS 관점은 안전

---

## 7. 테스트 현황

`WeeklyAiInsightServiceTest`에 3개 유닛 테스트 존재 (Mockito 기반):
- 신규 생성 (뉴스 2건 → 인사이트 생성, 프롬프트에 뉴스 제목 포함 검증)
- 기존 기간 갱신 (같은 기간 재생성 시 기존 row 갱신)
- 노출 토글 반전

**커버 안 된 케이스**: 소스 뉴스 0건 예외, `toggleVisible` 존재하지 않는 id 예외, LLM 클라이언트 예외 전파 — 필요 시 후속 테스트 추가 후보.

---

## 8. Known Gaps / 후속 작업 후보

- `docs/sql/`에 `weekly_ai_insight` 테이블 정의 SQL 없음 (마이그레이션 코드만 SoR), 해당 유니크 인덱스도 테이블 신규 생성 시에만 적용되어 기존 테이블 보정 불가
- 자동 생성 스케줄 없음 (수동 트리거만 가능) — `docs/MVP_SCOPE.md` Not Yet Implemented에 이미 기재됨
- 사용자용 과거 이력 조회/목록 API 없음 — 최신 1건만 노출
- `LlmClientException`에 사용자 메시지가 별도로 정의돼 있음에도 컨트롤러가 이를 쓰지 않고 기술 메시지(`getMessage()`)를 그대로 노출함
- 프롬프트 인젝션 방지 로직 없음 — 대상은 뉴스 요약뿐 아니라 날짜·출처·제목·URL 전체
- `sourceNewsCount`(전체 조회 건수)와 실제 LLM에 전달되는 뉴스 건수(최대 40건) 간 의미 불일치
- 최신 기간을 숨겨도 이전 visible 기간이 대신 노출되는 fallback 동작이 문서화되어 있지 않았음
- 기준일(`referenceDate`)에 대한 서버 측 범위 검증 부재 (HTML `max` 속성 우회 가능)
- 같은 기간 동시 생성 요청 시 선조회-저장 구조로 인한 race condition 가능성 (원자적 upsert/lock 없음)
- OpenAI 호출에 timeout/retry가 없고, JSON 응답 모드를 API로 강제하지 않아 프롬프트 지시에만 의존한 raw JSON 파싱
- 외부 LLM 호출이 DB 트랜잭션 내부에서 수행되어 응답 지연 시 트랜잭션/커넥션이 오래 점유될 수 있음
- `toggleVisible()`의 dirty-checking 저장 방식을 검증하는 통합 테스트 부재 (현재는 유닛 테스트 3건뿐: 신규 생성, 기간 갱신, 노출 토글 — 0건 뉴스/존재하지 않는 id/LLM 예외 케이스 미커버)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-03 | 사후 설계 문서 초안 작성 (기존 구현 기준) | Claude (대화 기반) |
| 0.2 | 2026-08-03 | Codex 검증 반영 — visible fallback, sourceNewsCount 의미 불일치, LLM 에러 메시지 미사용, 홈 화면 예외 전파, 서버 측 날짜 검증 부재, race condition, 트랜잭션 내 외부 호출 등 추가 | Claude (Codex `codex exec -s read-only` 검증 결과 반영) |
