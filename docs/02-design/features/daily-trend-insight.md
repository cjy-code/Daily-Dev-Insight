# 일일 개발 트렌드 (Daily Trend Insight) Design Document

> **Summary**: 매일 크롤링된 `TechNews`를 LLM으로 분석해 `DailyTrendInsight`(신규)를 생성·저장하고, 홈 화면에 별도 카드로 노출하며, 그 결과를 `DailyKnowledge` 생성 프롬프트에 서버가 항상 강제로 반영한다
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-13
> **Status**: 구현 완료(Codex, `danger-full-access`), Claude 독립 검증 완료(빌드/테스트 101건 통과, 코드 diff 전수 리뷰) — 사용자 최종 코드 리뷰 및 커밋 대기

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | `docs/01-plan/features/daily-trend-insight.md` 참조. Plan은 Codex 교차검증 2회 + 사용자 결정을 거쳐 Approved 상태 |
| **WHO** | 홈 화면 방문자(트렌드·학습 게시물 열람), 관리자(생성 트리거) |
| **RISK** | (1) `OracleSchemaMigrationRunner.java`에 `weekly-ai-insight-hashtag`(별도 Draft Plan, 아직 미승인)도 스키마 변경을 필요로 함 — **§9 Implementation Sequencing 필수 확인**. (2) `DailyKnowledgeGenerationService`의 두 LLM 호출 경로(예약/수동) 모두 수정 필요 — 누락 시 FR-07(강제 주입) 깨짐 |
| **SUCCESS** | Plan §6/FR-01~08 전항목이 코드로 매핑되고, §3.3 부분 성공 매트릭스가 실제 구현과 일치 |
| **SCOPE** | Plan §2.1/§2.2와 동일. `WeeklyAiInsight` 관련 파일은 일절 수정하지 않는다(읽기 참고만) |

---

## 1. Overview

### 1.1 목적

Plan의 결정 사항을 실제 엔티티/서비스/컨트롤러/템플릿 변경으로 매핑한다.

### 1.2 관련 파일

| 레이어 | 파일 | 변경 유형 |
|--------|------|-----------|
| Entity | `entity/DailyTrendInsight.java` (신규) | 신규 |
| Entity | `admin/entity/DailyTrendGenerationHistory.java` (신규) | 신규 |
| Entity | `admin/entity/GenerationHistory.java` | 필드 추가(`usedTrendId`) |
| Repository | `repository/DailyTrendInsightRepository.java` (신규) | 신규 |
| Repository | `admin/repository/DailyTrendGenerationHistoryRepository.java` (신규) | 신규 |
| DTO | `dto/DailyTrendInsightViewDTO.java` (신규) | 신규 |
| DTO | `admin/dto/GeneratedDailyTrendResult.java` (신규) | 신규 |
| DTO | `admin/dto/GenerationPreviewResponse.java`, `admin/dto/GenerationSaveRequest.java` | 필드 추가 |
| Service | `admin/service/DailyTrendInsightService.java` (신규) | 신규 — `WeeklyAiInsightService` 패턴 참고(코드 재사용 없이 독립 구현) |
| Service | `admin/service/DailyKnowledgeGenerationService.java` | 강제 트렌드 주입 로직 추가 |
| Service | `admin/service/ScheduledGenerationExecutor.java` | 트렌드 생성 단계 추가(예외 격리) |
| LLM 클라이언트 | `admin/service/LlmGenerationClient.java`, `OpenAiLlmGenerationClient.java`, `MockLlmGenerationClient.java` | 메서드 추가 |
| Controller | `admin/controller/AdminPageController.java` | 라우트 추가, `generationComposePage`/`previewManualGeneration` 수정 |
| Controller | `controller/InsightPageController.java` | 모델 속성 추가 |
| Schema | `config/OracleSchemaMigrationRunner.java` | 테이블 2개 + 시퀀스 2개 추가, `generation_history` 컬럼 추가 — **§9 순서 준수 필요** |
| Template | `templates/index.html` | 트렌드 카드 신설 |
| Template | `templates/admin/crawling.html` | `[오늘의 개발 트렌드]` 탭 신설 |
| Template | `templates/admin/generation-compose.html` | 트렌드 읽기 전용 구역 추가 |
| Test | `admin/service/DailyTrendInsightServiceTest.java`(신규), `DailyKnowledgeGenerationServiceTest.java`, `ScheduledGenerationExecutorTest.java` | 신규/수정 |

---

## 2. Data Model

