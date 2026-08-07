# Unit 14: 회원 관리 — Compact Card

> **Project**: dailyDevInsight | **Date**: 2026-08-05 | **문서 유형**: 1차 Compact Card
> Phase 3

## ① 목적

관리자가 회원 목록을 조회하고 개별 회원의 역할(role)·상태(status)를 직접 수정.

## ② 관련 파일

- `AdminPageController.membersPage()` / `updateMember()`
- `AdminManagementService.findUsers(page)` / `updateUser()`
- `templates/admin/members.html`

## ③ 진입 엔드포인트

| Method | Path | 용도 |
|---|---|---|
| GET | `/admin/members` | 회원 목록 화면 |
| POST | `/admin/members/{id}/update` | 특정 회원의 `role`/`status` 수정 |

## ④ 핵심 호출 흐름

목록은 `findUsers(page)`로 페이지 단위 조회(**[2026-08-06 갱신, P1-2]** 기존 `findRecentUsers()`의 "최근 30건 고정" → 페이지당 20건 페이지네이션 전환, `createdAt` 내림차순 정렬 유지). 수정은 `role`/`status` 두 필드만 폼으로 받아 `updateUser(id, role, status)` 호출 — **관리자가 이 화면에서 다른 회원을 강제로 `WITHDRAWN` 처리하거나 `ROLE_ADMIN`으로 승격시키는 것도 가능**(입력값은 `normalizeRole`/`normalizeStatus`로 USER/ADMIN, ACTIVE/INACTIVE만 허용하도록 이미 검증되고 있음이 코드로 확인됨).

### 처리 흐름도

```mermaid
flowchart TD
    A["GET /admin/members?page=N"] --> B["findUsers(page) 페이지 단위 조회(20건)"]
    B --> C["admin/members.html 렌더링"]

    D["POST /admin/members/{id}/update\n(role, status 폼 입력)"] --> E["AdminManagementService.updateUser(id, role, status)\nnormalizeRole/normalizeStatus로 검증"]
    E -->|성공| F["adminMessage flash + redirect"]
    E -->|실패(Exception)| G["adminError flash + redirect\n(Admin 표준 예외처리 패턴)"]
    F --> C
    G --> C

    E -.검증된 role만 저장.-> H["CustomUserDetailsService가 다음 로그인 시\nROLE_+role 권한 부여(Unit 8 연동)"]
```

## ⑤ 데이터/외부 연동

`User` 엔티티의 `role`/`status` 직접 갱신. Unit 8(인증)의 `CustomUserDetailsService`가 이 값을 그대로 읽어 인가에 사용하므로, **여기서 값을 바꾸면 즉시(다음 로그인부터) 권한에 반영됨**.

## ⑥ 인증·트랜잭션·캐시

- 인증: 관리자 권한 필요
- 예외 처리: `AdminPageController`의 다른 관리 화면들과 동일하게 광범위 `catch (Exception)` → `adminError` flash 패턴 (Unit 2/6/8에서 확인된 "여러 예외 처리 스타일" 중 이 스타일이 Admin 영역 전반의 표준)
- 캐시: 회원 목록 자체는 캐시 없음. **[2026-08-06 갱신]** `updateUser()`는 Phase 1(P1-1)에서 `adminStats`(TTL 5분) `@CacheEvict` 추가됨(activeUsers 지표 갱신 목적)
- 목록 조회: **[2026-08-06 갱신, P1-2]** "최근 30건 고정" → 페이지네이션(페이지당 20건) 전환

## ⑦ 화면 요약

회원 목록 테이블(역할/상태 표시) + 인라인 또는 별도 폼으로 role/status 수정 + 페이지네이션(이전/다음)

## ⑧ 패턴 특이사항

- **[2026-08-06 확인]** role/status는 폼으로 자유 문자열을 받지만, `AdminManagementService.updateUser()` 내부의 `normalizeRole()`/`normalizeStatus()`가 각각 `USER`/`ADMIN`, `ACTIVE`/`INACTIVE`만 허용하고 그 외 값은 `IllegalArgumentException`을 던지는 것으로 코드 확인됨 — 이전 문서의 "서버 측 enum 검증 미확인" 우려는 실제로는 이미 해결되어 있었음(문서-코드 불일치 정정)

## ⑨ 알아둘 점 / 리스크

- 이 화면 자체가 **가장 민감한 관리자 기능 중 하나**(임의 계정을 관리자로 승격 가능)인데도 §2.4 승격 기준(인증/보안 관련)에 넣지 않은 이유는 화면/로직이 단순(엔드포인트 2개)해서였음 — role/status 값 자체는 검증되지만, 어떤 관리자 계정이든 다른 계정을 ADMIN으로 승격 가능하다는 권한 설계 자체의 위험은 별개 이슈로 남아있음(Phase 1 범위 밖)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 (1차 사이클 compact card) |
| 0.2 | 2026-08-06 | 처리 흐름도(Mermaid) 추가 |
| 0.3 | 2026-08-06 | Phase 1(P1-1, P1-2) 반영 — 회원 목록 페이지네이션(20건) 전환, `updateUser()` admin 통계 캐시 evict 추가, role/status 서버 검증 기존 존재 사실 확인·정정 |
