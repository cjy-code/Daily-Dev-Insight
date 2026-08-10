# 크롤링 트랜잭션-외부I/O 분리 + 스케줄러 무한재시도 수정 (P0) Design Document

> **Summary**: `TechNewsCrawlingService`의 크롤링 실행 흐름에서 DB 트랜잭션이 외부 네트워크 I/O(RSS/상세페이지/썸네일) 전체를 감싸는 구조를 수집(I/O)/영속화(DB) 두 단계로 분리하고, 스케줄러가 실패/스킵 시에도 매분 재시도하는 Known Gap을 함께 수정한다
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-10
> **Status**: Draft (구현 착수 전)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | `docs/03-analysis/admin-crawling-management.md` §1.1 — 크롤링 트랜잭션이 외부 I/O 전체 기간 DB 커넥션을 점유하며, 이것이 `docs/02-design/features/scheduler.md` §⑨에 이미 기록된 "무한 재시도 방지 로직 없음" Known Gap과 결합해 크롤링 이력(최근 20건 제한)을 SKIPPED/FAILED 더미로 오염시킨다 |
| **WHO** | 크롤링을 실행하는 서버(관리자 수동 트리거 또는 매분 스케줄러 폴링) |
| **RISK** | 리팩터링 과정에서 정상 크롤링 동작(수집→필터→저장→이력 기록)에 회귀가 생길 위험. Spring AOP 프록시의 self-invocation 함정(같은 클래스 내부 메서드 호출은 `@Transactional`이 적용되지 않음)으로 인해 "분리했다고 착각하지만 실제로는 여전히 하나의 트랜잭션"이 되는 실수 위험이 특히 크다 — §3.4에서 별도 명시 |
| **SUCCESS** | DB 커넥션이 외부 I/O(RSS 조회, 상세페이지 재요청, 썸네일 다운로드) 기간 동안 열려 있지 않다. 스케줄러는 실패/스킵 시에도 다음 정상 슬롯까지 대기하며 매분 재시도하지 않는다. 기존 정상 크롤링 동작(필터링/중복제거/이력 기록)에는 회귀가 없다 |
| **SCOPE** | `TechNewsCrawlingService`, `CrawlHistoryService`, `ScheduledCrawlingExecutor`만 대상. `crawlingInProgress`의 다중 인스턴스 미대응(P2)과 조건 정규화 로직 공통화(P2)는 별도 Design으로 분리(범위 밖) |

---

## 1. Overview

### 1.1 목적

크롤링 실행 흐름에서 "DB 트랜잭션이 열려 있는 시간"과 "외부 I/O를 기다리는 시간"을 분리해, DB 커넥션 점유 시간을 밀리초 단위로 줄이고, 스케줄러가 실패 슬롯에서 매분 재시도하며 이력을 오염시키는 문제를 함께 해소한다.

### 1.2 배경

`docs/03-analysis/admin-crawling-management.md`(2026-08-07) §1.1에서 발견된 P0 이슈. 이번 세션에서 §1.6(TechNews ID 충돌 가능성)을 검증·기각한 뒤, 분석 문서에 남은 유일한 P0 항목으로 다음 착수 대상으로 확정(2026-08-10).

### 1.3 관련 파일

| 레이어 | 파일 | 변경 유형 |
|--------|------|-----------|
| Service | `admin/service/TechNewsCrawlingService.java` | `executeCrawling()` 재구성(오케스트레이션만 담당, `@Transactional` 제거) |
| Service (신규) | `admin/service/TechNewsPersistenceService.java` | 신설 — 영속화 전용, 단일 `@Transactional` 메서드 |
| Service | `admin/service/CrawlHistoryService.java` | 이력 기록(RUNNING/SUCCESS/FAILED/SKIPPED) 메서드를 `TechNewsCrawlingService`에서 이관 |
| Component | `admin/service/ScheduledCrawlingExecutor.java` | `markExecuted()`를 성공 여부와 무관하게 항상 호출하도록 수정 |
| Test | 위 4개 파일에 대응하는 테스트 (신설 또는 기존 확인 후 확장) | 신규 |

---

## 2. 현재 구조의 문제 (코드 확인 완료)

### 2.1 트랜잭션-외부I/O 결합 (`TechNewsCrawlingService.java`)