### 2.1 신규 Entity: `DailyTrendInsight`

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | Long | PK, SEQUENCE(`seq_daily_trend_insight`) | |
| `trendDate` | LocalDate | NOT NULL, UNIQUE(`ux_daily_trend_insight_date`) | 분석 기준일. 재생성 시 같은 row를 upsert(버전 테이블 없음 — Plan §6 "불일치 허용" 결정에 따라 이력은 `DailyTrendGenerationHistory`가 담당) |
| `keywords` | String (VARCHAR2 500) | NOT NULL | 쉼표(`,`)로 구분된 3~5개 키워드. 개별 키워드는 저장 전 trim + 내부 쉼표 제거(해시태그 Design §2.1과 동일 정규화 원칙) |
| `summary` | String (VARCHAR2 1000) | NOT NULL | 1~2문장 요약 |
| `sourceNewsCount` | Integer | NOT NULL | 실제 분석에 사용한 뉴스 건수(폴백으로 3일치를 썼어도 "실제 사용한 건수") |
| `visible` | Boolean | NOT NULL, DEFAULT 1 | 홈 노출 여부(§6: 최신 공개 트렌드 1건 노출 정책을 지원) |
| `createdAt` / `updatedAt` | LocalDateTime | NOT NULL | |

```java
public void updateAnalysis(String keywords, String summary, Integer sourceNewsCount) {
    this.keywords = keywords;
    this.summary = summary;
    this.sourceNewsCount = sourceNewsCount;
    this.updatedAt = LocalDateTime.now();
}

public void changeVisible(Boolean visible) {
    this.visible = visible;
    this.updatedAt = LocalDateTime.now();
}
```

`WeeklyAiInsight`와 동일하게 `updateAnalysis`/`changeVisible` 두 메서드만 노출(도메인 로직을 엔티티에 위임하는 기존 컨벤션 유지).

### 2.2 신규 Entity: `DailyTrendGenerationHistory`

Plan §3.3에서 확정한 "트렌드 단계 성공/실패는 반드시 기록"을 만족시키기 위한 전용 이력 테이블. `GenerationHistory`(지식 생성 전용, `createdKnowledgeId`/`title` 등 지식 특화 필드를 가짐)를 재사용하지 않고 별도 테이블로 분리한다 — 두 콘텐츠 유형의 이력을 한 테이블에 섞으면 관리자 이력 조회 화면에서 필터링 로직이 복잡해지고, `GenerationHistory`의 지식 특화 컬럼(`createdKnowledgeId`, `title`)이 트렌드에는 의미가 없어 nullable 컬럼만 늘어나기 때문(설계 판단, Codex 재검증 없이 Claude가 결정 — 사용자 리뷰 시 이견 있으면 조정).

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | Long | PK, SEQUENCE(`seq_daily_trend_gen_history`) | |
| `triggerType` | String (VARCHAR2 20) | NOT NULL | `SCHEDULED` / `MANUAL` (기존 `GenerationHistory.triggerType`과 동일 값 규칙) |
| `targetDate` | LocalDate | NOT NULL | |
| `status` | String (VARCHAR2 20) | NOT NULL | `SUCCESS` / `FAILED` (트렌드는 "중복 스킵" 개념이 없음 — 재생성은 항상 upsert로 재시도 가능하므로 `SKIPPED` 상태 불필요) |
| `sourceNewsCount` | Integer | NULL 허용 | 실패 시 0 또는 null |
| `createdTrendId` | Long | NULL 허용 | 성공 시 `DailyTrendInsight.id` |
| `errorMessage` | String (VARCHAR2 1000) | NULL 허용 | 실패 사유(뉴스 없음/LLM 오류/검증 실패) |
| `createdAt` | LocalDateTime | NOT NULL | |

### 2.3 `GenerationHistory` 필드 추가

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `usedTrendId` | Long | NULL 허용 | 이 지식 생성 시점에 실제 반영된 `DailyTrendInsight.id`. 트렌드 미반영(FR-04 폴백)이면 `null`. 실제 트렌드 텍스트 자체는 `promptSnapshot`에 이미 포함되므로(FR-07: 항상 프롬프트에 강제 삽입) 별도 스냅샷 컬럼은 두지 않는다 — `usedTrendId`는 "지금의 `DailyTrendInsight`와 같은지 다른지"를 관리자 화면에서 빠르게 배지로 비교하기 위한 용도 |

### 2.4 DTO

- `DailyTrendInsightViewDTO`: `id, trendDate, keywords(List<String>), summary, sourceNewsCount, visible` — `WeeklyAiInsightViewDTO.from()`과 동일하게 `keywords` CSV를 리스트로 분해(빈 문자열/공백 제거 후 `List.of()` 기본값)
- `GeneratedDailyTrendResult` (LLM 파싱 직후, 저장 전): `keywords(List<String>), summary(String)`

---

## 3. LLM 연동

### 3.1 `LlmGenerationClient` 인터페이스 추가

```java
GeneratedDailyTrendResult generateDailyTrend(String prompt, LocalDate targetDate);
```

### 3.2 `OpenAiLlmGenerationClient` 구현

`generateWeeklyInsight()`와 동일한 구조(요청 엔티티 생성 → 호출 → 파싱)를 별도 메서드로 구현한다(코드 공유 없이 독립 — Plan에서 `WeeklyAiInsight` 관련 코드를 건드리지 않기로 한 결정과 일관되게, 신규 메서드도 기존 weekly 메서드를 리팩터링해 공유하지 않는다).

