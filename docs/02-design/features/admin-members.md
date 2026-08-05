# Unit 14: 회원 관리 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3

## ① 목적

관리자가 회원 목록을 조회하고 개별 회원의 역할(role)·상태(status)를 직접 수정.

## ② 관련 파일

- `AdminPageController.membersPage()` / `updateMember()`
- `AdminManagementService.findRecentUsers()` / `updateUser()`
- `templates/admin/members.html`

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/admin/members` | 회원 목록 화면 |
| POST | `/admin/members/{id}/update` | 특정 회원의 `role`/`status` 수정 |

## ④ 핵심 호출 흐름

목록은 `findRecentUsers()`로 조회(정렬/개수 제한 기준은 정밀화 시 확인). 수정은 `role`/`status` 두 필드만 폼으로 받아 `updateUser(id, role, status)` 호출 — **관리자가 이 화면에서 다른 회원을 강제로 `WITHDRAWN` 처리하거나 `ROLE_ADMIN`으로 승격시키는 것도 가능**(입력값 검증 범위는 `AdminManagementService.updateUser()` 확인 필요, 이번 조사 범위 밖).

## ⑤ 데이터/외부 연동

`User` 엔티티의 `role`/`status` 직접 갱신. Unit 8(인증)의 `CustomUserDetailsService`가 이 값을 그대로 읽어 인가에 사용하므로, **여기서 값을 바꾸면 즉시(다음 로그인부터) 권한에 반영됨**.

## ⑥ 인증·트랜잭션·캐시

- 인증: 관리자 권한 필요
- 예외 처리: `AdminPageController`의 다른 관리 화면들과 동일하게 광범위 `catch (Exception)` → `adminError` flash 패턴 (Unit 2/6/8에서 확인된 "여러 예외 처리 스타일" 중 이 스타일이 Admin 영역 전반의 표준)
- 캐시 없음

## ⑦ 화면 요약

회원 목록 테이블(역할/상태 표시) + 인라인 또는 별도 폼으로 role/status 수정

## ⑧ 패턴 특이사항

- role/status를 자유 문자열로 폼 입력받는 것으로 보이는데, `CustomUserDetailsService`는 `role` 값을 그대로 `"ROLE_" + role`로 변환해 사용 — **관리자가 오타나 임의 문자열을 넣으면 존재하지 않는 권한 문자열이 그대로 생성될 수 있음**(서버 측 enum 검증 여부는 `updateUser()` 확인 필요)

## ⑨ 알아둘 점 / 리스크

- 이 화면 자체가 **가장 민감한 관리자 기능 중 하나**(임의 계정을 관리자로 승격 가능)인데도 §2.4 승격 기준(인증/보안 관련)에 넣지 않은 이유는 화면/로직이 단순(엔드포인트 2개)해서였음 — 다만 실제로 손댈 일이 생기면 입력값 검증 로직은 반드시 먼저 확인 필요

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
