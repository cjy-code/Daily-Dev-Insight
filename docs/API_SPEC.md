# Daily Dev Insight API 명세서 (MVP)

## 1. 범위

- 본 문서는 MVP 단계의 API 범위만 정의한다.
- 목표: 범위 과다를 방지하고 핵심 가치를 빠르게 제공한다.
- 기본 주소: `http://localhost:8080`
- 콘텐츠 타입: `application/json`

## 2. API가 다루는 MVP 기능

- 오늘의 지식 1개
- 오늘의 뉴스 리스트
- 날짜별 조회 (`yyyy-MM-dd`)

MVP 제외:

- 추천
- 알림
- 관리자 페이지

## 3. 엔드포인트 요약

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/insights` | 특정 날짜 기준 오늘의 지식 1개와 뉴스 리스트를 반환 |

## 4. GET `/api/insights`

### 쿼리 파라미터

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `date` | String (`yyyy-MM-dd`) | N | 서버 로컬 오늘 날짜 | 조회 대상 날짜 |

### 요청 예시

```http
GET /api/insights?date=2026-04-03 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### 성공 응답

- 상태 코드: `200 OK`

```json
{
  "date": "2026-04-03",
  "todayKnowledge": {
    "id": 101,
    "title": "Spring Boot 3.2 Release Notes",
    "url": "https://spring.io/blog/2024/01/01/spring-boot-3-2",
    "source": "Spring Blog",
    "summary": "Spring Boot 3.2 주요 변경 사항과 마이그레이션 포인트",
    "publishedAt": "2026-04-03"
  },
  "newsList": [
    {
      "id": 201,
      "title": "Java 21 Virtual Threads in Practice",
      "url": "https://example.com/java21-vthreads",
      "source": "Dev Weekly",
      "summary": "가상 스레드 적용 패턴과 성능 트레이드오프",
      "publishedAt": "2026-04-03"
    },
    {
      "id": 202,
      "title": "REST API Design Best Practices",
      "url": "https://example.com/rest-best-practices",
      "source": "API Design",
      "summary": "리소스 네이밍과 오류 응답 설계 기본",
      "publishedAt": "2026-04-03"
    }
  ]
}
```

### 응답 필드 정의

최상위 필드:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | String (`yyyy-MM-dd`) | Y | 조회 기준 날짜 |
| `todayKnowledge` | Object 또는 `null` | Y | 해당 날짜의 대표 지식 1건 |
| `newsList` | Array | Y | 해당 날짜의 뉴스 목록 (0..N) |

항목 필드 (`todayKnowledge` 및 `newsList` 각 요소):

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `id` | Long | Y | 인사이트 식별자 |
| `title` | String | Y | 제목 |
| `url` | String | Y | 원문 링크 |
| `source` | String | Y | 출처명 |
| `summary` | String | N | 요약 |
| `publishedAt` | String (`yyyy-MM-dd`) | Y | 발행일 |

## 5. 오류 처리 (MVP)

### 날짜 형식 오류

- 조건: `date`가 `yyyy-MM-dd` 형식과 일치하지 않음
- 상태 코드: `400 Bad Request`

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "잘못된 날짜 형식입니다. yyyy-MM-dd 형식을 사용하세요.",
  "path": "/api/insights"
}
```

### 서버 오류

- 상태 코드: `500 Internal Server Error`

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "예상하지 못한 서버 오류가 발생했습니다.",
  "path": "/api/insights"
}
```

## 6. 빈 상태 규칙

- 해당 날짜의 대표 지식이 없으면 `todayKnowledge`는 `null`이어야 한다.
- 해당 날짜의 뉴스가 없으면 `newsList`는 빈 배열(`[]`)이어야 한다.
- 빈 상태도 정상 응답(`200 OK`)으로 처리한다.

## 7. 구현 정렬 메모

- 본 문서는 MVP 목표 계약(Contract)을 반영한다.
- 현재 코드가 단순 배열을 반환한다면, MVP 구현 시 본 응답 구조로 맞춰야 한다.
