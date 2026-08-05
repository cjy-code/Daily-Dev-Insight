# Unit 8: 인증/보안 체계 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 2 (화면 없음) / §2.4 리스크 기반 승격 1차 후보 (2차 사이클에서 정밀화 예정)

## ① 목적

일반 사용자/관리자 인증·인가 필터 체인, 사용자 조회·비밀번호 검증, 역할(Role) 기반 접근 제어를 담당하는 횡단 관심사. Unit 5(로그인 화면)·Unit 6(마이페이지)·Unit 17(관리자 로그인)이 모두 이 unit에 의존.

## ② 관련 파일

- `SecurityConfig` — 필터 체인 2개(`/admin/**` Order 1, 나머지 Order 2) 정의
- `CustomUserDetailsService` (`UserDetailsService` 구현)
- `UserService` (프로필/비밀번호/탈퇴 검증), `AuthService`(로그아웃 위임)
- `User` entity(`role`, `status` 필드로 권한/활성 여부 판단), `UserRepository`

## ③ 진입 엔드포인트

직접적인 엔드포인트 없음(필터 레벨 횡단 관심사). `POST /login`, `POST /admin/login`을 Spring Security `formLogin` 필터가 가로챔(Unit 5·17 참조).

## ④ 핵심 호출 흐름

1. 요청이 `/admin/**`이면 admin 필터 체인(Order 1, `hasRole("ADMIN")` 요구), 아니면 user 필터 체인(Order 2, `authenticated()` 요구) 적용 — 두 체인은 `securityMatcher`로 상호 배타적
2. 인증 시 `CustomUserDetailsService.loadUserByUsername()` → `UserRepository.findByUserId()`로 DB 조회 → `role` 필드를 `ROLE_{role}`로 변환, `status != ACTIVE`면 `UserDetails.disabled=true` (Spring Security가 자동으로 로그인 거부)
3. 로그인 성공 후 `processRoleBoundLoginSuccess()`가 **로그인 경로와 계정 role이 일치하는지 재검증** — 예: ADMIN 계정이 어떤 경로로든 `ROLE_USER`가 요구되는 곳에서 인증되면 즉시 강제 로그아웃
4. 비밀번호 검증은 `PasswordEncoder.matches()`로 위임되지만 **Bean이 `NoOpPasswordEncoder`** — 사실상 평문 문자열 비교
5. 탈퇴는 하드 삭제가 아니라 `User.status`를 `WITHDRAWN`으로 변경 → 이후 로그인 시도 시 `CustomUserDetailsService`가 `disabled=true`로 판단해 자동 차단됨 (탈퇴와 로그인 차단이 `status` 필드 하나로 자연스럽게 연결됨)

## ⑤ 데이터/외부 연동

- 외부 인증 연동(OAuth2, SSO 등) 없음. 자체 DB 기반 폼 로그인만 존재

## ⑥ 인증·트랜잭션·캐시

- **`NoOpPasswordEncoder` 사용 — 비밀번호가 DB에 평문 또는 평문과 동일하게 비교 가능한 형태로 저장/비교됨.** `docs/MVP_SCOPE.md` §4에 이미 보안 부채로 기재된 알려진 이슈
- `UserService` 클래스 전체 `readOnly`, 쓰기 메서드만 개별 `@Transactional`
- 캐시 없음 (인증 관련 데이터는 매 요청 DB 조회)

## ⑦ 화면 요약

없음 (직접 화면 없는 횡단 관심사 unit)

## ⑧ 패턴 특이사항

- **로그아웃 로직이 최소 2곳에 분산 구현**(Unit 5/6에서 발견 사항 재확인): `AuthController`→`AuthService.logout()`(공용 `SecurityContextLogoutHandler` 인스턴스 재사용) vs `MyPageController.processWithdraw()`(그 자리에서 `new SecurityContextLogoutHandler()` 직접 생성) — 동일 동작이 통일된 진입점 없이 중복
- 관리자 CSRF 실패 처리(`processAdminAccessDenied`)에 `/admin/posts/` 경로만 특별히 Referer 기반 리다이렉트 예외 처리가 있음 — 다른 관리자 하위 경로는 일반 CSRF 에러 페이지로만 이동, 왜 posts만 예외인지 코드 주석 없음
- 사용자 계정과 관리자 계정이 **같은 `User` 테이블·같은 로그인 메커니즘**을 `role` 값으로만 구분 — 별도 Admin 전용 테이블/인증 체계가 아님

## ⑨ 알아둘 점 / 리스크

- **`NoOpPasswordEncoder`는 프로덕션 배포 전 반드시 해결해야 할 최우선 보안 이슈** — 이미 알려진 이슈지만 이번 조사로 정확한 영향 범위(로그인·비밀번호변경·탈퇴 검증 전부)를 재확인함
- 로그아웃 중복 구현은 향후 로그아웃 정책 변경(예: 세션 무효화 방식 변경) 시 한쪽만 수정하고 다른 쪽을 놓칠 위험
- **§2.4 승격 후보로 지정됨** — 2차 사이클에서 Plan/Design/Analysis/Report로 정밀화 예정 (이유: 보안 관련 unit, 이미 확인된 이슈의 정확한 재현 조건과 개선 방안을 정밀 문서로 남길 필요)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) — 로그아웃 중복 구현 확정, WITHDRAWN 상태와 로그인 차단 연결 구조 확인 |
