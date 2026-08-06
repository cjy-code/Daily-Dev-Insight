# Daily Dev Insight 범위 정의

> 이 문서는 프로젝트 전체 범위를 반영하는 살아있는(living) 문서다. 최초 MVP(5개 기능)는 이미 완료되어 훨씬 넓은 범위로 확장되었으며, 아래 "2. 현재 구현 범위"가 실제 코드베이스 기준 현재 상태다.
>
> **최종 갱신**: 2026-08-03 (프로젝트 스캔 기반)

## 1. 목적

- 코드베이스(SoR 1순위)를 기준으로 현재 서비스 범위를 정확히 문서화한다.
- 이후 신규 기능은 이 문서를 기준선으로 삼아 `docs/01-plan/features/`에 개별 Plan 문서로 추가한다.

## 2. 현재 구현 범위 (Current Scope)

### 2.1 인증 사용자 화면 (로그인 필요)

> 로그인·정적 리소스를 제외한 모든 사용자 경로·API는 인증이 필요하다 (완전한 공개 화면 아님).

| 기능 | 설명 |
|------|------|
| 오늘의 지식/뉴스 홈 | 메인 화면에서 대표 지식 1건 + 뉴스 리스트 + 주간 TOP 리스트 노출. 기본 3개월 범위 조회, 제목/내용 키워드 검색(지식 콘텐츠에만 적용) 지원 |
| 날짜별 조회 API | `GET /api/insights?date=yyyy-MM-dd` — 단일 날짜 기준 조회 (파라미터는 `date` 하나뿐, 기간 조회 미지원). 지식은 전체 기간 조회·검색 가능하지만, 뉴스는 종료일 기준 최근 2일(6건 미만이면 3일까지)만 조회됨 — 지식/뉴스 동작이 동일하지 않음 |
| 인사이트 상세 | `/insights/{type}/{id}` — 지식/뉴스 상세 페이지, 조회수 반영 |
| 주간 AI 인사이트 | 최근 7일 뉴스를 AI가 요약한 주간 트렌드 콘텐츠 (관리자 수동 생성 + 노출 토글). 사용자 화면에는 최신 공개 항목만 홈에 노출되며 목록/과거 이력/별도 상세 API는 없음 |

> 주간 TOP10/TOP5는 뉴스가 아니라 해당 월~일 구간의 **지식 콘텐츠를 조회수순으로 집계**한 것이다.

### 2.2 사용자 참여 기능

| 기능 | 설명 |
|------|------|
| 좋아요 | 지식/뉴스 게시물 좋아요 토글. 해제 시 엔티티가 삭제되어 과거 이력은 보존되지 않고 현재 유지 중인 항목만 조회 가능 |
| 북마크 | 지식/뉴스 게시물 북마크 토글. 좋아요와 동일하게 이력 미보존 |
| 댓글/대댓글 | 게시물 댓글 작성, 답글(대댓글) 지원, 본인 댓글 삭제(물리 삭제가 아닌 삭제 상태 처리) — 댓글 수정은 미구현 |

### 2.3 계정 / 마이페이지

| 기능 | 설명 |
|------|------|
| 로그인/로그아웃 | 사용자 로그인, 관리자 별도 로그인(`/admin/login`) |
| 프로필 조회/수정 | `/mypage`, `/mypage/profile` |
| 비밀번호 변경 | `/mypage/password` |
| 활동 내역 | 현재 유지 중인 좋아요/북마크 최근 30건 조회 (`/mypage/activity`) — 과거 이력 아님 |
| 회원 탈퇴 | `/mypage/withdraw` |

### 2.4 관리자 콘솔 (`/admin/**`)

| 영역 | 설명 |
|------|------|
| 대시보드/통계 | 조회수·북마크 상세 통계, 전체/활성 회원 수, 지식 게시물 수·당일 게시물 수, AI 생성 성공/실패 통계 |
| 게시물 관리 | 지식/뉴스 **최근 30건** 목록 조회·수정·삭제, 썸네일 업로드/삭제 (지식은 제목/카테고리만, 뉴스는 제목/출처만 수정 가능 — 전체 목록·전체 필드 수정 아님) |
| 회원 관리 | 회원 **최근 30건** 목록, 권한/상태 변경 |
| 프롬프트 템플릿 관리 | LLM 생성용 프롬프트 CRUD 및 활성화 |
| 수동 AI 콘텐츠 생성 | 생성 작업 화면(`generation-compose.html`), LLM 미리보기, 이미지 재생성 미리보기, 저장 |
| 생성 예약 설정 | **단일 예약 설정** 등록/갱신(복수 스케줄 등록 아님), 생성 이력 조회 |
| 크롤링 관리 | 수동 크롤링 실행(저장하지 않는 미리보기 포함), 단일 예약 설정, 키워드/도메인 프리셋, 크롤링 이력 |
| 주간 AI 인사이트 관리 | 수동 생성, 노출 여부 토글 |

### 2.5 자동화 (백그라운드 스케줄러)

| 작업 | 주기 | 설명 |
|------|------|------|
| `ScheduledCrawlingExecutor` | 매분 (`0 * * * * *`) | DB에 저장된 크롤링 스케줄 조건 확인 후 실행 |
| `ScheduledGenerationExecutor` | 매분 (`0 * * * * *`) | DB에 저장된 AI 생성 스케줄 조건 확인 후 실행 |

### 2.6 AI 생성 파이프라인

