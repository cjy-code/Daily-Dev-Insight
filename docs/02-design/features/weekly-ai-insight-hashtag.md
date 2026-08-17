# 주간 AI 인사이트 해시태그 (Weekly AI Insight Hashtag) Design Document

> **Summary**: `WeeklyAiInsight` 생성 시 LLM이 summary/trendAnalysis/developerView와 함께 3~5개의 해시태그를 생성하도록 확장하고, 홈 화면 카드에 배지로 노출
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-10
> **Status**: Draft (Plan 승인 대기 — 사용자 미검토, 2026-08-10 확인. 승인 전까지 구현 착수 안 함)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | `docs/01-plan/features/weekly-ai-insight-hashtag.md` 참조 — 서술형 3문단만으로는 "이번 주 핵심 주제"를 한눈에 스캔하기 어려움 |
| **WHO** | 홈 화면 방문자(배지 열람), 관리자(생성 트리거) |
| **RISK** | LLM 응답 스키마 확장이므로 파서(`OpenAiLlmGenerationClient`)·Mock 클라이언트 동시 갱신 필요. DB 컬럼 추가 시점이 다른 세션(P4-3 비밀번호 인코더 마이그레이션)과 같은 파일(`OracleSchemaMigrationRunner.java`)을 건드림 — **§9 Implementation Sequencing 필수 확인** |
| **SUCCESS** | 생성/재생성 시 해시태그 3~5개가 저장되고, 홈 화면 카드에 배지로 노출되며, 기존 3문단(summary/trendAnalysis/developerView) 동작에는 회귀가 없음 |
| **SCOPE** | 생성·저장·단순 배지 표시만. 클릭 인터랙션, 관리자 편집 UI, 프롬프트 참조 연동은 범위 밖 (Plan 문서 §2.2와 동일) |

---

## 1. Overview

### 1.1 목적

기존 `WeeklyAiInsight` 생성 파이프라인에 해시태그 필드를 추가해, 서술형 요약 앞단에서 핵심 키워드를 빠르게 스캔할 수 있게 한다.

### 1.2 배경

`docs/01-plan/features/weekly-ai-insight-hashtag.md` §1.2 참조. 기존 `docs/02-design/features/weekly-ai-insight.md`(v0.2)에 정의된 파이프라인을 확장하며, 별도 문서로 분리해 기반 문서의 Known Gaps(§8, 본 기능과 무관한 기존 이슈)와 신규 변경 사항을 섞지 않는다.

### 1.3 관련 파일

| 레이어 | 파일 | 변경 유형 |
|--------|------|-----------|
| Entity | `entity/WeeklyAiInsight.java` | 필드 추가 (`hashtags`) |
| DTO | `dto/WeeklyAiInsightViewDTO.java`, `admin/dto/GeneratedWeeklyInsightResult.java` | 필드 추가 |
| Service | `admin/service/WeeklyAiInsightService.java` | 저장/조회 로직에 해시태그 반영 |
| LLM 클라이언트 | `admin/service/OpenAiLlmGenerationClient.java`, `admin/service/MockLlmGenerationClient.java` | 프롬프트·응답 스키마·파서 확장 |
| Schema | `config/OracleSchemaMigrationRunner.java` | `weekly_ai_insight` 테이블에 컬럼 추가 — **§9 순서 준수 필요** |
| Template | `templates/index.html`, `templates/admin/crawling.html` | 배지 렌더링 추가 |
| Test | `test/.../WeeklyAiInsightServiceTest.java` | 시나리오 추가 |

---

## 2. Data Model

### 2.1 Entity 변경: `WeeklyAiInsight`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `hashtags` | String (VARCHAR2 500) | **NULL 허용** | 쉼표(`,`)로 구분된 3~5개 키워드. 기존 row(해시태그 도입 전 생성)는 `NULL` — 하위 호환을 위해 `NOT NULL` 제약을 걸지 않는다 |

`summary`/`trendAnalysis`/`developerView`와 달리 `NOT NULL`을 걸지 않는 이유: 기존 row에 대한 백필(소급 생성) 없이 신규/재생성 시점부터만 채워지므로(Plan §2.2 Out of Scope), NOT NULL이면 컬럼 추가 시점에 기존 row 마이그레이션이 강제된다.

저장 형식은 CLOB이 아니라 VARCHAR2로 충분(5개 × 최대 약 30자 + 구분자 여유 → 500자로 충분). 개별 태그에 쉼표가 포함되면 구분자와 충돌하므로, 저장 전 각 태그에서 쉼표를 제거해 정규화한다(§3.1).

