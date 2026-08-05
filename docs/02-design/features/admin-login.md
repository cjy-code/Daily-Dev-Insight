# Unit 17: 관리자 로그인 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3 / **`AdminPageController` 소속이 아니라 `LoginController`·`SecurityConfig` 소속 — Codex 1차 검증에서 이미 정정된 분류(§2.2 표 참조)**

## ① 목적

관리자 전용 로그인 화면과 인증 성공/실패/거부 처리. Unit 5(사용자 로그인)와 화면·컨트롤러는 분리되어 있지만 메커니즘은 Unit 8(인증/보안 체계)을 그대로 공유.

## ② 관련 파일

- `LoginController.adminLogin()` — `GET /admin/login`
- `SecurityConfig.adminSecurityFilterChain()` — `POST /admin/login` 실제 처리(Order 1, `/admin/**` 전용 필터체인)
- `templates/views/admin-login.html`

## ③ 진입 엔드포인트

| Method | Path | 처리 주체 |
|---|---|---|
| GET | `/admin/login` | `LoginController` |
| POST | `/admin/login` | Spring Security `formLogin`(컨트롤러 코드 없음, Unit 5와 동일 패턴) |

## ④ 핵심 호출 흐름

1. `GET /admin/login`: `error`/`logout`/`adminDenied`/`csrfError` 4가지 쿼리 파라미터를 각각 다른 flash 메시지로 변환 후 redirect(PRG) — **Unit 5(사용자)는 3가지(error/logout/withdraw)뿐인데 관리자는 인가거부·CSRF 실패까지 별도 케이스가 추가되어 있음**
2. `POST /admin/login`: admin 전용 필터체인이 인증 → `processRoleBoundLoginSuccess(..., ROLE_ADMIN, ...)`로 `ROLE_ADMIN` 아니면 강제 로그아웃 후 `?error`
3. 관리자 경로에서의 접근 거부(`AccessDeniedException`)는 `processAdminAccessDenied()`가 CSRF 실패와 단순 권한부족을 구분해서 각각 다른 쿼리파라미터로 리다이렉트 (Unit 8 §④ 참조)

## ⑤ 데이터/외부 연동

Unit 8과 완전히 동일 (`CustomUserDetailsService`, `User.role`/`status`)

## ⑥ 인증·트랜잭션·캐시

Unit 8 참조. 이 unit 고유 사항 없음.

## ⑦ 화면 요약

- 관리자 로그인 폼 + 4종 상태 메시지(로그인실패/로그아웃완료/권한거부/CSRF실패)

## ⑧ 패턴 특이사항

- 사용자 로그인(Unit 5)과 관리자 로그인이 **같은 메커니즘, 다른 화면·다른 필터체인**으로 완전히 병렬 구현되어 있음 — 공통 로직(에러 메시지 변환 등)이 `LoginController` 안에서 메서드만 분리된 채 유사 코드 반복
- CSRF 실패 시 `/admin/posts/` 경로만 예외적으로 Referer 기반 리다이렉트(Unit 8에서 이미 지적) — 관리자 로그인 자체보다 게시물 관리 화면(Unit 15)에서의 CSRF 실패를 더 세심하게 처리하는 비대칭

## ⑨ 알아둘 점 / 리스크

- Unit 8이 정밀화되면 이 unit은 그 결과를 그대로 상속받는 게 효율적 — 별도 승격 불필요(§2.4 기준에 해당 안 함, Unit 8의 부분집합에 가까움)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