시스템 지시문:
```
당신은 개발자를 위한 일일 기술 뉴스 분석가입니다. 반드시 JSON 객체로만 응답하세요.
필수 키는 keywords(문자열 배열, 3~5개), summary(1~2문장 한국어 요약) 입니다.
keywords는 명사형 기술 키워드로, summary는 실무 개발자가 바로 이해할 수 있게 구체적으로 작성하세요.
```

사용자 프롬프트: `WeeklyAiInsightService.formatNewsForPrompt()`와 동일한 포맷("- 날짜/출처/제목/요약/URL")으로 뉴스 목록을 나열 + 응답 JSON 예시(`{"keywords":["...","...","..."],"summary":"..."}`)

`parseDailyTrendResult()` 검증 규칙(Plan §2.1 확정 사항 그대로):
1. `keywords` 배열의 각 원소를 trim, 공백 원소 제외, 중복 제거
2. 정규화 후 개수가 3 미만이면 `LlmClientException`(파싱 실패로 표준화, 기존 `readRequiredValue()` 실패와 동일한 처리 방식)
3. 5개 초과면 실패시키지 않고 앞 5개만 사용
4. `summary`가 공백이면 실패 처리(`readRequiredValue()` 재사용)

### 3.3 `MockLlmGenerationClient` 구현

프롬프트 내용과 무관하게 고정 예시(`["백엔드", "AI", "클라우드"]` + 고정 요약 문장)를 반환 — 로컬 개발 시 카드 UI를 바로 확인 가능하게 함(해시태그 Design §3.3과 동일 원칙).

---

## 4. `DailyTrendInsightService` (신규)

`WeeklyAiInsightService`와 동일한 골격, 상수만 다르게 구성:

```java
private static final int TREND_FALLBACK_DAYS = 3;
private static final int TREND_FALLBACK_MIN_NEWS_COUNT = 3;
private static final int MAX_PROMPT_NEWS_COUNT = 40;
private static final int MAX_SUMMARY_LENGTH = 350;
```

### 4.1 `generateDailyTrend(LocalDate referenceDate)`

1. `targetDate = referenceDate == null ? LocalDate.now() : referenceDate`
2. `sourceNewsList = techNewsRepository.findByNewsDateOrderByIdDesc(targetDate)` (당일 우선, Plan §6)
3. `sourceNewsList.size() < TREND_FALLBACK_MIN_NEWS_COUNT`이면 `techNewsRepository.findByNewsDateBetweenOrderByNewsDateDescIdDesc(targetDate.minusDays(TREND_FALLBACK_DAYS - 1), targetDate)`로 재조회(3일 폴백)
4. 그래도 비어 있으면 `DailyTrendGenerationHistory`에 `FAILED`(사유: "분석할 테크 뉴스가 없습니다") 기록 후 `IllegalStateException` throw(`WeeklyAiInsightService`와 동일한 실패 방식)
5. 프롬프트 구성(§3.2) → `llmGenerationClient.generateDailyTrend()` 호출
6. 예외 발생 시 `DailyTrendGenerationHistory`에 `FAILED` 기록 후 rethrow
7. 성공 시 `dailyTrendInsightRepository.findByTrendDate(targetDate).orElseGet(() -> create(...))` → `updateAnalysis()` → save
8. `DailyTrendGenerationHistory`에 `SUCCESS` 기록(`createdTrendId` 포함)
9. `DailyTrendInsightViewDTO` 반환

### 4.2 조회 메서드

- `findLatestVisibleTrend()`: `dailyTrendInsightRepository.findTopByVisibleTrueOrderByTrendDateDescIdDesc()` — 홈 화면용(Plan §6: 최신 공개 1건)
- `findLatestTrendForAdmin()`: `findTopByOrderByTrendDateDescIdDesc()` — 관리자 화면 최신 카드용(비공개 포함)
- `findRecentTrendsForAdmin()`: `findTop5ByOrderByTrendDateDescIdDesc()` — 관리자 이력 테이블(주간 탭과 동일하게 5건)
- `toggleVisible(Long trendId)`

---

## 5. `DailyKnowledgeGenerationService` 변경 (FR-07 강제 주입)

### 5.1 신규 private 메서드

```java
private String appendDailyTrendContext(String basePrompt, LocalDate targetDate) {
    DailyTrendInsight trend = dailyTrendInsightRepository.findByTrendDate(targetDate).orElse(null);
    if (trend == null) {
        return basePrompt; // FR-04 폴백 — 트렌드 없이 그대로
    }
    String trendBlock = "[오늘의 개발 트렌드 참고]\n"
            + "키워드: " + trend.getKeywords() + "\n"
            + "요약: " + trend.getSummary();
    if (basePrompt.contains("${dailyTrend}")) {
        return basePrompt.replace("${dailyTrend}", trendBlock); // 관리자가 템플릿에 변수를 명시했다면 그 위치에
    }
    return basePrompt + "\n\n" + trendBlock; // 변수가 없으면 말미에 자동 추가(FR-07)
}
```

