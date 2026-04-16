# Daily Dev Insight

Daily Dev Insight는 **Spring Boot + Thymeleaf 기반 개발 인사이트 서비스**입니다.
`develop` 브랜치 기준으로 사용자 화면, 관리자 화면, 인사이트 상호작용(좋아요/북마크/댓글), 관리자 생성 관리 기능을 포함합니다.

## 1. 기술 스택

- Backend: Java 21, Spring Boot 3.2, Spring MVC, Spring Security, Spring Data JPA
- View: Thymeleaf, Vanilla JS, CSS
- DB: Oracle (ojdbc11)
- Cache: Redis (Spring Cache)
- Build: Gradle

## 2. 주요 기능

- 사용자 인사이트 화면
- 일자/기간 기반 인사이트 조회 및 검색
- 인사이트 상세 상호작용
- 좋아요/북마크 토글
- 댓글/대댓글 작성 및 삭제
- 사용자/관리자 로그인 분리
- 관리자 페이지
- 대시보드, 게시글 관리, 회원 관리
- 프롬프트 템플릿 관리
- 수동 생성 실행
- 예약 생성 설정 및 실행 이력 확인

## 3. 실행 환경

- JDK 21
- Oracle DB
- Redis
- (선택) Docker: Oracle 컨테이너 실행

## 4. 빠른 시작

### 4.1 Oracle 실행 (선택: Docker)

프로젝트 루트에서:

```powershell
docker compose up -d
```

기본 포트는 `1521`입니다.

### 4.2 DB 스키마 반영

아래 SQL 파일을 Oracle에서 순서대로 반영하세요.

1. `docs/sql/2026-04-13_insight_engagement_mvp_oracle.sql`
2. `docs/sql/2026-04-15_insight_comment_reply_migration_oracle.sql`
3. `docs/sql/2026-04-15_admin_generation_tables_oracle.sql`
4. `docs/sql/2026-04-15_users_user_id_migration_oracle.sql`
5. `docs/sql/2026-04-15_daily_knowledge_attachment_seed_oracle.sql`
6. `docs/sql/2026-04-15_tech_news_attachment_seed_oracle.sql`

### 4.3 환경 변수

`backend/src/main/resources/application.yml` 기준 주요 값:

- `DB_URL` (기본: `jdbc:oracle:thin:@//localhost:1521/ORCLPDB`)
- `DB_USERNAME` (기본: `daily`)
- `DB_PASSWORD` (기본: `1234`)
- `DB_DRIVER` (기본: `oracle.jdbc.OracleDriver`)
- `REDIS_HOST` (기본: `127.0.0.1`)
- `REDIS_PORT` (기본: `6379`)
- `REDIS_TIMEOUT` (기본: `3s`)
- `JPA_DDL_AUTO` (기본: `none`)

### 4.4 애플리케이션 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

기본 실행 주소: `http://localhost:9090`

## 5. 기본 계정 (마이그레이션 스크립트 기준)

`docs/sql/2026-04-15_users_user_id_migration_oracle.sql`에서 아래 계정을 시드합니다.

- 사용자: `user01` / `1234`
- 관리자: `admin01` / `1234`

현재 `SecurityConfig`는 `NoOpPasswordEncoder`를 사용합니다. 운영 환경 전환 시 반드시 암호화 인코더로 교체하세요.

## 6. 주요 URL

- 사용자 로그인: `/login`
- 관리자 로그인: `/admin/login`
- 메인: `/`
- 인사이트 상세: `/insights/{type}/{id}`
- 관리자 대시보드: `/admin/dashboard`
- 관리자 생성 관리: `/admin/generation`

## 7. 주요 API

- `GET /api/insights?date=yyyy-MM-dd`
- `GET /api/insights/{type}/{id}`
- `POST /api/insights/{type}/{id}/likes/toggle`
- `POST /api/insights/{type}/{id}/bookmarks/toggle`
- `POST /api/insights/{type}/{id}/comments`
- `DELETE /api/insights/{type}/{id}/comments/{commentId}`

상세 스펙은 `docs/API_SPEC.md`를 참고하세요.

## 8. 문서

- MVP 범위: `docs/MVP_SCOPE.md`
- API 스펙: `docs/API_SPEC.md`
- SQL 스크립트: `docs/sql/`

## 9. 테스트

```powershell
cd backend
.\gradlew.bat test
```