- `executeManualCrawling()`(`:58`, `@Transactional`)과 `executeScheduledCrawling()`(`:153`, `@Transactional`)이 공통으로 호출하는 `executeCrawling()`(`:184`)이 다음을 **전부 같은 트랜잭션 안에서 순차 실행**한다:
  1. `saveRunningHistory()`(`:213`) — DB 쓰기
  2. `newsCrawlerClient.crawlArticles()`(`:215`) — RSS 목록 조회 1회 + 기사 건당 상세페이지 재요청(`RssNewsCrawlerClient.java:94`, 최대 `maxArticles`건, 건당 최대 `retryCount+1`회 재시도, 요청당 최대 60초 타임아웃) — **네트워크 I/O**
  3. `buildInsertTargets()`(`:231`) 내부의 `resolveThumbnailPath()`(`:474`) → `NewsThumbnailStorageService.downloadAndStoreThumbnail()` — 기사 건당 이미지 다운로드 — **네트워크/디스크 I/O**
  4. `techNewsRepository.saveAll()`(`:233`) — DB 쓰기
  5. `updateSuccessHistory()`/`updateFailureHistory()`(`:236`/`:245`) — DB 쓰기
- 이론상 `maxArticles=100`, `retryCount=5`, 타임아웃 60초 조합이면 2~3단계만으로 DB 커넥션이 매우 긴 시간 점유될 수 있다.

### 2.2 스케줄러 무한 재시도 (`ScheduledCrawlingExecutor.java:33-36`)

```java
CrawlExecutionResult executionResult = techNewsCrawlingService.executeScheduledCrawling(LocalDate.now(), schedule);
if (executionResult.isSuccess()) {
    crawlScheduleService.markExecuted(now);
}
```

- `markExecuted()`는 **성공했을 때만** 호출된다. `CrawlScheduleService.isExecutionDue()`(`:85-98`)는 `lastExecutedAt`을 기준으로 다음 cron 발화 시각을 계산하는데, 실패/스킵 시 `lastExecutedAt`이 갱신되지 않으므로 다음 발화 시각이 계속 "과거"로 남아 **매분(`@Scheduled(cron = "0 * * * * *")`, `ScheduledCrawlingExecutor.java:25`) 재시도**된다.
- 이것이 §2.1의 문제와 결합하면: 크롤링이 실패하거나(예: 소스가 일시적으로 응답 없음) 오래 걸려서 다음 분 폴링이 `crawlingInProgress`에 막혀 SKIPPED 처리되는 상황이 **분마다 반복**되고, `CrawlHistoryService.findRecentHistory()`(최근 20건 제한)가 이 더미로 가득 차 실제 유의미한 이력을 밀어낸다.

---

## 3. 설계: 수집(I/O)/영속화(DB) 단계 분리

### 3.1 흐름 재구성

```
[기존] executeCrawling() 전체가 하나의 @Transactional
  RUNNING 기록 → RSS수집(I/O) → 상세페이지(I/O)×N → 썸네일(I/O)×N → 저장 → 결과 기록
  └──────────────────────── 하나의 DB 트랜잭션 ────────────────────────┘

[변경] executeCrawling()은 오케스트레이션만, @Transactional 없음
  RUNNING 기록(짧은 트랜잭션, CrawlHistoryService)
    → RSS수집(I/O) → 상세페이지(I/O)×N → 썸네일(I/O)×N   ← 트랜잭션 없음
      → 영속화(짧은 트랜잭션, TechNewsPersistenceService: 중복확인+ID할당+saveAll)
        → 결과 기록(짧은 트랜잭션, CrawlHistoryService)
```

### 3.2 신규 클래스: `TechNewsPersistenceService`

수집이 끝난(썸네일까지 다운로드된) 기사 목록을 받아, DB 관련 작업만 짧은 트랜잭션 안에서 처리한다:

```java
@Service
@RequiredArgsConstructor
public class TechNewsPersistenceService {

    private final TechNewsRepository techNewsRepository;

    /**
     * @desc 수집·필터링·썸네일 다운로드가 끝난 기사 목록을 중복 제거 후 저장합니다.
     */
    @Transactional
    public List<TechNews> persistArticles(
            LocalDate targetDate,
            String sourceName,
            List<EnrichedArticle> enrichedArticles, // 기존 NewsArticleData + resolvedThumbnailPath
            boolean allowDuplicate
    ) {
        // 기존 buildInsertTargets()의 DB 관련 부분(findExistingUrls, resolveNextNewsId,
        // 중복 확인, TechNews.builder(), saveAll)을 그대로 이관 — 로직 변경 없음, 위치만 이동
    }
}
```

- `resolveThumbnailPath()`(네트워크 호출)는 이 클래스로 옮기지 **않는다** — §3.3에서 수집 단계로 이동.
- 기존 `buildInsertTargets()`의 중복 제거/ID 할당/`saveAll` 로직은 **그대로 재사용**(순수 이동, 로직 변경 없음) — 회귀 위험을 최소화하기 위해 알고리즘 자체는 건드리지 않는다.

### 3.3 `TechNewsCrawlingService.executeCrawling()` 재구성

```java
private CrawlExecutionResult executeCrawling(...) {
    int normalizedMaxArticles = normalizeMaxArticles(maxArticles);

    if (!crawlingInProgress.compareAndSet(false, true)) {
        crawlHistoryService.recordSkipped(triggerType, targetDate, sourceName, normalizedMaxArticles, "이미 다른 크롤링 작업이 실행 중입니다.");
        return CrawlExecutionResult.builder().success(false).errorCode("crawl_in_progress")...build();
    }

    Long runningHistoryId = crawlHistoryService.recordRunning(triggerType, targetDate, sourceName, normalizedMaxArticles);
    try {
        // --- 여기서부터 트랜잭션 없음 (네트워크 I/O) ---
        List<NewsArticleData> collectedArticles = newsCrawlerClient.crawlArticles(...);
        List<NewsArticleData> filteredArticles = filterArticles(collectedArticles, ...);
        List<EnrichedArticle> enrichedArticles = filteredArticles.stream()
                .map(article -> new EnrichedArticle(article, resolveThumbnailPath(article, targetDate)))
                .collect(Collectors.toList());
        // --- 여기까지 트랜잭션 없음 ---

        List<TechNews> savedArticles = techNewsPersistenceService.persistArticles(
                targetDate, sourceName, enrichedArticles, allowDuplicate
        ); // 짧은 트랜잭션

        crawlHistoryService.recordSuccess(runningHistoryId, collectedArticles.size(), savedArticles.size());
        return CrawlExecutionResult.builder().success(true)...build();
    } catch (Exception exception) {
        crawlHistoryService.recordFailure(runningHistoryId, exception);
        return CrawlExecutionResult.builder().success(false)...build();
    } finally {
        crawlingInProgress.set(false);
    }
}
```

- `crawlingInProgress` AtomicBoolean 가드는 **그대로 유지**(동일 인스턴스 내 동시 실행 방지라는 기존 역할 변경 없음, 다중 인스턴스 미대응은 별도 P2).
- `executeManualCrawling()`/`executeScheduledCrawling()`의 `@Transactional`도 **제거**한다 — 이 두 메서드가 `executeCrawling()`을 호출하는 것 자체가 기존 문제의 원인이었으므로, 클래스 전체에서 크롤링 실행 경로에는 `@Transactional`이 남지 않는다. `@Caching(evict=...)`는 트랜잭션과 무관하게 동작하므로 그대로 유지.

### 3.4 왜 `private` 메서드 분리가 아니라 별도 Bean이 필요한가 (중요)

Spring의 `@Transactional`/`@CacheEvict`는 **프록시 기반 AOP**로 동작한다. 같은 클래스 안에서 `this.someTransactionalMethod()`처럼 자기 자신을 호출하면(self-invocation) 프록시를 거치지 않으므로 어노테이션이 **적용되지 않는다** — "트랜잭션을 분리했다"고 코드만 나눠놓고 실제로는 여전히 호출자의 트랜잭션(또는 트랜잭션 없음) 안에서 그대로 실행되는 흔한 함정이다.