### 2.2 DTO 변경: `GeneratedWeeklyInsightResult`, `WeeklyAiInsightViewDTO`

- `GeneratedWeeklyInsightResult`에 `List<String> hashtags` 필드 추가 (LLM 파싱 직후, 저장 전 단계)
- `WeeklyAiInsightViewDTO`에 `List<String> hashtags` 필드 추가 — `WeeklyAiInsightViewDTO.from()`에서 엔티티의 CSV 문자열을 분해해 리스트로 변환. `hashtags`가 `null`/공백이면 **빈 리스트**(`List.of()`)로 반환해 템플릿에서 null 체크 없이 안전하게 순회 가능하게 한다

---

## 3. 동작 명세 변경

### 3.1 생성 로직 (`WeeklyAiInsightService.generateWeeklyInsight`) 변경 지점

기존 흐름(Design 문서 v0.2 §3.1)에서 다음만 추가/변경한다:

1. `LlmGenerationClient.generateWeeklyInsight()` 반환값에서 `hashtags` 리스트를 함께 받는다
2. 저장 전, 각 태그를 `trim()` 하고 내부 쉼표를 제거해 정규화한 뒤 `String.join(",", ...)`로 CSV 문자열을 만들어 엔티티에 저장한다
3. `createWeeklyAiInsight()`(신규 생성)와 `updateAnalysis()`(기존 row 갱신) 양쪽 모두 해시태그를 반영한다 — `summary`/`trendAnalysis`/`developerView`와 동일한 갱신 대상으로 취급

### 3.2 LLM 응답 스키마 변경 (`OpenAiLlmGenerationClient`)

- 프롬프트 지시문(현재 "필수 키는 summary, trendAnalysis, developerView 입니다" 부분, `:333`)에 `hashtags`(문자열 배열, 3~5개) 요구사항 추가
- 예시 JSON(`:347`)에 `"hashtags":["...","...","..."]` 추가
- `parseWeeklyInsightResult()`(`:292`)에서 `hashtags` 배열을 읽어 `List<String>`으로 변환
- **검증 규칙(FR-02)**: 배열 크기가 3 미만이면 기존 `readRequiredValue()`가 빈 문자열에 대해 던지는 것과 동일한 방식으로 파싱 실패 처리(예외 발생 → 상위에서 `LlmClientException`으로 표준화). 5개를 초과하면 실패시키지 않고 **앞 5개만 사용**(관대한 처리 — LLM이 규칙을 약간 넘겨도 기능 전체가 실패하지 않도록)
- 개별 태그 문자열이 공백이면 제외하고 카운트, 제외 후 3개 미만이면 위와 동일하게 실패 처리

### 3.3 `MockLlmGenerationClient` 변경

- 기존처럼 프롬프트 내용과 무관하게 고정 해시태그 예시(예: `["백엔드", "AI", "클라우드"]` 등 3~4개)를 반환하도록 확장 — 로컬 개발/테스트 시 배지 UI를 바로 확인 가능하게 함

---

## 4. UI/UX 변경

### 4.1 사용자 화면 (`index.html`)

`weekly-ai-insight-card` 내부, 기존 3블록(summary/trend/dev-view) **앞**에 해시태그 배지 목록을 추가한다:

```html
<div class="weekly-ai-insight-tags" th:if="${weeklyAiInsight.hashtags != null and !weeklyAiInsight.hashtags.isEmpty()}">
    <span class="weekly-ai-insight-tag" th:each="tag : ${weeklyAiInsight.hashtags}" th:text="'#' + ${tag}">#태그</span>
</div>
```

- 클릭 불가(순수 텍스트 배지) — Plan §2.2 Out of Scope
- `hashtags`가 빈 리스트면(과거 row 또는 파싱 결과 없음) 배지 영역 자체가 렌더링되지 않음 — 기존 카드 레이아웃에 빈 여백이 남지 않도록 `th:if`로 완전히 제거

### 4.2 관리자 화면 (`admin/crawling.html`)

- 최신 인사이트 미리보기 카드에 동일한 배지 목록을 3블록 위에 추가 (사용자 화면과 동일한 표시 방식, 관리자가 생성 결과를 바로 확인할 수 있도록)
- 최근 5건 이력 테이블에는 추가하지 않음(테이블 컬럼 확장은 범위 밖 — 필요 시 후속 검토)

---

## 5. Error Handling

기존 Design 문서(v0.2) §5의 에러 처리 표에 다음 행을 추가하는 개념으로 확장한다:

