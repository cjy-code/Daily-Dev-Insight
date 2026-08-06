# 인증/보안 체계 Gap Analysis

> **Project**: dailyDevInsight
> **Date**: 2026-08-06 (v0.2: Codex 검증 반영 재작성)
> **대상 문서**: `docs/01-plan/features/auth-security.md`, `docs/02-design/features/auth-security.md`
> **대상 코드**: `SecurityConfig`, `CustomUserDetailsService`, `UserService`, `AuthService`, `LoginController`, `AuthController`, `MyPageController`(processWithdraw)

---

## 검증 범위 선언 (Audit Discipline)

- **검증한 것**: 코드 정독 + `codex exec -s read-only` 2차 교차검증
- **검증하지 않은 것**: `./gradlew test` 실행, 실제 브라우저/세션 동작

> **v0.1 정정 이력**: v0.1에서 "Match Rate 100%(잠정)"으로 기재했으나, Codex 검증 결과 **기각**한다. Unit 2에 이어 이번에도 자체 검증만으로는 놓친 사항이 다수 발견됨 — 특히 **관리자 세션이 일반 사용자 경로에 접근 가능하다는 High 우선순위 보안 이슈**를 최초 문서화 시 완전히 놓쳤었다.

---

## 1. Plan(FR) 대비 구현 일치 여부

| ID | Requirement | 코드 근거 | 일치 여부 |
|----|-------------|-----------|-----------|
| FR-01 | `/admin/**`은 `ROLE_ADMIN`만 접근 | 확인됨 | ✅ 일치 |
| FR-02 | 사용자 폼에서 ADMIN 로그인 시 강제 로그아웃 | 확인됨 | ✅ 일치 |
| FR-03 | 관리자 폼에서 USER 로그인 시 강제 로그아웃 | 확인됨 | ✅ 일치 |
| FR-04 | `status != ACTIVE` 로그인 거부 | 확인됨(테스트 미커버) | ✅ 일치 |
| FR-05 | 비밀번호 변경/탈퇴 시 현재 비밀번호 검증 | 확인됨 | ✅ 일치 |
| FR-06 | 관리자 CSRF 실패와 권한부족 구분 | 확인됨. **[v0.1 오류 정정] "테스트 없음"으로 잘못 기재했으나 `AdminPageControllerTest`에 실제 검증 테스트 존재** | ✅ 일치(테스트 커버리지 서술만 정정) |

**FR 자체의 코드 구현 일치율: 6/6.** 다만 이는 "Plan에 명시된 요구사항이 구현되어 있는가"만 확인한 것이며, **Plan 문서 자체가 다루지 않은 중요한 동작(§2 참조)이 존재**한다.

---

## 2. Plan/Design 문서가 다루지 않은 중요 동작 (누락, "불일치"와는 다른 범주)

Codex 교차검증에서 코드에는 존재하지만 Plan/Design 어디에도 기술되지 않았던 동작들:

| 발견 사항 | 심각도 |
|---|---|
| 관리자 세션이 `/mypage/**` 등 일반 사용자 경로에 접근 가능 | **High** |
| CSRF 실패 리다이렉트의 `Referer` 부분 문자열 검사로 인한 잠재적 오픈 리다이렉트 | Medium |
| `role` 필드 서버 측 검증 부재(null→USER 기본값, 임의 문자열 허용) | Medium |
| 동시 세션 제한 없음(비밀번호 변경/탈퇴 후에도 다른 세션 유지) | Medium |
| 로그아웃 지점이 2곳이 아니라 3곳(+ Spring 기본 `/logout` 잔존) | Low(정확도 문제) |

이 항목들은 Design 문서 v0.3에서 반영 완료했다(§6 Security Considerations, §8 Known Gaps).

---

## 3. Design 문서 세부 주장 대조 (v0.3 정정 후 기준)

| Design 문서 주장 | 코드 확인 결과 | 일치 여부 |
|---|---|---|
| 두 필터체인이 요청 경로 매칭 기준 상호 배타적 | 확인됨(단, 세션 공유는 별개 — §2 참조) | ✅ 일치 |
| 로그아웃이 최소 3곳에 구현 + Spring 기본 `/logout` 잔존 | Codex 검증으로 확정 | ✅ 일치(v0.3 정정 후) |
| Mermaid 흐름도(CSRF→계정상태확인→비밀번호비교→세션생성→역할재검증 순서) | Codex 검증으로 순서 정정 후 확인됨 | ✅ 일치(v0.3 정정 후) |
| `NoOpPasswordEncoder.encode()`가 저장 시에도 평문 기록 | `UserService.changePassword()`의 `passwordEncoder.encode()` 호출 결과가 원문과 동일함을 Codex가 확인 | ✅ 일치(v0.3 추가) |
| CSRF 실패 시 Referer 기반 리다이렉트가 오픈 리다이렉트 위험 | Codex 검증으로 확인 | ✅ 일치(v0.3 추가) |

---

## 4. 종합 판정

**"Match Rate 100%(잠정)"를 철회한다.** FR 자체의 구현 여부는 6/6으로 정확했지만, **Plan 문서의 스코프 자체가 좁아 중요한 보안 동작(관리자-사용자 경로 교차 접근 등)을 다루지 못했다.** Gap 분석에서 "Match Rate"라는 단일 숫자가 "Plan에 적힌 것과 코드가 일치하는가"만 측정할 뿐 "Plan이 충분히 포괄적인가"는 측정하지 못한다는 방법론적 한계를 이번 사례로 확인했다.

| 구분 | 상태 |
|---|---|
| Plan FR 6개 | 전부 코드로 구현됨(일치) |
| Design 최초 버전 | 5개 주장 중 다수가 부정확/불완전 → Codex 검증 후 v0.3에서 전량 정정 |
| Plan 스코프 누락 | High 1건(관리자-사용자 경로 교차접근) 포함 총 4건 발견 → Design에는 사후 반영, **Plan 문서 자체는 갱신하지 않음(범위 밖 판단, §5 Next Steps에서 논의)** |

---

## 5. Known Gaps와의 구분

Design 문서 §8에 기록된 모든 항목은 "불일치"가 아니라 후속 개선 과제. 단, **관리자 세션의 사용자 경로 접근 가능** 건은 다른 Known Gap들과 달리 "버그"인지 "의도된 설계"인지가 불분명해 우선 정책 확인이 필요하다(단순 개선 과제로 분류하기 애매함).

---

## 6. 미확인 리스크

- ~~`./gradlew test` 미실행~~ **[2026-08-06] 실행 완료** — JDK 21로 전체 14클래스 64건 통과 확인
- ~~실제 브라우저에서 관리자 세션으로 `/mypage` 접근 시...미확인~~ **[2026-08-06] 검증 완료** — F-18 수정 후 `admin01` 로그인 상태로 `/mypage` 접근 시 403 확인(사용자 직접 테스트)
- Spring Security 정확한 버전별 `preAuthenticationChecks` 순서는 Codex가 웹 검색으로 교차 확인했으나, 이 프로젝트가 실제 사용하는 Spring Security 버전과 100% 일치하는지는 재확인 안 함

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-06 | 최초 Gap 분석 — "Match Rate 100%(잠정)"로 기재 |
| 0.2 | 2026-08-06 | Codex 교차검증 반영 전면 재작성 — 100% 판정 철회, Plan 스코프 누락 4건(관리자-사용자 경로 교차접근 등) 확인, "일치율"과 "포괄성"은 다른 문제임을 명시 |
