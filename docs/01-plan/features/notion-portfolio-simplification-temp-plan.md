# Notion Portfolio Simplification Temp Plan

> Status: Temporary planning note. Delete this file after the Notion portfolio cleanup is completed.
> Target: DailyDevInsight portfolio Notion page
> Intent: Keep Notion concise and demo-focused, move detailed technical explanation to GitHub README/docs.

---

## 1. Goal

현재 Notion 포트폴리오 페이지는 README와 기술 문서의 내용이 많이 겹친다.
Notion은 면접관이 3~5분 안에 읽을 수 있는 포트폴리오 요약 페이지로 줄이고,
자세한 실행 방법, 아키텍처, ERD, 환경 변수, 커밋 근거는 GitHub README와 `docs/`에 남긴다.

Notion의 최우선 기준은 가독성과 흡입력이다.
복잡한 기술 설명을 많이 넣기보다, 첫 화면에서 "무슨 서비스인지", "무엇을 직접 판단하고 개선했는지",
"왜 이 사람이 괜찮은 개발자인지"가 빠르게 보여야 한다.

핵심 방향:

- Notion: 짧은 소개, 기능별 GIF, 핵심 문제 해결 사례, My Role, Limits & Next
- GitHub README: 상세 기술 스택, 실행 방법, DB/Redis/OpenAI 설정, ERD, 문서 링크
- docs: 설계/분석/리포트 근거 유지
- 첫 화면은 복잡하면 실패. 한 줄 소개 + 대표 GIF + 핵심 임팩트 3개만 먼저 보여준다.

---

## 1.1 Readability Rules

Notion 작성 시 다음 원칙을 지킨다.

- 첫 화면에서 긴 표, ERD, Mermaid, 커밋 상세를 노출하지 않는다.
- 한 섹션은 가능하면 5~8줄 이내로 유지한다.
- 문장은 짧게 쓴다. 한 문단에는 하나의 메시지만 둔다.
- 기술명 나열보다 "그래서 사용자가/운영자가 무엇을 얻는지"를 먼저 보여준다.
- 상세 근거는 본문에 풀어 쓰지 않고 GitHub README/docs 링크로 넘긴다.
- GIF 아래 설명은 "무엇을 보여주는지"와 "왜 중요한지"만 쓴다.
- 보안/성능 이야기는 깊게 설명하기보다 Before/After 카드처럼 보여준다.
- `TBD`, 긴 괄호 설명, 내부 작업 로그처럼 보이는 문장은 줄인다.

Hook 문장 후보:

- "RSS 뉴스를 수집해 개발 트렌드를 뽑고, 그 흐름을 일일 학습 콘텐츠로 연결한 서비스입니다."
- "기능 추가보다 먼저 보안, 캐시, 인덱스, 운영 흐름을 점검하며 서비스 품질을 끌어올렸습니다."
- "AI를 코드 생성 도구로만 쓰지 않고, 설계와 구현, 검증을 분리한 협업 프로세스로 운영했습니다."

---

## 2. Notion Target Structure

### 2.1 Hero

- 프로젝트 한 줄 소개
- 기술 태그는 5~6개만 유지
  - Java 21
  - Spring Boot
  - Oracle
  - Redis
  - OpenAI API
  - Thymeleaf SSR
- GitHub 링크
- Live Demo는 실제 링크가 준비되기 전까지 `TBD`를 노출하지 않거나 "준비 중"으로 짧게 처리

### 2.2 Demo GIF Sections

기능별로 GIF를 나누고, 각 GIF 아래 설명은 1~2줄만 둔다.

추천 GIF 목록:

1. Home Feed
   - 오늘의 지식, 기술 뉴스, 일일 개발 트렌드가 한 화면에 노출되는 흐름
   - 목적: 서비스가 무엇을 하는지 첫 화면에서 이해시키기

2. Insight Detail
   - 상세 조회, 좋아요, 북마크, 댓글/대댓글 작성
   - 목적: 사용자 참여 기능과 SSR 상세 화면을 보여주기

3. Admin Crawling
   - 조건 프리셋 선택, 수동 크롤링 실행, 실행 이력 확인
   - 목적: RSS 수집 관리 기능을 보여주기

4. AI Generation
   - 일일 지식 미리보기, 결과 수정, 저장
   - 목적: OpenAI 기반 콘텐츠 생성 워크플로우를 보여주기

5. Daily Trend Insight
   - 뉴스 기반 일일 트렌드 생성, 홈 노출, 지식 생성 프롬프트 연동
   - 목적: "RSS -> 트렌드 -> 지식 콘텐츠" 연결 가치를 보여주기

선택 GIF:

- Admin Stats / Posts
  - 관리자 통계, 게시물 관리, 페이지네이션
  - 포트폴리오 본문이 길어지면 스크린샷 1장으로 대체

### 2.3 Key Problem Solving

긴 설명 대신 3개 사례만 남긴다.