따라서 §3.2(`TechNewsPersistenceService`)와 §3.5(`CrawlHistoryService`의 기록 메서드들)는 `TechNewsCrawlingService`의 `private` 메서드가 아니라 **다른 Spring Bean의 public 메서드**로 만들어야 한다. `CrawlHistoryService`는 이미 별도 Bean으로 존재하므로(현재는 `findRecentHistory()`만 있음) 자연스럽게 이관 대상이 되고, 영속화 로직은 신규 Bean(`TechNewsPersistenceService`)으로 분리한다.

### 3.5 `CrawlHistoryService` 확장 — 이력 기록 메서드 이관

기존 `TechNewsCrawlingService`의 `saveRunningHistory()`(`:661`), `saveSkippedHistory()`(`:680`), `updateSuccessHistory()`(`:699`), `updateFailureHistory()`(`:719`)를 `CrawlHistoryService`로 그대로 이동(로직 변경 없음)하고, 각각 `recordRunning()`/`recordSkipped()`/`recordSuccess()`/`recordFailure()`로 이름을 정리한다. 각 메서드는 개별 `@Transactional`을 가지므로, RUNNING 기록은 크롤링 시작 즉시 커밋되어 관리자 화면에서 "실행 중" 상태를 바로 확인할 수 있다(기존 동작과 동일, 위치만 이동).

### 3.6 스케줄러 무한 재시도 수정 (`ScheduledCrawlingExecutor.java`)

```java
@Scheduled(cron = "0 * * * * *")
public void executeScheduledCrawling() {
    LocalDateTime now = LocalDateTime.now();
    if (!crawlScheduleService.isExecutionDue(now)) {
        return;
    }

    CrawlSchedule schedule = crawlScheduleService.getOrCreateSchedule();
    CrawlExecutionResult executionResult = techNewsCrawlingService.executeScheduledCrawling(LocalDate.now(), schedule);
    crawlScheduleService.markExecuted(now); // 성공 여부와 무관하게 항상 호출 (기존: if (executionResult.isSuccess())만)
    log.info("Reserved crawling executed at {} (success={}, errorCode={})", now, executionResult.isSuccess(), executionResult.getErrorCode());
}
```

- 이 슬롯을 "시도했다"는 의미로 `markExecuted`를 항상 호출한다 — 성공해야만 슬롯이 소비되는 기존 "성공 시점" 의미에서 "시도 시점" 의미로 바뀐다.
- 효과: 실패하거나(네트워크 오류 등) `crawlingInProgress`로 스킵되더라도, 다음 재시도는 **다음 cron 발화 시각**(기본값 `0 0 8 * * *` = 매일 08:00)까지 기다린다. 매분 재시도가 사라진다.

---

## 4. 변경 대상 파일

| 파일 | 변경 |
|---|---|
| `TechNewsCrawlingService.java` | `executeCrawling()` 재구성(위 §3.3), `executeManualCrawling()`/`executeScheduledCrawling()`에서 `@Transactional` 제거, 이력 기록 메서드 4개 삭제(§3.5로 이관), `buildInsertTargets()`의 DB 관련 부분을 `TechNewsPersistenceService`로 이관, `resolveThumbnailPath()`는 유지(수집 단계에서 호출) |
| `TechNewsPersistenceService.java`(신규) | `persistArticles()` 단일 `@Transactional` 메서드 — 기존 `buildInsertTargets()`의 중복확인/ID할당/`saveAll` 로직 그대로 이동 |
| `CrawlHistoryService.java` | `recordRunning()`/`recordSkipped()`/`recordSuccess()`/`recordFailure()` 추가(기존 `TechNewsCrawlingService`의 4개 메서드 이동) |
| `ScheduledCrawlingExecutor.java` | `markExecuted()` 호출을 성공 조건 없이 항상 실행하도록 변경 |

`RssNewsCrawlerClient.java`, `NewsThumbnailStorageService.java`, `CrawlScheduleService.java`는 **변경 불필요**(호출 방식/시그니처 그대로 재사용).

---

## 5. Error Handling

