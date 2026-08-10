# 크롤링 보안 강화 (SSRF/XXE) Design Document

> **Summary**: `RssNewsCrawlerClient`의 상세페이지 재요청 URL에 대한 SSRF 방어와 RSS XML 파싱의 XXE 방어를 추가한다
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-10
> **Status**: Draft (구현 착수 전)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | `docs/03-analysis/admin-crawling-management.md` §1.2(SSRF)/§1.3(XXE) — 크롤링은 관리자가 RSS 소스 URL만 지정하고, 실제 상세페이지 재요청 URL과 XML 콘텐츠는 **외부 발행자(RSS 피드)가 결정**하므로 서버가 악의적 내부망 요청을 대신 실행할 위험이 있음 |
| **WHO** | 크롤링을 실행하는 서버(관리자가 트리거하거나 스케줄러가 자동 실행) |
| **RISK** | 피드가 오염되거나 악의적이면 서버가 클라우드 메타데이터 엔드포인트(`169.254.169.254`), 내부망 관리 URL 등으로 요청을 보낼 수 있고(SSRF), RSS XML에 외부 엔티티가 포함되면 파서가 이를 해석해 로컬 파일/내부 리소스를 노출할 수 있음(XXE) |
| **SUCCESS** | 사설/루프백/링크로컬 대역으로의 요청이 차단되고, XML 파싱 시 DOCTYPE/외부 엔티티가 완전히 거부된다. 기존 정상 크롤링 동작(공개 인터넷 RSS 피드 수집)에는 회귀가 없다 |
| **SCOPE** | `RssNewsCrawlerClient` 내부 검증 로직만 대상. `TechNewsCrawlingService`의 트랜잭션-I/O 결합 구조 개선(P0)은 별도 Design 문서로 분리(범위 밖) |

---

## 1. Overview

### 1.1 목적

RSS 피드 콘텐츠(외부 발행자 통제)에 의해 결정되는 상세페이지 URL 요청과 XML 파싱 과정에서, 서버가 신뢰할 수 없는 입력에 의해 내부망을 스캔하거나 악성 XML을 그대로 해석하지 않도록 방어 로직을 추가한다.

### 1.2 배경

`docs/03-analysis/admin-crawling-management.md`(2026-08-07)에서 기능 확장 전 사전 점검으로 발견된 P1 이슈 2건. 크롤링 보안/완성도 개선을 우선순위화하기로 한 2026-08-10 PM 세션 결정에 따라, 가장 착수 비용이 낮고 영향도가 높은 항목으로 첫 번째로 착수한다.

### 1.3 관련 파일

| 레이어 | 파일 | 변경 대상 메서드 |
|--------|------|------------------|
| Service | `admin/service/RssNewsCrawlerClient.java` | `validateSourceUrl`, `fetchRssDocument`, `fetchArticleDetail`(신규 리다이렉트 처리 추가) |
| Test | `test/.../RssNewsCrawlerClientTest.java` (신설 또는 기존 파일 확인 후 확장) | 신규 |

---

## 2. XXE 방어 강화

### 2.1 현재 상태 (`fetchRssDocument`, `:209-237`)

```java
documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
documentBuilderFactory.setExpandEntityReferences(false);
documentBuilderFactory.setXIncludeAware(false);
```

`setExpandEntityReferences(false)`는 DOM에서 엔티티를 별도 노드로 펼칠지 여부만 제어하며, 파서가 외부 엔티티를 해석하는 것 자체를 막지 않는다(널리 알려진 오해 지점).

### 2.2 변경 사항

`fetchRssDocument()`의 `DocumentBuilderFactory` 생성 부분에 다음 기능 플래그를 추가한다(OWASP XXE Prevention Cheat Sheet 권고 기준):

```java
documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
documentBuilderFactory.setXIncludeAware(false); // 기존 유지
documentBuilderFactory.setExpandEntityReferences(false); // 기존 유지
```

- `disallow-doctype-decl=true`가 핵심 방어선이다 — RSS 2.0/Atom 피드는 정상적으로 DOCTYPE 선언이 필요 없으므로, DOCTYPE 자체를 통째로 거부해도 정상 피드 파싱에 영향이 없다
- 위 기능이 지원되지 않는 파서 구현체를 만나 `ParserConfigurationException`이 발생하면, 현재 최상위 `crawlArticles()`의 `catch (Exception)` → `IllegalStateException("RSS 기사 수집에 실패했습니다: ...")` 경로로 이미 처리되므로 별도 예외 처리 추가는 불필요

### 2.3 수용 기준

- 정상 RSS/Atom 피드(기존 소스)는 기존과 동일하게 파싱 성공
- `<!DOCTYPE foo [...]>` 또는 외부 엔티티 참조가 포함된 XML을 입력하면 파싱이 예외로 실패(=엔티티가 해석되지 않고 거부됨)

---

## 3. SSRF 방어 추가

### 3.1 현재 상태 (`validateSourceUrl`, `:243-252`)

스킴이 http/https인지만 검사한다. 이 메서드는 RSS 소스 URL(`crawlArticles`, `:68`, 관리자 입력)과 상세페이지 URL(`fetchArticleDetail`, `:179`, **RSS 피드 콘텐츠가 결정**) 양쪽에서 호출되므로, 여기를 강화하면 두 지점 모두에 적용된다.