이 메서드는 **LLM 호출 직전에만** 호출한다 — `renderPrompt()`(템플릿 치환)나 `buildRenderedPromptForManual()`(관리자 초기 화면 표시용)에는 넣지 않는다. 이유: `buildRenderedPromptForManual()`은 admin이 편집할 textarea의 초기값을 만들 뿐이고, 실제로 LLM에 보내지는 텍스트는 관리자가 편집한 이후의 값이기 때문에(§2절 관련 파일 표의 "현재 처리 흐름" 참고), 트렌드 주입은 편집 가능한 프롬프트 텍스트와 분리된 "항상 붙는 구역"으로 취급한다.

### 5.2 호출 지점 변경

- `generateKnowledgeAndPersist()`: `llmGenerationClient.generateKnowledge(renderedPrompt, ...)` 호출 직전 → `llmGenerationClient.generateKnowledge(appendDailyTrendContext(renderedPrompt, targetDate), ...)`로 변경. 실제 사용된 `trend.getId()`를 `saveSuccessHistory()`/`saveFailureHistory()`에 `usedTrendId`로 함께 전달(§2.3)
- `previewManualGeneration()`: `llmGenerationClient.generateKnowledge(request.getPromptContent(), ...)` 호출 직전 동일하게 `appendDailyTrendContext()` 적용. 이때 사용한 트렌드를 **응답에 스냅샷으로 포함**(§5.3, Codex 2차 검증 반영 — 미리보기와 저장 사이 트렌드가 재생성돼도 저장 시점에 다시 조회하지 않고 미리보기 때 값을 그대로 사용)
- `saveManualGenerationFromPreview()`: 클라이언트가 되돌려준 `dailyTrendId`를 그대로 `saveSuccessHistory()`에 전달(재조회하지 않음)

### 5.3 DTO 변경

| DTO | 추가 필드 |
|-----|-----------|
| `GenerationPreviewRequest` | (변경 없음 — `targetDate` 기준으로 서버가 자체 조회) |
| `GenerationPreviewResponse` | `dailyTrendId(Long)`, `dailyTrendKeywords(List<String>)`, `dailyTrendSummary(String)` — 트렌드가 없으면 전부 `null`/빈 값 |
| `GenerationSaveRequest` | `dailyTrendId(Long)` — 프론트엔드 JS가 미리보기 응답을 그대로 폼 hidden 필드에 담아 저장 요청에 echo(기존 `generatedTitle`/`generatedSummary`/`generatedDetail`을 이미 이 방식으로 처리 중이므로 동일 패턴 재사용) |

### 5.4 `generationComposePage()` (컨트롤러)

`buildRenderedPromptForManual()` 호출 이후 `dailyTrendInsightService.findLatestTrendForAdmin()`이 아니라 **`targetDate`에 해당하는 트렌드**(`findByTrendDate(targetDate)`)를 조회해 `model.addAttribute("dailyTrendPreview", ...)`로 확인 화면에 전달(§6 읽기 전용 구역용). 없으면 `null` → 템플릿에서 "트렌드 없음" 안내 문구 표시(FR-04 폴백 상태를 관리자가 인지하도록).

---

## 6. `ScheduledGenerationExecutor` 변경

```java
@Scheduled(cron = "0 * * * * *")
public void executeScheduledGeneration() {
    LocalDateTime now = LocalDateTime.now();
    if (!generationScheduleService.isExecutionDue(now)) {
        return;
    }

    try {
        dailyTrendInsightService.generateDailyTrend(LocalDate.now());
    } catch (Exception trendException) {
        log.warn("Scheduled daily trend generation failed: {}", trendException.getMessage());
        // 예외를 여기서 흡수 — 지식 생성 단계로 전파하지 않음(Plan §3.3)
    }

    GenerationExecutionResult executionResult = dailyKnowledgeGenerationService.executeScheduledGeneration(LocalDate.now());
    if (executionResult.isSuccess()) {
        generationScheduleService.markExecuted(now);
    }
    ...
}
```

`markExecuted()` 조건은 **기존 코드 그대로**(`executionResult.isSuccess()`만 기준) — Plan §3.3에서 확정한 대로 트렌드 성공/실패는 이 판단에 전혀 관여하지 않는다. 트렌드 실패 시 재시도는 자동으로 일어나지 않으며, 관리자가 `[오늘의 개발 트렌드]` 탭에서 수동 재생성해야 한다(Plan §6).

---

## 7. 관리자 화면

### 7.1 `admin/crawling.html` — `[오늘의 개발 트렌드]` 탭 신설

`weeklyInsight` 탭(§324~392, 기존 코드)과 동일한 구조를 복제:

