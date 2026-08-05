# 주간 AI 인사이트 (Weekly AI Insight) Gap Analysis

> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **대상 문서**: `docs/01-plan/features/weekly-ai-insight.md` (Plan), `docs/02-design/features/weekly-ai-insight.md` (Design)
> **대상 코드**: 커밋 `b7989e3`([feat] 주간 AI 인사이트 생성/노출 기능 추가), `b0dcd00`([fix] CSS 누락분)

---

## 검증 범위 선언 (Audit Discipline)

- **검증한 것**: `WeeklyAiInsightService`, `WeeklyAiInsight` entity, `AdminPageController`/`InsightPageController`의 관련 부분, `LlmGenerationClient`/`MockLlmGenerationClient`/`OpenAiLlmGenerationClient`, `OracleSchemaMigrationRunner`의 weekly_ai_insight 관련 부분, 템플릿(`admin/crawling.html` 일부, `index.html` 일부), CSS를 **코드 정독**으로 Design 문서와 1:1 대조
- **검증하지 않은 것**: `./gradlew test` 실제 실행 — 이 세션의 셸 환경 `JAVA_HOME`이 Java 8(1.8.0_202)로 설정되어 있어 프로젝트 요구 버전(JDK 21)과 맞지 않아 빌드 자체가 configuration 단계에서 실패함(환경 문제, 코드 결함 아님). 테스트 코드 내용은 정독했으나 **실제 실행 통과 여부는 미확인**
- **검증하지 않은 것**: 브라우저를 통한 실제 화면 렌더링(관리자 카드, 사용자 홈 섹션) — 코드/템플릿 정독으로만 확인

---

## 1. Plan(FR) 대비 구현 일치 여부

| ID | Requirement | 코드 근거 | 일치 여부 |
|----|-------------|-----------|-----------|
| FR-01 | 기준일 기준 최근 7일 뉴스로 생성 | `generateWeeklyInsight()`: `weekEndDate`/`weekStartDate` 계산 후 `findByNewsDateBetweenOrderByNewsDateDescIdDesc` 조회 | ✅ 일치 |
| FR-02 | 소스 뉴스 0건 시 에러 처리 | `sourceNewsList.isEmpty()` → `IllegalStateException`, `AdminPageController.generateWeeklyAiInsight()`의 `catch (Exception)` → `adminError` flash | ✅ 일치 |
| FR-03 | 같은 기간 재생성 시 갱신, `visible` 유지 | `findByWeekStartDateAndWeekEndDate().orElseGet(...)` 후 `updateAnalysis()` 호출 — `updateAnalysis()`는 `visible` 필드를 건드리지 않음 | ✅ 일치 |
| FR-04 | 노출 여부 토글 | `toggleVisible()`: `changeVisible(!현재값)`, `save()` 명시 호출 없이 dirty checking 의존 | ✅ 일치 |
| FR-05 | 홈 화면은 `visible=true` 최신 1건만 | `findLatestVisibleInsight()` → `findTopByVisibleTrueOrderByWeekEndDateDescIdDesc()` | ✅ 일치 |
| FR-06 | 관리자 미리보기(공개 무관) + 최근 5건 이력 | `findLatestInsightForAdmin()`(`findTopByOrderByWeekEndDateDescIdDesc`), `findRecentInsightsForAdmin()`(`findTop5By...`) | ✅ 일치 |

**FR 일치율: 6/6 = 100%**

---

## 2. Design 문서 세부 주장 대조

