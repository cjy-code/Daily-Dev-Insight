# 크롤링 관리 코드 평가 / 개선점 분석

> **Project**: dailyDevInsight
> **Date**: 2026-08-07
> **대상 문서**: `docs/02-design/features/admin-crawling-management.md`(Unit 12), `docs/02-design/screens/admin-crawling.md`, `docs/02-design/features/scheduler.md`(Unit 7), `docs/02-design/features/cache-policy.md`(Unit 9), `docs/02-design/features/db-schema-migration.md`(Unit 10)
> **대상 코드**: `TechNewsCrawlingService`(전체), `RssNewsCrawlerClient`(전체), `NewsThumbnailStorageService`(전체), `ScheduledCrawlingExecutor`, `CrawlScheduleService`, `CrawlHistoryService`, `CrawlConditionPresetService`, `AdminPageController`(크롤링 관련 엔드포인트)

---

## 검증 범위 선언 (Audit Discipline)

- **검증한 것**: 위 대상 코드 정독, 관련 설계 문서 5건과의 교차 대조
- **검증하지 않은 것**: `./gradlew test`/빌드 실행, 브라우저 실동작, Codex 등 외부 교차검증, `AdminManagementService`(게시물 관리)의 TechNews 수동 등록 경로 상세 확인
- 기능 확장(신규 기능 추가) 전 사전 점검 목적으로 수행 — Gap Analysis(Plan/Design vs 코드 불일치)가 아니라 **코드 품질/아키텍처 평가**가 중심이며, 크롤링(Unit 12)은 별도 `01-plan` 문서가 없어 `02-design`의 compact card를 SoR로 삼아 비교함
- 위 미검증 범위에서 이후 문제가 발견되면 새 이슈가 아니라 **스코프 누락**으로 기록할 것

---

## 1. 핵심 발견 (신규, 우선순위순)

### 1.1 [P0] 크롤링 트랜잭션이 외부 I/O 전체를 감싸며, 이것이 스케줄러의 기존 Known Gap과 결합해 이력을 오염시킴

- `TechNewsCrawlingService.executeManualCrawling()`(`:58` `@Transactional`)과 `executeScheduledCrawling()`(`:153` `@Transactional`)이 내부적으로 호출하는 `executeCrawling()`(`:184`)은 다음을 **모두 같은 트랜잭션 안에서 순차 실행**한다:
  1. RSS 목록 조회 1회
  2. `RssNewsCrawlerClient.crawlArticles()`(`:215`) — 기사 건당 상세페이지 재요청 1회(`RssNewsCrawlerClient.java:94`, 최대 `maxArticles`건까지 순차 반복, 건당 최대 `retryCount+1`회 재시도, 요청당 최대 60초 타임아웃)
  3. `buildInsertTargets()`(`:231`) 내부의 `resolveThumbnailPath()`(`:474`) → `NewsThumbnailStorageService.downloadAndStoreThumbnail()` — 기사 건당 이미지 다운로드 1회 추가
- 즉 **DB 커넥션이 외부 네트워크 I/O 전체 기간 동안 열려 있다.** `maxArticles=100`, `retryCount=5` 조합이면 이론상 매우 긴 시간 동안 트랜잭션/커넥션이 점유될 수 있다. `/admin/crawling/run` 요청 스레드도 이 시간만큼 동기 블로킹되며, 화면은 실제 진행률이 아닌 경과 시간 타이머만 보여준다(`admin-crawling.md` §2.2에 이미 기록됨).
- **기존 `scheduler.md`(Unit 7) §⑨의 Known Gap과 결합 시 더 심각해짐**: "계속 실패하는 경우 매분마다 재시도 발생 가능(무한 재시도 방지 로직 없음)"이 이미 문서화돼 있는데, 위 구조로 크롤링이 오래 걸리거나 반복 실패하면:
  - 실행 중: 다음 분 폴링이 `crawlingInProgress`(`:47`)에 막혀 SKIPPED 이력 반복 저장
  - 실패 시: `markExecuted`가 갱신되지 않아(`ScheduledCrawlingExecutor.java:34-36`) 매분 재시도
  - 두 경우 모두 `CrawlHistoryService.findRecentHistory()`(`:22`, 최근 **20건**만 조회)를 SKIPPED/FAILED 더미로 채워 실제 유의미한 이력을 밀어낼 수 있음
- 두 사실(신규 발견 + 기존 문서화된 Known Gap)이 독립적으로는 각각 중간 정도 이슈였지만, 결합하면 "느린 동기 크롤링 하나가 이력 시스템 전체의 신뢰성을 저하시킨다"는 단일 근본 원인으로 재구성됨

### 1.2 [P1] 상세페이지 재요청 URL에 대한 SSRF 방어 부재