```html
<button type="button" class="admin-tab-btn" data-tab-target="dailyTrend">오늘의 개발 트렌드</button>
```

```html
<section class="admin-panel generation-tab-panel daily-trend-admin-panel" data-tab-panel="dailyTrend" hidden>
    <div class="daily-trend-admin-header">
        <div>
            <h3>오늘의 개발 트렌드</h3>
            <p>기준일 테크 뉴스(부족 시 최근 3일)를 기반으로 홈에 노출할 일일 트렌드를 생성합니다.</p>
        </div>
        <form method="post" th:action="@{/admin/daily-trend/generate}" class="daily-trend-generate-form">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
            <label for="dailyTrendReferenceDate">기준일</label>
            <input type="date" id="dailyTrendReferenceDate" name="referenceDate"
                   th:value="${dailyTrendReferenceDate}" th:max="${T(java.time.LocalDate).now()}">
            <button type="submit" th:text="${dailyTrend == null} ? '트렌드 생성' : '트렌드 재생성'">트렌드 생성</button>
        </form>
    </div>

    <article class="daily-trend-admin-card" th:if="${dailyTrend != null}">
        <div class="daily-trend-admin-meta">
            <span th:text="${dailyTrend.trendDate}">2026-08-13</span>
            <span th:text="'분석 뉴스 ' + ${dailyTrend.sourceNewsCount} + '건'">분석 뉴스 0건</span>
            <span th:classappend="${dailyTrend.visible} ? ' active' : ' inactive'" class="status-badge"
                  th:text="${dailyTrend.visible} ? '노출 중' : '숨김'">노출 중</span>
        </div>
        <div class="daily-trend-keywords">
            <span class="daily-trend-tag" th:each="kw : ${dailyTrend.keywords}" th:text="'#' + ${kw}">#태그</span>
        </div>
        <p th:text="${dailyTrend.summary}">트렌드 요약</p>
        <form method="post" th:action="@{/admin/daily-trend/{id}/toggle-visible(id=${dailyTrend.id})}">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
            <button type="submit" class="secondary"
                    th:text="${dailyTrend.visible} ? '사용자 노출 끄기' : '사용자 노출 켜기'">사용자 노출 끄기</button>
        </form>
    </article>

    <p class="empty" th:if="${dailyTrend == null}">아직 생성된 오늘의 개발 트렌드가 없습니다.</p>

    <table class="admin-table daily-trend-history-table" th:if="${!#lists.isEmpty(dailyTrendList)}">
        <thead><tr><th>기준일</th><th>키워드</th><th>뉴스 수</th><th>노출 상태</th></tr></thead>
        <tbody>
        <tr th:each="trend : ${dailyTrendList}">
            <td th:text="${trend.trendDate}">2026-08-13</td>
            <td th:text="${#strings.arrayJoin(trend.keywords, ', ')}">Rust, AI</td>
            <td th:text="${trend.sourceNewsCount}">0</td>
            <td th:text="${trend.visible} ? '노출 중' : '숨김'">노출 중</td>
        </tr>
        </tbody>
    </table>
</section>
```

### 7.1.1 생성 시도 이력(성공/실패) — 구현 검증 중 추가(0.3)

위 테이블은 `DailyTrendInsight`(성공 결과만)만 보여준다. `DailyTrendGenerationHistory`(실패 포함 전체 시도 이력, §2.2)는 최초 구현에 관리자 UI가 전혀 없어 "이력은 저장되지만 볼 방법이 없는" 상태였다(사용자가 기존 `admin/generation.html`의 "생성 이력" 탭 + 오류 상세 모달(`historyErrorModalOverlay`) 패턴을 기억하고 지적해 발견). 동일 패턴을 재사용해 `[오늘의 개발 트렌드]` 탭에 추가:

- `DailyTrendGenerationHistoryRepository.findTop20ByOrderByCreatedAtDesc()` 신설(`GenerationHistoryRepository`/`CrawlHistoryRepository`와 동일한 명명 규칙)
- `DailyTrendInsightService.findRecentGenerationHistoryForAdmin()` 신설, `AdminPageController.crawlingPage()`에서 `dailyTrendHistoryList` 모델 속성으로 전달
- `admin/crawling.html`의 `dailyTrend` 탭 안에 "최근 생성 시도 이력" 테이블 추가 — `admin/generation.html`의 `generationHistoryTable`과 동일하게 `.history-error-cell`/`.history-error-trigger`/`.history-error-full` 구조로 오류 메시지를 클릭 시 모달로 표시
- `historyErrorModalOverlay` 모달 마크업 자체가 `crawling.html`에는 없었으므로 `generation.html`에서 그대로 복사해 추가(JS의 `bindHistoryErrorModal()`은 `admin.js`에 페이지 공용으로 이미 있어 별도 JS 수정 불필요 — `document.querySelectorAll('.history-error-trigger')`로 페이지 내 모든 트리거를 자동으로 찾음)