1. RSS Crawler Security
   - 문제: RSS 상세 URL이 외부 피드에 의해 결정되어 SSRF/XXE 위험 존재
   - 해결: 내부망 IP 차단, 수동 리다이렉트 검증, DOCTYPE/외부 엔티티 차단
   - 근거 링크: GitHub README 또는 `docs/02-design/features/crawling-security-hardening.md`

2. Comment Performance Decision
   - 문제: 댓글 트리 성능 저하 우려
   - 판단: 트리 조립은 이미 O(n), 실제 병목 후보는 DB 조회 인덱스
   - 해결: `(content_type, content_id, is_deleted, created_at)` 복합 인덱스 추가

3. AI Content Pipeline
   - 문제: 크롤링 뉴스와 일일 지식 생성이 분리되어 맥락 연결이 약함
   - 해결: 일일 트렌드 생성 후 Daily Knowledge 생성 프롬프트에 강제 반영
   - 근거 링크: `docs/01-plan/features/daily-trend-insight.md`, `docs/02-design/features/daily-trend-insight.md`

### 2.4 My Role

Notion에서는 역할을 짧게 정리한다.

- 요구사항과 우선순위 결정
- 회원가입 개방보다 서비스 품질 개선을 먼저 진행하도록 순서 조정
- 보안 개선 범위와 제외 범위 결정
- AI 협업 결과를 그대로 신뢰하지 않고 diff/test/doc 기준으로 검증
- 최종 반영 여부 승인

### 2.5 Limits & Next

한계는 유지하되 짧게 쓴다.

- 회원가입/비밀번호 재설정 미개방
- 부하 테스트와 Oracle 실행계획 검증 미실시
- DNS 리바인딩 잔여 위험
- 다중 인스턴스 환경의 분산 락 미적용
- LLM 타임아웃/재시도/프롬프트 인젝션 대응 보강 필요

---

## 3. README Role

README에는 상세 정보를 유지하거나 보강한다.

유지할 내용:

- 프로젝트 소개
- 주요 기능
- 기술 스택
- 아키텍처
- ERD
- 실행 방법
- 환경 변수
- DB 마이그레이션 순서
- 테스트 명령
- Known Issues & Next Steps

보강 후보:

- Notion에서 줄인 Deep Dive 내용을 README 또는 별도 docs로 링크
- 기능별 GIF가 만들어지면 README에는 대표 GIF 1~2개만 배치
- 나머지는 `docs/images/` 또는 별도 assets 경로로 관리

---

## 4. Work Plan

### Phase 1. Inventory

- 현재 Notion 본문에서 README와 중복되는 섹션 표시
- Notion에 남길 내용과 GitHub로 넘길 내용을 분류
- 기준일을 정한다
  - Option A: 2026-08-10 케이스 스터디 기준 유지
  - Option B: 현재 HEAD 기준으로 Daily Trend 기능까지 반영

### Phase 2. Demo Asset Capture

- 기능별 GIF 촬영 시나리오 작성
- 각 GIF 길이는 10~25초 권장
- 노션에는 압축된 GIF 또는 mp4를 사용
- GIF 파일명 예시:
  - `demo-home-feed.gif`
  - `demo-insight-detail.gif`
  - `demo-admin-crawling.gif`
  - `demo-ai-generation.gif`
  - `demo-daily-trend.gif`

### Phase 3. Notion Rewrite

- 긴 기술 표와 ERD는 제거하거나 접기 처리
- 본문은 "문제 -> 내가 한 판단 -> 결과" 중심으로 재작성
- 각 상세 근거는 GitHub README/docs 링크로 연결
- 테스트 수치는 "당시 커밋 메시지/문서 기준"임을 명시

### Phase 4. README Alignment

- Notion에서 제거한 기술 상세가 README 또는 docs에 있는지 확인
- README가 깨져 보이는 인코딩 문제를 별도 확인
- 최신 HEAD 기준이면 Daily Trend 기능과 프리셋 시드 내용을 README/Notion 양쪽에 맞춤

### Phase 5. Final Cleanup

- Notion에서 placeholder 제거
  - `Live Demo: TBD`
  - `[선택 GIF]`
  - 중복 스크린샷 설명
- 링크 동작 확인
- 이 임시 계획 파일 삭제

---

## 5. Completion Criteria

- Notion 본문이 3~5분 안에 읽히는 분량으로 축약됨
- 기능별 GIF 4개 이상 배치됨
- README와 Notion의 역할이 분리됨
- Notion의 모든 상세 기술 주장에 GitHub README/docs 링크가 연결됨
- `TBD` placeholder가 제거되거나 명확한 "준비 중" 표현으로 바뀜
- 이 파일을 삭제해도 작업 상태 추적에 문제가 없음

---

## 6. Delete Condition

다음이 완료되면 이 파일을 삭제한다.

- Notion 포트폴리오 페이지 축약 완료
- GIF/스크린샷 정리 완료
- README와 Notion 기준일 정합성 확인 완료
- GitHub 링크와 문서 링크 확인 완료