| 컴포넌트 | 설명 |
|----------|------|
| `NewsCrawlerClient` (RSS 구현체) | 외부 뉴스 크롤링 |
| `LlmGenerationClient` (OpenAI/Mock 구현체) | 지식 콘텐츠 텍스트 생성 |
| `ImageGenerationClient` (OpenAI 구현체) | 지식 콘텐츠 썸네일 이미지 생성 (`uploads/knowledge/{date}`) |
| `NewsThumbnailStorageService` | 뉴스 원본 썸네일 다운로드/저장 (`uploads/news/{date}`) |
| `WeeklyAiInsightService` | 7일치 뉴스 기반 주간 트렌드 요약 생성 |

### 2.7 인프라/설정

- `SecurityConfig` — `/admin/**`과 사용자 경로 분리된 Security Filter Chain, 권한 체크
- `RedisCacheConfig` — 캐시별 TTL 정책
- `OracleSchemaMigrationRunner` — 앱 기동 시 Oracle 스키마 자동 마이그레이션
- `WebResourceConfig` — `/uploads/**` 정적 리소스 매핑

## 3. 현재 범위 밖 (Not Yet Implemented)

- 추천 기능
- 알림 기능
- 회원가입/계정 생성
- 비밀번호 찾기·재설정
- 댓글 수정 (삭제만 가능)
- 주간 AI 인사이트 자동 예약 생성
- 사용자용 주간 AI 인사이트 목록/과거 내역
- (그 외 개별 기능은 `docs/01-plan/features/`에서 신규 Plan으로 제안)

## 4. 문서 부채 (Known Gaps)

- ~~`docs/02-design/`가 비어 있어...`WeeklyAiInsightService`가 최우선~~ **[2026-08-06 갱신] 해소됨** — `docs/01-plan/features/full-documentation-initiative.md`에 따라 전체 17개 unit(참여 기능·계정/마이페이지·인증·관리자·자동화·AI 생성·캐시·DB마이그레이션 포함)을 1차 사이클(compact card, `docs/02-design/features/*.md`)로 전수 문서화 완료. 그중 위험도 높은 3개 unit(인사이트 상세·인증보안체계·AI생성관리)은 Plan+Design+Screen+Analysis+Report 정밀 문서로 2차 승격, Codex 교차검증 반영 완료. `WeeklyAiInsightService`는 별도로 PDCA 전체 사이클(Plan~Report) 완료
- ~~`docs/01-plan/features/`에는...Plan 문서도 실제 구현 기능과 대응하지 않음~~ **[2026-08-06 갱신] 해소됨** — `weekly-ai-insight.md`, `insight-detail.md`, `auth-security.md`, `admin-ai-generation.md`, `full-documentation-initiative.md` 등 실제 구현 기능에 대응하는 Plan 문서 다수 추가됨
- 문서화 과정에서 발견된 코드 이슈 26건이 `docs/03-analysis/full-documentation-initiative-code-findings.md`에 백로그로 남아있음 (그중 1건은 사용자 결정 후 수정 완료 — 관리자 세션의 사용자 경로 접근 차단)
- `docs/API_SPEC.md`는 API 누락뿐 아니라 기존 `GET /api/insights` 계약 자체가 실제 구현과 다름 (뉴스는 정확한 하루가 아닌 최근 2~3일 조회, 응답 필드 확장, 인증 요구·미래 날짜 보정 미기재) — 상세/좋아요/북마크/댓글/관리자 API도 전부 누락
- 인증/권한 매트릭스 문서 부재. **`SecurityConfig`에서 `NoOpPasswordEncoder` 사용 중 — 문서 문제가 아니라 별도 관리가 필요한 보안 부채**
- DB 변경 기준선이 `docs/sql`과 `OracleSchemaMigrationRunner` 코드 마이그레이션으로 분산됨. 예: `weekly_ai_insight` 테이블은 코드 마이그레이션에만 존재하고 대응 SQL 문서가 없음
- 운영 설정 문서 부재: Redis 캐시/TTL 정책, `llm.provider=mock|openai` 전환, OpenAI 키/모델 설정, `uploads` 로컬 영속성, 스케줄러 실행 정책·타임존
- `AGENTS.md`가 요구하는 `docs/.pdca-status.json`이 존재하지 않음 (PDCA 추적 지속 시 해소 필요)

## 5. 산출물 정의

- 본 문서(`docs/MVP_SCOPE.md`)를 제품 범위의 단일 기준 문서로 사용한다.
- 기능별 상세는 `docs/01-plan/features/{feature}.md` (Plan) → `docs/02-design/features/{feature}.md` (Design)로 분리 관리한다.
- API 상세는 `docs/API_SPEC.md`.

---

## 부록: 최초 MVP (V1, 완료)

> 아래는 최초 릴리즈 범위였으며 현재 모두 구현 완료되어 위 "2. 현재 구현 범위"에 흡수되었다. 이력 참고용으로 보존한다.

### V1 목적
- 초기 릴리즈에서 범위 과다를 방지한다.
- 핵심 사용자 가치인 "오늘의 지식/뉴스 확인"과 "날짜별 조회"만 제공한다.

### V1 포함 기능 (5개)
1. 오늘의 지식 1개 조회
2. 오늘의 뉴스 리스트 조회
3. 날짜별 조회
4. 기본 조회 API 제공 (`GET /api/insights?date=yyyy-MM-dd`)
5. 최소 오류 처리 및 빈 상태 처리

### V1 수용 기준 (달성됨)
- 오늘 접속 시 대표 지식 1개와 뉴스 리스트를 볼 수 있다.
- 날짜를 바꿔 해당 날짜 데이터로 조회할 수 있다.
- 데이터가 없는 날짜에서도 오류 없이 빈 상태 안내를 볼 수 있다.