| 상황 | 처리 |
|------|------|
| LLM 응답에 `hashtags`가 없거나 유효 태그가 3개 미만 | 기존 `readRequiredValue()` 실패와 동일하게 파싱 예외 → `LlmClientException`으로 표준화되어 생성 전체가 실패 처리됨 (부분 성공 없음 — summary/trend/dev-view만 저장하고 해시태그만 누락시키지 않는다) |
| `hashtags`가 5개 초과 | 실패시키지 않고 앞 5개만 사용 (§3.2) |
| 과거 row(해시태그 없음) 조회/렌더링 | `WeeklyAiInsightViewDTO.hashtags`가 빈 리스트로 기본값 처리되어 NPE 없이 배지 영역만 생략됨 |

---

## 6. Security Considerations

기존 Design 문서(v0.2) §6과 동일한 관점이 적용된다:

- 해시태그도 `summary` 등과 동일하게 LLM이 생성한 외부 유래 텍스트이므로, 프롬프트 인젝션 방지 로직 부재라는 기존 Known Gap의 적용 범위에 포함됨(신규 이슈 아님, 기존 갭의 연장)
- 화면 출력은 `th:text`로 이스케이프되므로 XSS 관점은 안전 (§4.1 예시와 동일)

---

## 7. 테스트 계획

`WeeklyAiInsightServiceTest`에 다음 시나리오 추가:

- 신규 생성 시 해시태그가 함께 저장되는지 (Mock LLM 클라이언트가 반환한 해시태그 리스트 → 저장된 엔티티의 CSV 문자열 → DTO 변환 시 다시 리스트로 복원되는 왕복 검증)
- 기존 기간 갱신 시 해시태그도 summary 등과 함께 갱신되는지
- 해시태그가 없는(과거) row를 DTO로 변환할 때 빈 리스트를 반환하는지(하위 호환)

파서 단(`OpenAiLlmGenerationClient`) 전용 테스트는 기존에도 없었으므로(서비스 레벨 Mockito 테스트만 존재) 본 기능에서도 동일 범위로 유지 — 필요 시 별도 후속 과제로 분리.

---

## 8. Known Gaps / 후속 작업 후보

- 과거 row에 대한 소급 해시태그 생성(백필)은 범위 밖 — 필요해지면 별도 배치/스크립트로 후속 진행
- 해시태그 클릭 시 검색 연동, 관리자 편집 UI는 Plan 문서에서 이미 범위 밖으로 확정됨
- 해시태그를 `DailyKnowledgeGenerationService`의 프롬프트 렌더링에 참조시키는 연동은 별도 Plan으로 분리 예정(Plan 문서 §6 Next Steps #3)
- 기존 Design 문서(v0.2) §8의 Known Gaps(race condition, 트랜잭션 내 외부 호출, 날짜 검증 부재 등)는 본 기능과 무관하게 그대로 유효 — 본 문서에서 다루지 않음

---

## 9. Implementation Sequencing (중요)

`OracleSchemaMigrationRunner.java`는 **현재 다른 세션이 P4-3(비밀번호 인코더 BCrypt 마이그레이션, `ensureSeedUserPasswordsHashed()`)를 uncommitted 상태로 작업 중**이다(2026-08-10 확인).

- 본 기능의 컬럼 추가 로직(§2.1 `hashtags`)은 **P4-3 변경이 커밋된 이후에** 같은 파일에 반영한다 — 같은 워킹트리에서 미커밋 변경과 동시에 같은 파일을 수정하면 충돌 위험이 있다
- 그 외 변경 파일(Entity/DTO/Service/LLM 클라이언트/템플릿)은 P4-3과 겹치지 않으므로 순서 제약이 없다
- 구현 착수 전 `git status`로 `OracleSchemaMigrationRunner.java`가 커밋되었는지 재확인할 것

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-10 | 최초 작성 — Plan 문서(`weekly-ai-insight-hashtag.md`) 기반 설계, 기존 `weekly-ai-insight.md`(v0.2) Design 문서 확장. §9 Implementation Sequencing에 P4-3(비밀번호 인코더 마이그레이션)과의 `OracleSchemaMigrationRunner.java` 파일 충돌 위험 및 순서 명시 | Claude (PM 세션 진행) |
| 0.2 | 2026-08-10 | Status 오기 정정 — Plan 문서와 Status 표기가 불일치(Design은 "Plan 승인 완료", Plan은 "승인 대기")함을 발견해 사용자에게 직접 확인한 결과, **Plan은 아직 사용자 검토 전**임을 확인. Design Status를 "Plan 승인 대기"로 정정. 소스 구현은 사용자 지시로 추후 진행 | Claude (검증) |