- `RssNewsCrawlerClient.validateSourceUrl()`(`:243`)은 스킴이 http/https인지만 검사한다.
- 관리자가 직접 입력하는 건 RSS 소스 URL뿐이고, **기사 상세페이지 재요청 URL(`fetchArticleDetailWithRetry`, `:145`)은 RSS 피드 콘텐츠(외부 발행자)가 결정**한다. 피드가 오염되거나 악의적이면 서버가 내부망 주소(예: 클라우드 메타데이터 엔드포인트, 사내 관리자 URL 등)로 요청을 보낼 수 있다.
- 내부/사설 IP 대역 차단, 리다이렉트 체인 검증 등이 없음.

### 1.3 [P1] XXE 방어가 불완전할 수 있음

- `RssNewsCrawlerClient.fetchRssDocument()`(`:209`)는 `FEATURE_SECURE_PROCESSING`(`:230`)과 `setExpandEntityReferences(false)`(`:231`)만 설정한다.
- `setExpandEntityReferences(false)`는 **DOM에서 엔티티를 별도 노드로 펼칠지 여부만 제어**하며, 파서가 외부 엔티티를 해석하는 것 자체를 막지 않는다(널리 알려진 Java XXE 오해 지점). DOCTYPE 선언 차단이나 외부 일반/파라미터 엔티티 명시적 차단(`disallow-doctype-decl`, `external-general-entities` 등) 설정이 없다.
- RSS 소스는 관리자가 지정하지만, 피드 자체의 콘텐츠까지 관리자가 통제하는 것은 아니므로 리스크가 존재.

### 1.4 [P1→P2] `crawlingInProgress`가 인스턴스 로컬 — 다중 인스턴스 배포 시 무력화

- `TechNewsCrawlingService.crawlingInProgress`(`:47`, `AtomicBoolean` 필드)는 단일 JVM 내에서만 중복 실행을 막는다. 수평 확장(다중 인스턴스) 배포 시 인스턴스별로 별도 크롤링이 동시 실행될 수 있다. 현재 배포 형태가 단일 인스턴스라면 당장은 실질 리스크가 낮음 — 확장 계획이 있는지에 따라 우선순위 재조정 필요.

### 1.5 [P2] 조건 정규화/실행판단 로직의 반복적 중복

- `normalizeKeywordMatchType` / `normalizeIncludeKeywordOperators` / `normalizeStringList` / `joinAsCsv` / `splitCsv` 로직이 `TechNewsCrawlingService`(`:538-582`), `CrawlScheduleService`(`:162-201`), `CrawlConditionPresetService`(`:138-188`) 세 클래스에 거의 동일하게 중복 구현되어 있다.
- 이는 프로젝트에 이미 알려진 패턴과 같은 계열이다: `scheduler.md`(Unit 7) §⑧이 "`CrawlScheduleService`와 `GenerationScheduleService`의 `isExecutionDue`/`markExecuted` 로직이 거의 동일하게 중복 구현됨"을 이미 지적한 바 있다. 즉 "스케줄용/생성용/크롤링용 각각 복붙" 패턴이 이번 조사에서 한 번 더 확인됨 — 공통 컴포넌트 추출 여지가 프로젝트 전반에 걸쳐 있음.

### 1.6 [P2, 검증 완료(2026-08-10) — 실제 위험 없음] TechNews 수동 ID 생성과 동시 쓰기 경로 충돌 가능성 → 기각

- `TechNews` 엔티티는 `@GeneratedValue` 없이 `TechNewsCrawlingService.resolveNextNewsId()`(`:482`, `findTopByOrderByIdDesc()+1`)로 애플리케이션 레벨에서 다음 ID를 계산한다.
- **재확인 결과**: `AdminManagementService`에 TechNews를 다루는 메서드는 `updateTechNewsPost()`(`:232`), `updateTechNewsThumbnail()`(`:265`), `deleteTechNewsThumbnail()`(`:296`) 3개뿐이며, 전부 `techNewsRepository.findById()`로 기존 행을 조회한 뒤 `.id(originalNews.getId())`로 **동일 ID를 유지**하며 `save()`(JPA merge)한다 — 신규 ID를 발급하는 삽입이 아니라 수정이다.
- `grep -rn "new TechNews|TechNews.builder" backend/src/main/java`로 전체 코드베이스를 확인한 결과, `TechNews`를 **신규 생성**(빌더로 새 ID 할당)하는 경로는 `TechNewsCrawlingService`(크롤링 저장) 단 하나뿐이다. 관리자 게시물 관리 화면 등에 별도의 수동 생성 경로는 존재하지 않는다.
- **결론**: 이 항목이 가정한 "수동 생성 경로와 크롤링 경로의 ID 충돌"은 애초에 발생할 수 없는 시나리오였다. 별도 조치 불필요. `crawlingInProgress`(§1.4, 다중 인스턴스 미대응)는 여전히 유효한 별개 이슈로 남아 있음.

---

## 2. 기존 문서화된 Known Gaps (참고용 재확인, 새 이슈 아님)

