# Daily-Dev-Insight
Daily Dev Insight – 개발 지식·기술 뉴스 자동 큐레이션 서비스


아래 내용 그대로 사용하면 **포트폴리오 프로젝트 1개**로 완성된다.

설명 구조, 목적, 아키텍처, 기술스택, 구현기능, ERD, API 설계까지 바로 넣을 수 있게 정리했다.

---

# 프로젝트명

**Daily Dev Insight – 개발 지식·기술 뉴스 자동 큐레이션 서비스**

---

# 1. 프로젝트 개요

개발자에게 필요한 **하루 1개의 핵심 지식**과 **최신 기술 뉴스를 자동으로 수집·요약·정리**해 제공하는 웹 서비스다.

LLM을 활용해 매일 자동으로 갱신되고, 사용자는 날짜별로 정제된 개발 정보만 효율적으로 소비할 수 있다.

---

# 2. 프로젝트 목적

1. 분산된 기술 정보(뉴스/아티클)를 매일 한곳에서 확인
2. LLM 기반 요약을 통해 핵심 내용만 빠르게 습득
3. 단순 뉴스 리스트가 아닌 **지식 + 뉴스**의 통합 정보 흐름 제공
4. 개발자 개인 학습 아카이브 제공(북마크, 히스토리)

---

# 3. 주요 기능

### 1) 자동 지식 생성

- 매일 1회 LLM 프롬프트 실행
- “오늘의 개발 지식(개발/CS/인프라/시장 중 랜덤)” 생성
- 요약 + 상세 설명 자동 생성
- DB 저장

### 2) 기술 뉴스 자동 크롤링

- GeekNews, ZDNet, InfoQ, ITWorld 등 RSS 기반 기사 수집
- 본문 자동 요약
- 출처·링크와 함께 저장

### 3) 웹 서비스 기능

- 오늘의 지식
- 오늘의 기술 뉴스
- 날짜별 히스토리
- 카테고리별 지식 탐색
- 북마크/관심사 기반 추천
- 기사 원문 링크 제공

---

# 4. 시스템 아키텍처

```
       [Scheduler(LLM + Crawler)]
                 ↓
         [Spring Boot API]
                 ↓
             [PostgreSQL]
                 ↓
       [Next.js Frontend UI]
                 ↓
              [사용자]

```

### 동작 흐름

1. Scheduler가 매일 09:00 실행
2. LLM 프롬프트로 "오늘의 지식" 생성
3. 크롤러가 기술 기사 수집 → LLM이 요약
4. DB 저장
5. Next.js가 API로 데이터를 호출해 화면 렌더링
6. 과거 데이터는 히스토리로 저장·검색 가능

---

# 5. 기술 스택

### Backend

- **Java 21 / Spring Boot 3**
- Spring Scheduler
- Spring Data JPA
- PostgreSQL
- Jsoup (크롤링)
- OpenAI API or Claude API

### Frontend

- **Next.js 14**
- TypeScript
- TailwindCSS
- React Query

### Infra

- **Vercel** (Frontend)
- **Render / Railway / EC2** (Backend)
- GitHub Actions (스케줄러/배포 자동화 가능)

---

# 6. DB 스키마(ERD 요약)

### daily_knowledge

| column | type | desc |
| --- | --- | --- |
| id | bigint PK |  |
| date | date | 오늘 날짜 |
| category | varchar | backend/cs/cloud/economy |
| title | varchar | 지식 제목 |
| summary | text | 요약 |
| detail | text | 상세 |
| created_at | timestamp |  |

### tech_news

| column | type | desc |
| --- | --- | --- |
| id | bigint PK |  |
| date | date |  |
| source | varchar |  |
| title | varchar |  |
| url | varchar |  |
| summary | text |  |
| created_at | timestamp |  |

---

# 7. API 설계

### GET /api/daily

오늘의 지식 반환

### GET /api/daily/{date}

특정 날짜의 지식 조회

### GET /api/news/today

오늘 기술 뉴스 목록

### GET /api/news/{date}

특정 날짜 뉴스 조회

### POST /api/admin/generate

관리자용 수동 실행(LLM + 뉴스 업데이트)

---

# 8. 스케줄러 구조 예시

```java
@Scheduled(cron = "0 0 9 * * *")
public void runDailyUpdate() {
    // 1. LLM 지식 생성
    var knowledge = llmService.generateDailyKnowledge();

    // 2. 뉴스 크롤링 + 요약
    var newsList = crawlerService.collectTechNews();
    var summarized = newsList.stream()
        .map(n -> llmService.summarize(n))
        .collect(Collectors.toList());

    // 3. DB 저장
    dailyKnowledgeRepository.save(knowledge);
    techNewsRepository.saveAll(summarized);
}

```

---

# 9. 프롬프트 예시(LLM)

### 오늘의 지식 자동 생성

```
"오늘의 개발 지식을 생성해줘. 카테고리는 backend/cs/cloud/economy 중 하나를 랜덤으로 선택해라.
요약 5줄 + 상세 설명 7~10줄로 구성해라.
실무 적용 사례와 간단 예제 코드도 포함해라."

```

### 기술 뉴스 요약

```
"다음 기술 기사를 개발자 기준으로 5줄로 요약해줘.
핵심은 변화/이유/영향 3가지 기준으로 정리해라."

```

---

# 10. 기능 확장 아이디어

- 개인 맞춤 추천(태그 기반)
- 북마크 기능
- 알림: “오늘의 지식 업데이트됨”
- RSS/Slack/Discord 연동
- 모바일 반응형 UI
- 관리자 대시보드(조회수/클릭률/관심태그 분석)

---