### 7.2 `AdminPageController` 라우트 추가

`weekly-insight` 라우트(§556~590, 기존 코드)와 동일한 패턴:

```java
@PostMapping("/daily-trend/generate")
public String generateDailyTrend(@RequestParam(value = "referenceDate", required = false) LocalDate referenceDate,
                                  RedirectAttributes redirectAttributes) {
    return executeAdminAction(redirectAttributes, "/admin/crawling", null, () -> {
        var dailyTrend = dailyTrendInsightService.generateDailyTrend(referenceDate);
        redirectAttributes.addFlashAttribute("adminMessage",
                "오늘의 개발 트렌드가 생성되었습니다. 기준일: " + dailyTrend.getTrendDate());
    });
}

@PostMapping("/daily-trend/{id}/toggle-visible")
public String toggleDailyTrendVisible(@PathVariable("id") Long trendId, RedirectAttributes redirectAttributes) {
    return executeAdminAction(redirectAttributes, "/admin/crawling", null, () -> {
        var dailyTrend = dailyTrendInsightService.toggleVisible(trendId);
        redirectAttributes.addFlashAttribute("adminMessage",
                Boolean.TRUE.equals(dailyTrend.getVisible()) ? "오늘의 개발 트렌드가 사용자 화면에 노출됩니다." : "오늘의 개발 트렌드가 숨겨졌습니다.");
    });
}
```

`crawling` 페이지 렌더링 메서드(`weeklyAiInsight`/`weeklyAiInsightList` 모델 속성을 채우는 기존 메서드)에 `dailyTrend`/`dailyTrendList` 속성도 함께 채우도록 확장.

### 7.3 `generation-compose.html` — 트렌드 읽기 전용 구역

기존 프롬프트 textarea 위/아래에 편집 불가 구역 추가:

```html
<section class="daily-trend-readonly-block" th:if="${dailyTrendPreview != null}">
    <h4>참고: 오늘의 개발 트렌드(자동 반영됨, 편집 불가)</h4>
    <p th:text="'키워드: ' + ${#strings.arrayJoin(dailyTrendPreview.keywords, ', ')}">키워드: Rust, AI</p>
    <p th:text="${dailyTrendPreview.summary}">트렌드 요약</p>
</section>
<p class="daily-trend-missing-guide" th:if="${dailyTrendPreview == null}">
    기준일에 생성된 트렌드가 없어 트렌드 없이 생성됩니다. (관리자 → 크롤링 관리 → 오늘의 개발 트렌드에서 먼저 생성 가능)
</p>
```

---

## 8. 사용자 화면 (`index.html`)

`weekly-ai-insight-section`과 `dailyKnowledgeChunks` 슬라이더 사이(§1.3 Plan 확정 위치)에 삽입:

```html
<section class="content-section daily-trend-section" th:if="${dailyTrend != null}">
    <div class="section-head">
        <h2>오늘의 개발 <span class="insight-word">Trend</span></h2>
        <p th:text="${dailyTrend.trendDate + ' | 분석 뉴스 ' + dailyTrend.sourceNewsCount + '건'}">
            2026-08-13 | 분석 뉴스 0건
        </p>
    </div>
    <div class="daily-trend-card">
        <div class="daily-trend-keywords">
            <span class="daily-trend-tag" th:each="kw : ${dailyTrend.keywords}" th:text="'#' + ${kw}">#태그</span>
        </div>
        <p class="daily-trend-summary" th:text="${dailyTrend.summary}">오늘의 트렌드 요약</p>
    </div>
</section>
```

`InsightPageController.index()`에 `model.addAttribute("dailyTrend", dailyTrendInsightService.findLatestVisibleTrend());` 추가(§6: `selectedDate`와 무관하게 최신 공개 1건 — `weeklyAiInsight`와 동일한 위치·방식으로 추가).

트렌드 카드 클릭 시 딥링크 없음(Plan Out of Scope). 배치는 물리적으로 "트렌드 → 학습 게시물" 순서로 고정해 인과관계를 전달(Plan Purpose).

---

## 9. Implementation Sequencing (중요)

`OracleSchemaMigrationRunner.java`는 본 기능 착수 시점에 **다른 미승인 Plan(`weekly-ai-insight-hashtag.md`)도 같은 파일에 스키마 변경(컬럼 추가)을 필요로 한다**(2026-08-13 확인, 해당 Plan은 아직 Draft·미승인).

- 본 기능은 신규 테이블 2개(`daily_trend_insight`, `daily_trend_generation_history`) + 시퀀스 2개 + `generation_history.used_trend_id` 컬럼 추가가 필요
- 두 기능 중 어느 쪽이든 먼저 구현에 들어가는 쪽이 `OracleSchemaMigrationRunner.java`를 먼저 커밋하고, 나머지 한쪽은 그 이후에 반영한다(동시 미커밋 상태로 같은 파일을 건드리지 않음)
- 구현 착수 전 `git status`로 `OracleSchemaMigrationRunner.java`가 다른 세션에서 미커밋 상태로 수정 중이 아닌지 재확인할 것(해시태그 Design §9와 동일 원칙)