| Design 문서 주장 | 코드 확인 결과 | 일치 여부 |
|---|---|---|
| 뉴스 최대 40건(`MAX_PROMPT_NEWS_COUNT`)만 프롬프트에 포함 | `buildWeeklyInsightPrompt()`: `.limit(MAX_PROMPT_NEWS_COUNT)`, `MAX_PROMPT_NEWS_COUNT = 40` | ✅ 일치 |
| 뉴스 요약 350자(`MAX_SUMMARY_LENGTH`)로 절단 | `truncate(techNews.getSummary(), MAX_SUMMARY_LENGTH)`, `MAX_SUMMARY_LENGTH = 350` | ✅ 일치 |
| `sourceNewsCount`는 40건이 아니라 전체 조회 건수 저장 | `createWeeklyAiInsight(..., sourceNewsList.size())` / `updateAnalysis(..., sourceNewsList.size())` — 둘 다 `.size()`(전체), 프롬프트는 `.limit(40)`으로 별도 | ✅ 일치 (의미 불일치 자체도 코드상 사실로 확인됨) |
| LLM 호출이 `@Transactional` 메서드 내부에서 발생 | `generateWeeklyInsight()`에 `@Transactional` 부착, 그 안에서 `llmGenerationClient.generateWeeklyInsight(...)` 호출 | ✅ 일치 |
| `toggleVisible()`은 `save()` 미호출, dirty checking 의존 | 코드상 `save()` 호출 없음, `@Transactional` 메서드 내 엔티티 변경만 수행 | ✅ 일치 |
| `MockLlmGenerationClient`는 프롬프트 무시하고 고정 문구 반환 | `generateWeeklyInsight()` 구현이 `prompt`/`weekStartDate`/`weekEndDate` 파라미터를 사용하지 않고 고정 빌더 반환 | ✅ 일치 |
| 관리자 컨트롤러가 `exception.getMessage()`(기술 메시지)를 그대로 노출 | `AdminPageController.generateWeeklyAiInsight()`/`toggleWeeklyAiInsightVisible()` 둘 다 `catch (Exception exception)` → `exception.getMessage()`를 flash에 그대로 사용 | ✅ 일치 |
| `weekly_ai_insight` 테이블/시퀀스는 코드 마이그레이션에만 존재 | `OracleSchemaMigrationRunner`에 `ensureWeeklyAiInsightTable()`/`ensureWeeklyAiInsightSequence()` 존재, `docs/sql/`에 대응 파일 없음(미확인 상태 유지) | ✅ 일치 |
| CSS 클래스(`.weekly-ai-insight-*`)로 관리자 카드/홈 섹션 스타일링 | `admin.css`(`.weekly-ai-insight-admin-*`), `index.css`(`.weekly-ai-insight-*`) 커밋 `b0dcd00`에 존재 | ✅ 일치 (Design 문서엔 CSS 클래스명까지는 기재 안 되어 있었으나 상충 없음) |

**세부 주장 일치율: 9/9 = 100%**

---

## 3. 종합 Match Rate

| 구분 | 일치 | 전체 | 비율 |
|---|---|---|---|
| Plan FR | 6 | 6 | 100% |
| Design 세부 주장 | 9 | 9 | 100% |
| **종합** | **15** | **15** | **100%** |

> bkit PDCA 기준(90% 이상)을 충족한다. Design 문서가 애초에 코드를 읽고 역산 작성되었기 때문에 100%는 "새로 검증했다"기보다 "작성 당시의 정확도가 이번 재검증에서도 재확인되었다"는 의미로 해석해야 한다.

---

## 4. 불일치 항목

없음. (검증 범위 내에서 Plan/Design 문서와 코드 간 불일치를 발견하지 못함)

---

## 5. Known Gaps와의 구분

아래 항목들은 **문서-코드 불일치가 아니라, 애초에 Design 문서 §8에 "코드 자체의 한계"로 이미 기록되어 있던 사항**이다. 혼동 방지를 위해 여기서도 명시한다.

- 서버 측 날짜 범위 검증 부재
- 동시 생성 요청 시 race condition 가능성
- OpenAI 호출 timeout/retry 부재
- `LlmClientException`의 사용자 메시지 미사용
- 테스트 커버리지 공백(소스 뉴스 0건, 존재하지 않는 id 토글, LLM 예외 전파)

이 항목들은 Gap 분석의 "불일치"가 아니라 **후속 개선 과제**이며, Plan 문서 §6 Next Steps 3번(별도 개선 Plan으로 분리)에서 다룬다.

---

## 6. 미확인 리스크 (Audit 선언에 따른 스코프 누락 사항)

- 테스트 3건(`WeeklyAiInsightServiceTest`)이 코드상 의도대로 작성되어 있음은 확인했으나, 이 세션에서 **실제 실행 통과 여부는 검증하지 못함** (JDK 버전 불일치로 빌드 불가). 다음 세션에서 JDK 21 환경으로 재실행 확인 필요
- 브라우저 렌더링 확인 안 함 — CSS 클래스명과 HTML 구조가 대응되는지는 정적 대조만 수행

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-05 | 최초 Gap 분석 — Plan/Design vs 코드 100% 일치 확인, 테스트 미실행 스코프 명시 | Claude (대화 기반) |