| 상황 | 처리 |
|------|------|
| 수집 단계(네트워크)에서 예외 발생 | 트랜잭션 없이 발생하므로 롤백 대상 없음. `executeCrawling()`의 catch에서 `crawlHistoryService.recordFailure()` 호출 — 기존과 동일한 사용자 관점 결과 |
| 영속화 단계(`persistArticles`)에서 예외 발생(DB 오류 등) | `TechNewsPersistenceService`의 `@Transactional`이 롤백 — 부분 저장 없음(기존과 동일 보장) |
| `recordRunning()` 자체가 실패(DB 연결 불가 등) | 크롤링을 시작하지 않고 예외 전파 — 기존에도 동일 지점에서 실패했을 시나리오 |
| RUNNING 기록 후 애플리케이션이 재시작 등으로 비정상 종료 | 기존과 동일한 미해결 Known Gap(RUNNING 상태로 영구히 남는 이력) — 이번 범위에서 다루지 않음(§6) |

---

## 6. Known Gaps (본 범위에서 다루지 않음)

- **중복 확인(`findExistingUrls`)과 실제 저장 사이의 TOCTOU**: 두 시점이 여전히 완전히 원자적이지 않다(단, 이는 기존 코드에도 이미 존재하던 특성이며 `tech_news.url`에 유니크 제약이 없어 DB 레벨에서도 강제되지 않음 — 이번 변경으로 새로 생기는 위험이 아니라 기존 특성을 그대로 유지하는 것)
- **스케줄 간격이 매우 촘촘한 경우(예: 1분 간격 cron)** `markExecuted`를 무조건 호출하면 실패한 슬롯 바로 다음의 정상 슬롯도 건너뛸 수 있다 — 현재 기본 cron은 `0 0 8 * * *`(1일 1회)이라 실질 영향 없음. 향후 사용자가 촘촘한 cron을 직접 설정하는 시나리오가 생기면 재검토
- **RUNNING 상태로 영구히 남는 이력**(재시작 등으로 SUCCESS/FAILED 갱신 없이 종료): 이번 범위 밖, 기존에도 동일
- **크롤링 요청 스레드가 여전히 동기 블로킹됨**(`/admin/crawling/run` 응답까지 전체 크롤링 시간만큼 대기): `admin-crawling.md` §2.2에 이미 기록된 별개의 UX 이슈이며, 이번 변경은 "DB 트랜잭션 점유 시간"만 줄일 뿐 요청 스레드 자체를 비동기화하지 않는다 — 범위 밖
- `crawlingInProgress` 다중 인스턴스 미대응(P2), 조건 정규화 로직 공통화(P2)는 분석 문서의 별개 항목 — 별도 Design으로 분리 예정

---

## 7. 테스트 계획

`TechNewsCrawlingServiceTest`(기존 파일 확인 후 확장 또는 신설), `TechNewsPersistenceServiceTest`(신규), `CrawlHistoryServiceTest`(신규 또는 확장), `ScheduledCrawlingExecutorTest`(기존 확인 후 확장)에 다음 시나리오 포함:

- 정상 크롤링 시 기존과 동일하게 수집→필터→중복제거→저장→SUCCESS 이력이 기록되는지(회귀 없음)
- 영속화 단계에서 예외 발생 시 부분 저장 없이 롤백되는지
- 수집 단계에서 예외 발생 시 저장이 아예 시도되지 않고 FAILED 이력이 기록되는지
- `crawlingInProgress`로 스킵될 때 SKIPPED 이력이 기록되는지(기존 동작 유지 확인)
- `ScheduledCrawlingExecutor`: 실패/스킵 결과를 받아도 `markExecuted()`가 호출되는지(신규 — Known Gap 수정 검증의 핵심)
- `ScheduledCrawlingExecutor`: 성공 시 기존과 동일하게 `markExecuted()`가 호출되는지(회귀 없음)

---

## 8. Implementation Sequencing

이번 변경은 `OracleSchemaMigrationRunner.java`, DB 스키마와 무관하다. 현재 진행 중인 다른 세션의 해시태그 기능(`weekly-ai-insight-hashtag`)도 크롤링 관련 파일을 건드리지 않으므로 파일 충돌 없이 바로 착수 가능하다.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-10 | 최초 작성 — `admin-crawling-management.md` §1.1 기반, 수집(I/O)/영속화(DB) 분리 + 스케줄러 무한재시도 수정 설계 확정. Spring AOP self-invocation 함정 회피를 위해 신규 Bean(`TechNewsPersistenceService`) 도입 및 이력 기록 메서드를 `CrawlHistoryService`로 이관하는 방식 채택 | Claude (코드 확인 기반) |