---

## 10. Error Handling

| 상황 | 처리 |
|------|------|
| 기준일 + 3일 폴백에도 분석할 뉴스가 없음 | `DailyTrendGenerationHistory`에 `FAILED` 기록, `IllegalStateException` → 스케줄 경로는 흡수(§6), 관리자 수동 경로는 `executeAdminAction`이 에러 메시지로 표시 |
| LLM 응답에 유효 키워드가 3개 미만 | 파싱 실패 → `LlmClientException` → 위와 동일하게 `FAILED` 기록 |
| `DailyKnowledge` 생성 시점에 해당 기준일 트렌드가 없음 | `appendDailyTrendContext()`가 원본 프롬프트를 그대로 반환 — 트렌드 없이 정상 생성(FR-04), `usedTrendId=null`로 이력 기록 |
| 과거 `DailyKnowledge`(트렌드 없음) 조회/렌더링 | 홈 화면은 `DailyTrendInsight`를 독립적으로 조회하므로 영향 없음 — `GenerationHistory.usedTrendId`가 `null`이어도 관리자 이력 목록은 정상 렌더링 |
| 수동 미리보기 이후 저장 전 트렌드 재생성 | 저장 시점에 트렌드를 재조회하지 않고, 미리보기 응답에 담긴 `dailyTrendId`를 그대로 사용(§5.3). **단, 구현에서는 ID만 전달되고 트렌드 본문(키워드/요약) 자체는 스냅샷으로 함께 전달되지 않음 — §13 Known Gaps 참고** |

---

## 11. Security Considerations

기존 `weekly-ai-insight.md`(v0.2) §6, 해시태그 Design §6과 동일한 관점:

- 트렌드 키워드/요약도 LLM이 생성한 외부 유래 텍스트 — 프롬프트 인젝션 방지 로직 부재라는 기존 Known Gap의 연장(신규 이슈 아님, Plan NFR "Inherited Risk"에서 Design 범위로 명시)
- 화면 출력은 `th:text`로 이스케이프(XSS 안전)
- `previewManualGeneration()`이 트렌드 컨텍스트를 서버에서 재조회하지 않고 클라이언트가 보낸 `dailyTrendId`를 그대로 신뢰하는 것은 **저장(save) 단계에서만** 허용(§5.3) — preview 단계는 항상 서버가 `targetDate` 기준으로 자체 조회하므로 클라이언트가 임의의 트렌드 내용을 주입할 수 없음

---

## 12. 테스트 계획

`DailyTrendInsightServiceTest`(신규):
- 당일 뉴스 3건 이상 → 당일만 사용해 생성
- 당일 뉴스 2건 이하 → 3일 폴백 적용
- 3일 폴백에도 뉴스 없음 → 실패 + 이력 기록
- 재생성 시 upsert(같은 `trendDate` row 갱신, 신규 row 생성 안 됨)
- 키워드 5개 초과 응답 → 앞 5개만 저장
- 키워드 2개 이하 응답 → 실패 처리

`DailyKnowledgeGenerationServiceTest`(확장):
- 트렌드 있음 → 프롬프트에 트렌드 블록 포함해 LLM 호출됨
- 트렌드 없음 → 트렌드 블록 없이 기존과 동일하게 LLM 호출됨(폴백)
- 템플릿에 `${dailyTrend}` 있음 → 그 위치에 치환 / 없음 → 말미에 추가
- `previewManualGeneration()` 응답에 `dailyTrendId`/`dailyTrendKeywords`/`dailyTrendSummary` 포함 확인
- `saveManualGenerationFromPreview()`가 요청의 `dailyTrendId`를 그대로 `GenerationHistory.usedTrendId`에 저장(재조회 안 함) 확인

`ScheduledGenerationExecutorTest`(확장):
- 트렌드 생성 예외 발생 시에도 지식 생성이 정상 호출됨(예외 격리 확인)
- 트렌드 성공/실패 각각의 경우 `markExecuted()`가 지식 생성 `success` 값에만 의존함을 확인(§3.3 매트릭스 재현)

---

## 13. Known Gaps / 후속 작업 후보

