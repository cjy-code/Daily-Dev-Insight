# 인증/보안 체계 Planning Document

> **Summary**: 사용자/관리자 인증·인가 필터 체인, 사용자 조회·비밀번호 검증, 역할(Role) 기반 접근 제어를 담당하는 횡단 관심사
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **Status**: **Implemented (사후 문서화, 2차 사이클 정밀화)** — 1차 사이클 compact card를 거쳐 정밀 문서로 승격됨 (SoR: 코드 우선)

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 사용자/관리자 경로를 분리 보호하고, 계정별 권한에 맞는 화면·API 접근만 허용해야 함 |
| **WHO** | 전체 인증 사용자(일반/관리자), Unit 5·6·17이 이 체계에 의존 |
| **RISK** | `NoOpPasswordEncoder` 사용 중(평문 비교), 로그아웃 로직 중복 구현 |
| **SUCCESS** | 사용자/관리자가 각자의 로그인 경로로만 인증 가능하고, 잘못된 역할로 로그인 시 자동 차단되며, 탈퇴 계정은 즉시 로그인 불가 |
| **SCOPE** | 인증/인가 필터체인, 사용자 조회, 비밀번호 검증/변경, 로그아웃. OAuth2/SSO 등 외부 인증 연동은 범위 밖(미구현) |

---

## 1. Overview

### 1.1 목적

`SecurityConfig`가 정의한 2개 필터체인(`/admin/**` vs 나머지)으로 사용자/관리자 경로를 분리 보호하고, `CustomUserDetailsService`가 DB 기반 인증을 제공한다.

### 1.2 배경

1차 사이클(compact card, 2026-08-05)에서 Unit 8로 가볍게 문서화했으나, §2.4 승격 기준(보안 관련 unit, 이미 알려진 `NoOpPasswordEncoder` 이슈)에 해당해 2차 사이클에서 정밀 문서로 승격됨.

### 1.3 관련 파일

| 레이어 | 파일 |
|--------|------|
| Config | `SecurityConfig` (필터체인 2개, `PasswordEncoder` Bean) |
| Service | `CustomUserDetailsService`(`UserDetailsService` 구현), `UserService`(프로필/비밀번호/탈퇴), `AuthService`(로그아웃) |
| Controller | `LoginController`(로그인 화면), `AuthController`(로그아웃) |
| Entity | `User`(`role`, `status` 필드) |
| Test | `SecurityConfigLoginFlowTest`(4건), `AuthControllerTest`(2건) |

---

## 2. Scope

### 2.1 In Scope

- [x] 사용자/관리자 필터체인 분리(`/admin/**` vs 나머지)
- [x] 역할(Role) 불일치 시 로그인 성공 후 강제 로그아웃
- [x] 탈퇴(`WITHDRAWN`) 계정 로그인 차단
- [x] 비밀번호 변경/탈퇴 시 현재 비밀번호 검증
- [x] 관리자 CSRF 실패와 권한 부족 구분 처리

### 2.2 Out of Scope

- 외부 인증 연동(OAuth2/SSO) — 미구현
- `NoOpPasswordEncoder`를 실제 암호화 인코더로 교체하는 작업 — Known Gap으로만 기록, 본 사이클에서 직접 수정하지 않음(§2.3 원칙)
- 로그아웃 로직 통합 리팩터링 — Known Gap으로만 기록

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | `/admin/**` 경로는 `ROLE_ADMIN`만 접근 가능하다 | High | Done |
| FR-02 | 사용자 로그인 폼으로 ADMIN 계정이 인증되면 강제 로그아웃 후 에러 처리된다 | High | Done |
| FR-03 | 관리자 로그인 폼으로 USER 계정이 인증되면 강제 로그아웃 후 에러 처리된다 | High | Done |
| FR-04 | `status != ACTIVE`인 계정은 로그인이 거부된다 | High | Done |
| FR-05 | 비밀번호 변경/탈퇴 시 현재 비밀번호가 일치해야 진행된다 | High | Done |
| FR-06 | 관리자 CSRF 실패 시 일반 권한부족과 다른 경로로 안내된다 | Medium | Done |

### 3.2 Non-Functional Requirements

| Category | Criteria |
|----------|----------|
| Security | 비밀번호는 안전하게 저장/비교되어야 함 | ❌ **미충족** — `NoOpPasswordEncoder` 사용 중 |
| Consistency | 로그아웃 로직은 단일 진입점을 통해야 함 | ❌ **미충족** — 최소 2곳에 중복 구현(findings F-02) |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 필터체인 분리 및 역할 재검증 로직이 코드로 구현되어 있고 Design 문서로 설명 가능
- [x] 테스트 6건(로그인 흐름 4 + 로그아웃 2)이 코드 정독 기준 요구사항을 커버함을 확인
- [ ] `NoOpPasswordEncoder` 교체 — **미완료, 별도 개선 작업 필요**
- [ ] 로그아웃 로직 단일화 — **미완료, 별도 개선 작업 필요**

### 4.2 Quality Criteria

- [x] 필터체인이 관리자/사용자 경로를 명확히 분리
- [ ] 예외 처리·로그아웃 로직의 프로젝트 전역 일관성(findings F-02, F-03과 연관) — 별도 과제

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| `NoOpPasswordEncoder`로 인한 평문 비밀번호 노출 위험 | High | High(현재 상태) | 프로덕션 배포 전 반드시 실제 인코더(BCrypt 등)로 교체 필요 — 마이그레이션(기존 평문 비밀번호 재해시) 전략 수립 필요 |
| 로그아웃 로직 중복으로 인한 정책 변경 시 누락 | Medium | Medium | `AuthService`로 단일화 검토 |
| 관리자 CSRF 예외 처리가 `/admin/posts/`에만 특별 대응되어 다른 경로는 일관성 없음 | Low | Low | 필요 시 전체 경로로 일반화 검토 |

---

## 6. Next Steps

1. [ ] Design 문서(`docs/02-design/features/auth-security.md`) 정밀 확장
2. [ ] Gap 분석(`docs/03-analysis/auth-security.md`)
3. [ ] 완료 보고서(`docs/04-report/auth-security.md`)
4. [ ] Codex 교차검증
5. [ ] `NoOpPasswordEncoder` 교체는 별도 보안 개선 Plan으로 분리 제안

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | 2차 사이클 정밀 기획 문서 초안 작성 | Claude (대화 기반) |
