# Unit 9: 캐시 정책 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 2 (화면 없음)

## ① 목적

Redis 기반 캐시 매니저와 캐시별 TTL 정책을 정의하는 횡단 관심사. Unit 1(홈), Unit 2(상세)가 실사용.

## ② 관련 파일

- `RedisCacheConfig` — `CacheManager` Bean, 캐시명 상수 5개, TTL 정책
- 실사용처: `DailyInsightService`(Unit 1), `DailyKnowledgeService`(주간 TOP), `InsightDetailService`(Unit 2)

## ③ 진입 엔드포인트

없음 (설정 클래스)

## ④ 핵심 호출 흐름

`@Cacheable`/`@CacheEvict` 어노테이션이 붙은 서비스 메서드가 Spring AOP를 통해 자동으로 이 `CacheManager`를 거침. 직접 호출 흐름은 없고 선언적으로 동작.

## ⑤ 데이터/외부 연동

- Redis 서버 연동 (`RedisConnectionFactory` 필요, `docker-compose.yml`에 Redis 컨테이너 정의 여부는 별도 확인 필요)
- 값 직렬화: `GenericJackson2JsonRedisSerializer` + `JavaTimeModule`(LocalDate 등 처리) + `activateDefaultTyping`(다형성 타입 정보 포함 직렬화)

## ⑥ 캐시별 정책 (실측)

| 캐시명 | 상수 | TTL | 사용처 |
|---|---|---|---|
| `weeklyHotTop10` | `CACHE_WEEKLY_TOP10` | 20분 | `DailyKnowledgeService.findWeeklyHotKnowledgeTop10` (Unit 1) |
| `weeklyHotTop5` | `CACHE_WEEKLY_TOP5` | 20분 | `DailyKnowledgeService.findWeeklyHotKnowledgeTop5` (Unit 1) |
| `insightsByDate` | `CACHE_INSIGHTS_BY_DATE` | 10분 | `DailyInsightService.getInsightsByDate` (Unit 1, `/api/insights`) |
| `insightsByRange` | `CACHE_INSIGHTS_BY_RANGE` | 5분 | `DailyInsightService.getInsightsByRange` (Unit 1, 홈 화면) |
| `insightEngagement` | `CACHE_INSIGHT_ENGAGEMENT` | 90초 | `InsightDetailService.getEngagementOnly` (Unit 2) |
| (미지정 나머지) | — | 기본 10분 | — |

- `disableCachingNullValues()` 전역 적용 — null 반환값은 캐시 안 함
- 캐시 무효화는 각 서비스가 `@CacheEvict`로 개별 관리 (이 설정 파일 자체에는 무효화 트리거 없음)

## ⑦ 화면 요약

없음

## ⑧ 패턴 특이사항

- **[Unit 1 조사 정정 반영]** 최초 Unit 1 compact card 작성 시 "Top10은 캐시 안 됨"으로 잘못 기록했으나, 실제로는 Top10/Top5 둘 다 20분 TTL로 캐시됨 — grep 기반 조사의 컨텍스트 누락으로 인한 오류였고 이 unit 작성 중 발견해 정정함(§Version History 참고, `home-insight-list.md`도 함께 수정됨)
- `insightEngagement`(90초)만 다른 캐시 대비 TTL이 극단적으로 짧음 — 좋아요/북마크/댓글처럼 실시간성이 중요한 데이터라서로 추정되나 명시적 근거 주석은 없음
- `@CacheEvict(allEntries = true)` 패턴이 여러 서비스(Unit 2, 12, 13)에서 공통적으로 쓰임 — 특정 키만 evict하는 세밀한 무효화는 이 프로젝트에 없음 (일괄 무효화만 존재)

## ⑨ 알아둘 점 / 리스크

- `activateDefaultTyping(NON_FINAL)`은 역직렬화 시 클래스 타입 정보를 신뢰한다는 뜻 — Redis 자체가 내부 인프라라 외부 입력이 직접 캐시에 주입될 경로는 없어 보이나, 일반적으로 이 설정은 역직렬화 보안 이슈(gadget chain 등)의 알려진 주의 대상이라는 점은 기록해둘 가치 있음(이 프로젝트에서 실제 악용 경로가 있는지는 확인 못함)
- Redis 장애 시 캐시 미스가 아니라 애플리케이션 전체 오류로 이어지는지(fail-open/fail-closed) 여부는 이번 조사 범위 밖 — 정밀화 시 확인 필요

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) — Unit 1의 "Top10 캐시 미적용" 오류를 발견해 정정 |
