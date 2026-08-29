# DailyDevInsight 문제 해결 사례

> 목적: Notion은 짧게 유지하고, 기술적 판단과 근거, 남은 위험은 GitHub에 남긴다.
> Notion 요약: https://app.notion.com/p/DailyDevInsight-RSS-AI-3b8806eb2ec081d6be8fd464578b8f4e

이 문서는 Notion에 짧게 소개한 세 가지 문제 해결 사례를 상세히 정리한다.

## 1. RSS 크롤러 보안 강화

### 문제

크롤러는 관리자가 등록한 RSS 소스 URL만 요청하지 않는다. RSS 피드 안에 포함된 기사 URL도 다시 요청해 상세 본문과 메타데이터를 가져온다. 이 기사 URL은 외부 RSS 발행자가 통제하므로 서버가 그대로 신뢰하면 안 된다.

XML 파서도 XXE 방어가 더 필요했다. secure processing과 entity expansion 비활성화만으로는 방어 경계를 명확히 설명하기 어려웠다.

### 판단

URL 요청 경계와 XML 파싱 경계를 함께 강화하기로 했다.

- loopback, link-local, private, unspecified, multicast 주소 차단
- 요청 전 호스트 주소 해석과 검증
- 자동 리다이렉트 비활성화
- 3xx `Location` 헤더를 매 단계 재검증
- 리다이렉트 최대 횟수 제한
- `DOCTYPE` 거부
- 외부 general/parameter entity 비활성화

### 결과

커밋 `76ec0c8`에 반영했고, 설계 근거는 [crawling-security-hardening.md](../02-design/features/crawling-security-hardening.md)에 정리했다.

후속 커밋 `1789bd4`에서는 크롤링 트랜잭션과 외부 I/O를 분리해 네트워크 호출이 긴 DB 트랜잭션을 붙잡는 구조를 줄였다. 관련 문서는 [crawling-transaction-io-separation.md](../02-design/features/crawling-transaction-io-separation.md)이다.

### 남은 위험

DNS 리바인딩은 아직 잔여 위험으로 남아 있다. 검증 시점의 DNS 해석 결과와 실제 연결 시점의 해석 결과가 달라질 수 있기 때문이다. 완전한 완화에는 검증된 IP로 직접 연결하면서 Host/TLS 동작을 보존하는 HTTP 클라이언트 구성과 운영망 egress 제한이 필요하다.

## 2. 댓글 성능 판단

### 문제

댓글과 대댓글이 늘어나면 트리 조립 로직이 느려질 것이라는 우려가 있었다.

하지만 실제 병목을 확인하지 않고 알고리즘부터 바꾸면 복잡도만 늘고 핵심 문제를 놓칠 수 있었다.

### 판단

먼저 실제 조회 흐름을 확인했다.

댓글 서비스는 댓글을 한 번 조회하고, 작성자 이름을 일괄 조회한 뒤, `parentCommentId` 기준으로 메모리에서 트리를 조립한다. 콘텐츠 상세 요청 1건 기준으로 O(n)에 가까운 구조이며, 재귀 쿼리나 N+1 조회 패턴은 아니었다.

따라서 더 직접적인 개선 지점은 댓글 트리 알고리즘이 아니라 콘텐츠별 댓글 조회를 받쳐주는 DB 인덱스라고 판단했다.

### 결과

콘텐츠별 댓글 조회를 위해 Oracle 복합 인덱스를 추가했다.

```sql
CREATE INDEX idx_insight_comment_content
ON insight_comment (content_type, content_id, is_deleted, created_at)
```

판단 근거는 [insight-comment.md](../02-design/features/insight-comment.md)에 정리되어 있다.

### 남은 위험

이 판단은 코드 흐름과 스키마를 기준으로 한 개선이다. Oracle 실행계획, 인덱스 적용 전후 응답시간, 대량 데이터 부하 테스트 수치는 아직 측정하지 않았다.

## 3. AI 콘텐츠 파이프라인 연결

### 문제

초기 구조에서는 RSS 뉴스 수집과 일일 지식 생성이 별도 기능처럼 보였다. 뉴스는 수집되고 AI 콘텐츠는 생성되지만, 둘 사이의 연결이 항상 명확하지 않았다.

### 판단

RSS 뉴스와 일일 지식 생성 사이에 일일 트렌드 계층을 추가했다.

- 최근 크롤링 뉴스에서 일일 트렌드 키워드와 요약 생성
- 일일 트렌드를 별도 엔티티로 저장
- 사용자 홈 화면에 트렌드 카드 노출
- 해당 날짜의 트렌드가 있으면 일일 지식 생성 프롬프트에 자동 반영
- 트렌드가 없거나 생성 실패해도 기존 방식으로 지식 생성 가능

### 결과

커밋 `73e8a05`에 반영했고, 상세 설계는 [daily-trend-insight.md](../02-design/features/daily-trend-insight.md)에 정리했다.

관리자는 일일 트렌드를 생성/재생성하고 생성 이력을 확인할 수 있다. 이후 일일 지식 생성 화면에서 해당 트렌드가 읽기 전용 참고 정보로 표시된다.

### 남은 위험

수동 미리보기와 저장 사이에 같은 날짜의 트렌드가 재생성되면 좁은 race window가 생길 수 있다. 현재 구현은 트렌드 ID를 저장하지만, 트렌드 본문 전체를 불변 스냅샷으로 저장하지 않는다. 이 한계는 [daily-trend-insight.md](../02-design/features/daily-trend-insight.md)에 Known Gap으로 기록했다.

## 근거 범위

이 문서는 포트폴리오 설명용이지 벤치마크 리포트가 아니다.

- 보안과 아키텍처 관련 주장은 설계 문서와 커밋에 근거한다.
- 테스트 수치는 각 커밋과 문서에 기록된 시점의 값이다.
- 부하 테스트나 운영 트래픽 지표는 아직 주장하지 않는다.