`docs/02-design/screens/admin-crawling.md` §5에 이미 기록됨:
- 프리셋 삭제 UI/API 없음
- 예약은 애플리케이션이 고정 ID 1 레코드만 관리(DB 제약 아님)
- 예약 비활성 시 조건 입력 필드가 시각적으로만 비활성화(`aria-disabled`), "중복 저장 허용" 토글만 실제 비활성화
- 크롤링 이력 20건 제한(§1.1의 이력 오염 이슈와 결합 시 영향도 상향)

`docs/02-design/features/cache-policy.md`에 이미 기록됨:
- 현재 작업트리의 `DailyKnowledgeGenerationService`/`TechNewsCrawlingService` 캐시 evict 추가(uncommitted diff)는 §"[2026-08-06] Codex 교차검증 반영"에 근거가 명시된 **의도된 변경**임을 확인함 — 별도 이슈 아님

---

## 3. 종합 우선순위

| 순위 | 항목 | 근거 | 상태 |
|---|---|---|---|
| P0 | 크롤링 트랜잭션-외부I/O 결합 구조 개선(수집/저장 분리 또는 비동기화) + 스케줄러 무한재시도로 인한 이력 오염 | §1.1 (신규 발견 + `scheduler.md` 기존 Known Gap 결합) | **설계 완료(2026-08-10)** — `docs/02-design/features/crawling-transaction-io-separation.md`, 구현 착수 대기 |
| ~~P0~~ | ~~`/admin/generate`(즉시생성) 고아 엔드포인트 정책 확정~~ | `docs/04-report/admin-ai-generation.md` Next Steps에 이미 대기 중 | **완료(2026-08-10)** — 엔드포인트/서비스메서드/테스트 제거 커밋(`116cf46`) |
| ~~P1~~ | ~~상세페이지 재요청 URL SSRF 방어 추가~~ | §1.2 (신규) | **완료(2026-08-10)** — `docs/02-design/features/crawling-security-hardening.md`, 커밋 `76ec0c8` |
| ~~P1~~ | ~~RSS XML 파싱 XXE 방어 강화(DOCTYPE/외부 엔티티 명시적 차단)~~ | §1.3 (신규) | **완료(2026-08-10)** — 상동, 커밋 `76ec0c8` |
| P1 | 이미지 생성 fail-soft를 서비스 계약으로 명시 | `docs/02-design/features/admin-ai-generation.md` §8 | 미착수 |
| P2 | `crawlingInProgress` 다중 인스턴스 대응(배포 확장 계획에 따라 재조정) | §1.4 | 확인 필요 |
| P2 | 크롤링/스케줄/생성 조건 정규화·실행판단 로직 공통화 | §1.5 (신규 + `scheduler.md` 기존 Known Gap 결합) | 미착수 |
| ~~P2~~ | ~~TechNews 수동 ID 생성과 동시 쓰기 경로 충돌 가능성 검증~~ | §1.6 (검증 완료, 2026-08-10) | 기각 — 실제 위험 없음 |
| P2 | 콘텐츠·이력 저장 트랜잭션 정합성(AI 생성 쪽) | `docs/02-design/features/admin-ai-generation.md` §8 | 미착수 |
| P2 | 서비스 레벨 테스트 확충(크롤링/생성 양쪽) | 양쪽 문서 공통 | 미착수 |

---

## 4. Next Steps

- [x] 위 우선순위에 대해 사용자(PM)에게 보고하고 착수 순서 확정 — 2026-08-10, 순서: TechNews ID 검증 → P0(크롤링 트랜잭션) → 해시태그 기능
- [x] `AdminManagementService`(게시물 관리)의 TechNews 수동 생성 경로 존재 여부 확인(§1.6 검증) — 존재하지 않음, 기각 처리
- [ ] P0(크롤링 트랜잭션-외부I/O 결합) 설계 착수
- [ ] 착수 항목 확정 후 해당 항목만 별도 Design 갱신 → 구현 → Report 사이클 진행

---

## Version History

| Version | Date | Changes | Author |
|---|---|---|---|
| 0.1 | 2026-08-07 | 최초 작성 — 기능 확장 전 사전 점검 목적, 크롤링 코드 신규 발견 6건 + 관련 설계 문서(scheduler/cache-policy/db-schema-migration) 교차 확인 결과 반영 | Claude (대화 기반, 외부 교차검증 미실시) |
| 0.2 | 2026-08-10 | P0(`/admin/generate` 고아 엔드포인트), P1(SSRF/XXE) 3건 구현·커밋 완료 반영(`116cf46`, `76ec0c8`). §1.6(TechNews ID 충돌 가능성)을 `AdminManagementService`/전체 코드베이스 재확인으로 검증 완료 — 수동 생성 경로 자체가 존재하지 않아 기각. Next Steps 갱신, 다음 착수 대상은 P0(크롤링 트랜잭션-외부I/O 결합) | Claude (검증) |
