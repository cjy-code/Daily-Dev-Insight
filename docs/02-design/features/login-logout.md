# Unit 5: 로그인/로그아웃 (사용자) — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 1 / 인증 아키텍처 전체(필터 체인 구성 등)는 Unit 8(인증/보안 체계, §2.4 승격 후보)에서 정밀 다룸. 여기서는 사용자 로그인/로그아웃 화면·흐름만 기록

## ① 목적

일반 사용자의 로그인 화면 렌더링, 로그인 성공/실패 처리(Spring Security 위임), 로그아웃 처리.

## ② 관련 파일

- `LoginController.login()` — `GET /login`, 화면 렌더링 + 쿼리파라미터(error/logout/withdraw)를 flash 메시지로 변환
- `AuthController.logout()` — `POST /auth/logout`
- `AuthService.logout()` — `SecurityContextLogoutHandler` 위임
- `SecurityConfig.userSecurityFilterChain()` — 실제 인증 처리(`POST /login`은 이 컨트롤러들이 아니라 Spring Security의 `formLogin` 필터가 가로챔)
- `templates/views/login.html` (실사용), `templates/login.html`(미사용 추정 — 이번에 재확인 결과 `LoginController`가 `"views/login"`을 반환하는 것으로 코드 확인됨 → **미사용 확정**)

## ③ 진입 엔드포인트

| Method | Path | 처리 주체 |
|---|---|---|
| GET | `/login` | `LoginController` (화면 렌더링) |
| POST | `/login` | **컨트롤러 코드 없음** — Spring Security `formLogin`의 `loginProcessingUrl`이 가로채 인증 처리 |
| POST | `/auth/logout` | `AuthController` |

## ④ 핵심 호출 흐름

1. `GET /login`: `error`/`logout`/`withdraw` 쿼리 파라미터가 있으면 flash attribute로 변환 후 `/login`으로 redirect(PRG 패턴), 없으면 `views/login` 뷰 직접 렌더링
2. `POST /login`: 이 프로젝트 코드가 아니라 `SecurityConfig`가 등록한 Spring Security 필터가 인증 수행 → 성공 시 `processRoleBoundLoginSuccess`가 **로그인한 계정의 권한이 `ROLE_USER`인지 재확인** — ADMIN 계정이 사용자 로그인 폼으로 로그인 성공해도 권한이 안 맞으면 강제 로그아웃 후 `/login?error`로 리다이렉트
3. 로그아웃: `POST /auth/logout` → `AuthService.logout()`이 세션/인증 컨텍스트 클리어 → 로그인했던 계정의 role에 따라 `/login?logout` 또는 `/admin/login?logout`로 분기 리다이렉트

## ⑤ 데이터/외부 연동

- 외부 연동 없음. 인증은 `UserDetailsService` 구현체(Unit 8에서 확인 예정)가 DB 사용자 조회

## ⑥ 인증·트랜잭션·캐시

- **비밀번호 인코더가 `NoOpPasswordEncoder`(평문 비교)** — `SecurityConfig`에 명시적으로 Bean 등록되어 있음. 이미 알려진 보안 부채(`docs/MVP_SCOPE.md` §4에 기재)
- 트랜잭션/캐시 해당 없음 (이 unit 자체는 DB 쓰기 없음, 인증은 Security 필터 레벨)

## ⑦ 화면 요약

- 로그인 폼(아이디/비밀번호), 로그인 실패/로그아웃 완료/탈퇴 완료 상태별 안내 메시지(flash attribute 기반)

## ⑧ 패턴 특이사항

- **로그아웃 처리 위치가 마이페이지 탈퇴와 다름**: 일반 로그아웃은 `AuthController` → `AuthService`로 위임되는데, 회원 탈퇴 시 로그아웃은 `MyPageController`(Unit 6)가 자체적으로 처리하는 것으로 Codex 검증에서 확인됨 — 로그아웃 로직이 한 곳에 통일되어 있지 않음 (Unit 6 작성 시 재확인 필요)
- 로그인 성공 후 **역할(role) 재검증**이 컨트롤러가 아니라 `SecurityConfig`의 `successHandler` 안에 있음 — 이 프로젝트에서 인증 관련 분기 로직이 컨트롤러보다 Security 설정 레이어에 몰려있는 패턴

## ⑨ 알아둘 점 / 리스크

- `NoOpPasswordEncoder` 사용 중 — 프로덕션 배포 전 반드시 해결해야 할 보안 이슈 (Unit 8에서 정밀 문서화 시 우선순위 재확인)
- `templates/login.html`은 이번 조사로 **미사용이 최종 확인됨** → `full-documentation-initiative.md` §2.3 Out of Scope의 "추정" 표기를 "확정"으로 갱신 필요

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) — `templates/login.html` 미사용 확정 |