- `weekly-ai-insight.md`(v0.2) §8, 해시태그 Design §8의 기존 Known Gap(날짜 검증, race condition 등)은 본 기능과 무관하게 유효 — 본 문서에서 다루지 않음(Plan NFR Quality Criteria와 동일 경계)
- 일간 트렌드를 모아 주간 트렌드에 집계 반영하는 연동은 Plan Out of Scope — 후속 Plan으로 분리
- 트렌드 카드 클릭 딥링크, 관리자 수동 편집 UI는 Plan Out of Scope
- **수동 생성 저장 시 트렌드 스냅샷 미보존(2026-08-13, 구현 검증 중 발견, 사용자 확인 후 의도적으로 보류)**: `saveManualGenerationFromPreview()`는 미리보기 시점에 사용한 `dailyTrendId`를 `GenerationHistory.usedTrendId`에 정확히 저장하지만, 트렌드 본문(키워드/요약) 자체는 스냅샷으로 함께 저장하지 않는다. `DailyTrendInsight`는 재생성 시 같은 `trendDate` row를 upsert(값만 덮어씀, 새 row/버전 생성 안 함)하므로, "미리보기 화면을 열어둔 채로 그 사이 관리자가 같은 날짜 트렌드를 재생성 → 뒤늦게 저장"하는 좁은 타이밍이 겹치면 `usedTrendId`로 나중에 조회했을 때 **실제로 프롬프트에 반영됐던 내용이 아니라 그 시점 이후 변경된 내용**이 나온다. 즉 FR-08("이 지식이 어떤 트렌드를 근거로 만들어졌는지 식별 가능")이 이 좁은 race window에서는 완전히 보장되지 않는다.
  - **영향 범위**: 예약 생성 경로는 해당 없음(트렌드 조회~LLM 호출~저장이 한 번의 스케줄 실행 안에서 끊김 없이 일어나 재생성이 끼어들 틈이 없음). 수동 생성 경로만 해당
  - **왜 지금 안 고치는지**: 단일 관리자가 운영하는 포트폴리오 서비스 특성상 "미리보기 대기 중 트렌드 재생성"이 실제로 겹칠 가능성이 낮다고 판단해, 사용자가 지금 수정 대신 기록만 남기고 넘어가기로 결정(2026-08-13)
  - **재검토 트리거/해결 방향**: 관리자가 여러 명으로 늘거나 이 문제로 실제 혼선이 발생하면, `GenerationSaveRequest`/`GenerationPreviewResponse`에 트렌드 키워드·요약 본문 자체를 스냅샷 필드로 추가해 ID 재조회 없이 저장하도록 보완(작은 패치로 가능, 별도 Plan 불필요)

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-13 | 최초 작성 — Plan(Approved) 기반 설계. 신규 엔티티 2개(`DailyTrendInsight`, `DailyTrendGenerationHistory`), `GenerationHistory` 확장, LLM 클라이언트 확장, `DailyKnowledgeGenerationService` 강제 주입 로직, 스케줄 예외 격리, 관리자 탭(§7.1)·홈 카드(§8) 구현 방식을 코드 레벨로 확정. `OracleSchemaMigrationRunner` 순서 리스크 §9에 명시 | Claude (Design 세션) |
| 0.2 | 2026-08-13 | `codex exec -s danger-full-access`로 구현 위임 → 완료(git add/commit/push 미실행, 작업 트리에만 변경). Claude 독립 검증 수행: `git diff` 전체 파일 리뷰 + `./gradlew test --rerun` 직접 실행(101 tests, 0 failures — Codex 자체 보고와 일치). 핵심 로직(FR-07 강제 주입, §6 예외 격리, §9 스키마 마이그레이션, LLM 파싱 검증)은 설계와 일치 확인. **검증 중 gap 발견**: 수동 저장 경로가 `usedTrendId`는 정확히 전달하지만 트렌드 본문 스냅샷은 전달하지 않아, 미리보기~저장 사이 트렌드 재생성 시 FR-08이 깨지는 좁은 race window 존재 — 사용자 확인 후 지금은 수정하지 않고 §13 Known Gaps에 기록하기로 결정(§10 표, §13 신규 항목에 반영). 홈 화면 트렌드 카드 배치가 설계 문구(`dailyKnowledgeChunks` 슬라이더 앞)와 다르게 "오늘의 개발 인사이트" 단일 카드 앞에 놓인 것을 확인 — 인과관계 전달 취지에는 더 부합해 결함으로 보지 않음 | Claude (구현 검증 세션) |
| 0.3 | 2026-08-13 | 사용자가 기존 `admin/generation.html`의 "생성 이력" 탭 + 오류 상세 모달 패턴을 기억하고 "트렌드 쪽엔 왜 없냐"고 지적 → `DailyTrendGenerationHistory`(실패 이력 포함)가 저장은 되지만 관리자 UI에 전혀 노출되지 않는 설계 누락 확인(§7.1.1 신설). Claude가 직접 패치(Codex 재위임 없이): `DailyTrendGenerationHistoryRepository.findTop20ByOrderByCreatedAtDesc()`, `DailyTrendInsightService.findRecentGenerationHistoryForAdmin()`, 컨트롤러 모델 속성, `crawling.html`에 이력 테이블 + 기존 오류 모달 마크업 재사용 추가. `./gradlew test --rerun` 재확인(101 tests, 0 failures, 회귀 없음) | Claude (구현 검증 세션) |