### 3.2 변경 사항 — 사설/루프백/링크로컬 대역 차단

`validateSourceUrl()`에 스킴 검증 뒤, 호스트명을 `InetAddress`로 resolve하여 다음 조건이면 거부하도록 추가한다:

```java
InetAddress address = InetAddress.getByName(uri.getHost());
if (address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isAnyLocalAddress()
        || address.isMulticastAddress()) {
    throw new IllegalArgumentException("내부망 또는 예약된 주소로의 요청은 허용되지 않습니다: " + uri.getHost());
}
```

`isLinkLocalAddress()`가 `169.254.0.0/16`(클라우드 메타데이터 엔드포인트 `169.254.169.254` 포함)을 커버한다. `isSiteLocalAddress()`가 `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`을 커버한다.

`InetAddress.getByName()` 실패(호스트 resolve 불가)는 `UnknownHostException`이며, 이는 `validateSourceUrl()`의 checked 예외 시그니처에 포함시키거나 기존 패턴처럼 `IllegalArgumentException`으로 감싸 전파한다.

### 3.3 리다이렉트 처리 — TOCTOU 완화

`fetchRssDocument()`/`fetchArticleDetail()`은 `HttpURLConnection`의 기본 리다이렉트 자동 추적(`HttpURLConnection.setFollowRedirects`, 기본 `true`)을 그대로 쓰고 있다. 최초 URL은 검증을 통과해도, 서버가 3xx 응답으로 내부망 주소로 리다이렉트시키면 방어가 무력화된다.

두 메서드 모두에 다음을 적용한다:

1. `connection.setInstanceFollowRedirects(false);`로 자동 리다이렉트 비활성화
2. 응답 코드가 3xx(301/302/303/307/308)이면 `Location` 헤더를 읽어 새 URL에 대해 `validateSourceUrl()`을 다시 실행한 뒤 재요청
3. 최대 리다이렉트 횟수(예: 3회)를 넘으면 실패 처리 — 무한 리다이렉트 루프 방지

이 로직은 두 메서드에서 중복되므로 `private Document/DetailCrawlData followValidatedRedirects(...)` 형태로 추출하기보다는, 우선 각 메서드에 인라인으로 최소 구현하고(범위를 좁게 유지) 중복 제거는 Known Gaps로 남긴다 — 기존 `admin-crawling-management.md` §1.5에서 이미 "조건 정규화 로직 3중 중복" 패턴이 지적된 프로젝트이므로, 이번에 새로운 중복을 또 만드는 대신 **다음 리팩터링 후보로만 기록**한다.

### 3.4 수용 기준

- 공개 인터넷 RSS 피드/상세페이지(기존 정상 소스)는 기존과 동일하게 크롤링 성공
- `http://127.0.0.1/...`, `http://169.254.169.254/...`, `http://10.0.0.1/...` 등 사설/루프백/링크로컬 주소를 RSS 소스 URL로 등록하면 크롤링이 즉시 실패
- 정상 URL로 시작했다가 3xx로 내부망 주소로 리다이렉트되는 경우도 차단됨

---

## 4. Known Gaps (본 범위에서 다루지 않음)

- **DNS 리바인딩(TOCTOU) 잔여 위험**: `validateSourceUrl()`의 resolve 시점과 실제 `HttpURLConnection.connect()`의 resolve 시점이 분리되어 있어, 그 사이 DNS 응답이 바뀌면(공격자가 짧은 TTL로 사설 IP를 반환하도록 조작) 검증을 우회할 수 있다. 완전한 방어는 검증된 IP로 직접 커넥션을 맺는 커스텀 `Socket`/`HttpClient` 구현이 필요하며, 이는 본 범위를 넘는 더 큰 변경이라 별도 후속 과제로 남긴다
- 리다이렉트 처리 로직의 두 메서드 간 중복(§3.3) — 다음 리팩터링 후보
- `crawlingInProgress` 다중 인스턴스 미대응(P2), 조건 정규화 로직 중복(P2) 등 분석 문서의 다른 항목은 본 문서 범위 밖 — 별도 Design으로 분리 예정

---

## 5. 테스트 계획

`RssNewsCrawlerClient`에 대한 기존 테스트 파일 유무를 구현 착수 시 먼저 확인하고, 없으면 신설한다. 최소 다음 시나리오를 포함한다:

- 정상 공개 URL은 기존과 동일하게 통과(회귀 없음)
- 루프백/사설/링크로컬 주소는 `IllegalArgumentException`으로 거부
- DOCTYPE 선언이 포함된 XML은 파싱 실패로 거부(XXE 차단 확인)
- 3xx 리다이렉트가 사설 주소를 가리키면 거부

---

## 6. Implementation Sequencing

`OracleSchemaMigrationRunner.java`, DB 스키마와 무관한 변경이므로 현재 다른 세션의 P4-3(비밀번호 인코더 마이그레이션) 작업과 파일이 겹치지 않는다 — 순서 제약 없이 바로 착수 가능하다.

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-10 | 최초 작성 — `admin-crawling-management.md` §1.2/§1.3 기반, SSRF(사설망 차단+리다이렉트 재검증)/XXE(DOCTYPE 전면 거부) 설계 확정 | Claude (PM 세션 진행) |
