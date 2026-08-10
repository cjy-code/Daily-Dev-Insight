# NoOpPasswordEncoder → BCrypt 마이그레이션 설계 (P4-3) — Codex 구현용

> **Project**: dailyDevInsight | **Date**: 2026-08-07 (개정: 2026-08-10)
> **정책 결정 근거**: `docs/01-plan/features/service-quality-roadmap.md` P4-3, §7(Phase 4 보안 리뷰 게이트)
> **Status**: v0.3 — **security-architect 재리뷰 완료(2026-08-10, v0.2 대상 조건부 GO → D1/D2 반영해 v0.3으로 정정)**. **게이트 통과, Codex 구현 착수 가능**
> **담당**: 설계 = Claude, 보안 리뷰 = security-architect, 구현 = Codex, 검증 = Claude

## ⓪ v0.1 → v0.2 변경 배경 (왜 다시 썼는가)

v0.1은 `DelegatingPasswordEncoder` + `NoOpPasswordEncoder` 폴백(레거시 평문 매칭) + `UserDetailsPasswordService`(로그인 시 자동 재해시) 조합이었다. security-architect 리뷰(2026-08-10)에서 **No-Go** 판정과 함께 다음 결함이 지적됨:

| ID | 문제 |
|---|---|
| B1 (Critical) | `updatePassword()`에 `@Transactional` 누락 → 레거시 계정 로그인마다 500 |
| B2 (High) | 재해시 쓰기 실패가 정상 로그인까지 막음 |
| B3 (High) | "기존 테스트 무수정 통과" 주장이 틀림 — 실제로는 컨텍스트 기동 실패/NPE로 깨짐 |
| B4 (High) | NoOp 폴백이 BCrypt와의 응답시간 차이로 **신규 사용자 열거 채널**을 만듦(휴면 계정에 대해 무기한 노출) |
| B5 (High) | BCrypt 72바이트 절삭 미대응. "`UserService.java` 변경 불필요" 주장이 틀림 |
| B6 (Medium) | 시드 자격증명 노출은 해결 안 됨 + 시드 SQL 재실행 시 해시가 평문으로 롤백됨 |

리뷰의 권고안(대상이 시드 계정 2건뿐이므로 폴백/자동재해시 메커니즘 자체를 제거)을 채택한다. **이 결정은 사용자 승인 완료(2026-08-10)**.

## ① 목적

`SecurityConfig`의 `NoOpPasswordEncoder`(평문 비교/저장)를 BCrypt로 교체한다. 대상 계정이 시드 2건(`user01`/`admin01`)뿐이므로, "로그인 시점 자동 재해시" 같은 런타임 마이그레이션 메커니즘 대신 **애플리케이션 기동 시 1회성 데이터 보정**으로 해결한다 — 새 코드 경로(인증 필터체인 변경, `UserDetailsPasswordService`)를 만들지 않아 v0.1의 B1~B4가 애초에 발생할 여지가 없다.

## ② 현재 상태 (코드 확인 완료, v0.1 대비 라인 재검증)

| 파일 | 관련 내용 |
|---|---|
| `SecurityConfig.java:100-101` | `PasswordEncoder` Bean = `NoOpPasswordEncoder.getInstance()` |
| `SecurityConfig.java:62, 90` | `adminSecurityFilterChain`/`userSecurityFilterChain` 둘 다 `.userDetailsService(userDetailsService)` 사용 — **이번 설계에서는 변경하지 않음** |
| `UserService.java:62-82` (`changePassword`) | `passwordEncoder.matches()`로 현재 비밀번호 검증 후 `passwordEncoder.encode(normalizedNewPassword)`로 저장 — 인코더만 바뀌면 신규 비밀번호는 자동으로 BCrypt 저장됨. `:74`에 최소 8자 검증은 있으나 **최대 길이 검증 없음**(→ ④에서 추가) |
| `UserRepository.updatePasswordById(Long id, String password, LocalDateTime updatedAt)` | 기존 재사용 |
| `User.java:32` | `password` 컬럼 `length = 255` — BCrypt 해시(60자) 저장에 충분 |
| `OracleSchemaMigrationRunner.java` (`@PostConstruct ensureOracleSchemaMigrations()`) | 기동 시 스키마/데이터를 **멱등하게(idempotent)** 보정하는 기존 컨벤션이 이미 존재함(`ensureSequenceAlignedWithTableMaxId` 등). **이번 시드 비밀번호 재해시를 이 패턴에 맞춰 추가**(③-2) — 별도 메커니즘을 새로 발명하지 않음 |
| `docs/sql/2026-04-15_users_user_id_migration_oracle.sql:62-70, 81-89` | `MERGE ... WHEN MATCHED THEN UPDATE SET target.password = source.password` — 이 스크립트를 수동 재실행하면 BCrypt 해시가 평문 `'1234'`로 **롤백됨**(B6). 앱 기동 경로는 이 파일을 읽지 않으므로 자동 실행 위험은 아니고, 수동 재실행 시 위험 |
| `SecurityConfigLoginFlowTest.java` | `@WebMvcTest(LoginController.class) @Import(SecurityConfig.class)`, `@MockBean UserDetailsService`. Mock `User.password("pw")`(접두어 없음) + 폼에 `"pw"` 제출 |

## ③ 설계

### 1) `PasswordEncoder` Bean — BCrypt로 교체 (폴백 없음)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

- Spring Security가 권장하는 표준 팩토리 메서드. 기본 인코딩 id `bcrypt`, `encode()`는 항상 `{bcrypt}$2a$...` 형식으로 저장.
- `defaultPasswordEncoderForMatches`를 설정하지 않으므로, `{id}` 접두어가 없는 저장값에 대해 `matches()`는 **`IllegalArgumentException`을 던진다**(조용한 `false`가 아님). 즉 **③-2의 기동 시 재해시가 반드시 로그인 트래픽보다 먼저 완료되어 있어야 한다** — Spring Boot는 `@PostConstruct`를 포함한 컨텍스트 리프레시(`finishBeanFactoryInitialization`)가 끝난 뒤에야 `WebServerStartStopLifecycle`이 내장 톰캣 커넥터를 붙이고 요청을 받기 시작하므로, 기동 순서상 이는 항상 보장된다(추가 락 불필요). **단, 이 보장은 `spring.main.lazy-initialization=true`를 켜지 않고 `OracleSchemaMigrationRunner`에 `@Lazy`를 붙이지 않는다는 전제 하에 성립한다** — 현재 `application.yml`/프로필 파일에 lazy-init 설정 없음을 확인함(2026-08-10 재검토).

### 2) 시드 계정 비밀번호를 기동 시 1회성으로 BCrypt 재해시 (`OracleSchemaMigrationRunner`)

기존 "ensure X" 멱등 패턴을 그대로 따른다. `PasswordEncoder`를 생성자 주입받아 추가:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OracleSchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder; // 추가

    @PostConstruct
    public void ensureOracleSchemaMigrations() {
        if (!isOracleDatabase()) {
            return;
        }
        // ... 기존 항목들 ...
        ensureSeedUserPasswordsHashed();
    }

    /**
     * @date 2026-08-10
     * @desc 시드 계정(user01/admin01)의 평문 비밀번호를 BCrypt 해시로 1회성 재기록합니다.
     */
    private void ensureSeedUserPasswordsHashed() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, password FROM users WHERE id IN (9001, 9002)");
        for (Map<String, Object> row : rows) {
            String storedPassword = (String) row.get("PASSWORD");
            if (storedPassword == null || storedPassword.startsWith("{")) {
                continue; // 이미 {id} 접두어가 붙은 인코딩 값(또는 이례적 null) — 멱등, 재작업 안 함
            }
            Number id = (Number) row.get("ID");
            jdbcTemplate.update(
                    "UPDATE users SET password = ?, updated_at = SYSTIMESTAMP WHERE id = ?",
                    passwordEncoder.encode(storedPassword), id.longValue()
            );
            log.info("Rehashed seed user password to BCrypt: id={}", id);
        }
    }
}
```

- `queryForList` + 루프 후 `update`(재조회 결과를 먼저 리스트로 받은 뒤 커서가 닫힌 상태에서 갱신)를 사용한다. `jdbcTemplate.query(sql, rs -> {...})` 형태의 암시적 타입 람다는 `query(String, ResultSetExtractor<T>)`/`query(String, RowCallbackHandler)` 오버로드가 모호해 컴파일 에러가 날 수 있고, 콜백 안에서 곧바로 `update()`를 호출하면 커서가 열린 채로 커넥션 풀에서 추가 커넥션을 잡는 불필요한 부작용이 있다(2026-08-10 재검토 반영, D2).
- `storedPassword.startsWith("{")` 체크로 재실행해도 안전(멱등) — 스키마 마이그레이션과 동일한 안전 원칙. `storedPassword == null` 가드는 `password` 컬럼이 `NOT NULL`이라 정상 도달하지 않지만, `passwordEncoder.encode(null)`이 `IllegalArgumentException`을 던져 앱 기동을 막는 사고를 방지하기 위한 방어.
- 로그에 평문/해시 값을 남기지 않음(id만 기록) — OWASP A09.
- ID 9001/9002로 시드 데이터를 하드코딩 대상으로 지정한 것은 `docs/sql/2026-04-15_users_user_id_migration_oracle.sql`의 시드 값과 일치시킨 것. 향후 실사용자가 늘어나면 이 방식은 유지보수되지 않으므로 ⑦에 범위 제한을 명시.

### 3) 시드 SQL 스크립트 수정 (B6 대응)

`docs/sql/2026-04-15_users_user_id_migration_oracle.sql`의 두 `MERGE` 문에서 `WHEN MATCHED THEN UPDATE SET` 절의 `target.password = source.password` 라인을 제거한다. 이 스크립트를 향후 다른 목적(예: `name`/`role` 보정)으로 재실행하더라도, 이미 BCrypt로 재해시된 비밀번호가 평문으로 되돌아가지 않도록 한다. `WHEN NOT MATCHED THEN INSERT`(신규 행 삽입) 경로는 그대로 유지 — 최초 삽입 시에는 평문으로 들어가더라도 ③-2가 다음 기동 시 자동으로 재해시한다.

### 4) 신규 비밀번호 최대 길이 검증 추가 (B5 대응)

`UserService.changePassword()`(`:74` 인근)에 최소 길이 검증과 나란히 최대 길이 검증을 추가한다. BCrypt는 72바이트 초과분을 조용히 잘라내므로(UTF-8 한글 기준 24자부터 절삭 시작), 이를 애플리케이션 레벨에서 명시적으로 거부한다:

```java
if (normalizedNewPassword.length() < 8) {
    throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
}
if (normalizedNewPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
    throw new IllegalArgumentException("새 비밀번호는 72바이트(UTF-8 기준)를 초과할 수 없습니다.");
}
```

## ④ 변경 대상 파일

| 파일 | 변경 |
|---|---|
| `backend/src/main/java/com/dailydevinsight/config/SecurityConfig.java` | `passwordEncoder()` Bean을 `PasswordEncoderFactories.createDelegatingPasswordEncoder()`로 교체. `NoOpPasswordEncoder` import 제거. **필터체인(`.userDetailsService(...)`)은 변경하지 않음** |
| `backend/src/main/java/com/dailydevinsight/config/OracleSchemaMigrationRunner.java` | `PasswordEncoder` 생성자 주입 추가, `ensureSeedUserPasswordsHashed()` 메서드 추가 및 `ensureOracleSchemaMigrations()`에서 호출 |
| `backend/src/main/java/com/dailydevinsight/service/UserService.java` | `changePassword()`에 최대 길이(72바이트) 검증 추가 |
| `docs/sql/2026-04-15_users_user_id_migration_oracle.sql` | 두 `MERGE` 문의 `WHEN MATCHED` 절에서 `password` 컬럼 갱신 제거 |
| `backend/src/test/java/com/dailydevinsight/config/SecurityConfigLoginFlowTest.java` | Mock 비밀번호를 `{id}` 접두어 없는 `"pw"`에서 **프로덕션과 동일한 팩토리로 인코딩된 값**으로 변경하고 폼 제출 값은 평문 `"pw"` 유지(아래 참고, D1) |

`CustomUserDetailsService.java`, `SecurityConfig.java`의 필터체인, `DaoAuthenticationProvider` 빈은 **변경 불필요**(v0.1과의 핵심 차이) — 폴백/자동재해시 메커니즘 자체가 없으므로.

## ⑤ 영향받는 기존 테스트

- `SecurityConfigLoginFlowTest.java`: 위 ④ 변경대로 mock 저장값을 BCrypt 인코딩값으로 바꿔야 함(**수정 필요** — v0.1의 "수정 불필요" 주장은 틀렸었고, v0.2도 수정이 필요하지만 이유가 다름: 필터체인이 아니라 인코더 자체가 `{id}` 접두어 없는 값을 거부하기 때문). **주의(D1, 2026-08-10 재검토)**: `new BCryptPasswordEncoder().encode("pw")`로 바꾸는 것만으로는 안 된다 — 이 값에는 `{bcrypt}` 접두어가 없어 여전히 `IllegalArgumentException`으로 실패한다. 반드시 프로덕션과 동일한 팩토리를 써야 한다:
  ```java
  private static final String ENCODED_PW =
          PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("pw");
  // ... .password(ENCODED_PW) ...  (폼 제출은 평문 "pw" 그대로)
  ```
  수정 후에는 정상 통과 예상. `formLogin`을 사용하는 테스트는 이 파일뿐이며(프로젝트 전체 확인), `SecurityConfig`를 `@Import`하는 나머지 테스트는 `@WithMockUser` 경로라 인코더 변경의 영향을 받지 않는다.
- `AuthControllerTest.java`: 비밀번호 인코딩과 무관. 영향 없음.
- 신규 테스트 필요:
  1. 앱 기동(또는 `ensureSeedUserPasswordsHashed()` 단위 테스트) 후 시드 계정 비밀번호가 `{bcrypt}` 형식으로 재기록되는지
  2. 재기록된 해시로 로그인 성공하는지
  3. 이미 `{bcrypt}`로 시작하는 값은 재호출 시 변경되지 않는지(멱등성)
  4. `changePassword()` 신규 저장값이 `{bcrypt}` 형식인지
  5. `changePassword()`에 72바이트 초과 비밀번호를 넣으면 `IllegalArgumentException`이 발생하는지
  6. `changePassword()`/`withdraw()`에서 틀린 현재 비밀번호는 여전히 거부되는지(BCrypt `matches()` 오답 경로)

## ⑥ 수용 기준 (Acceptance Criteria)

- [ ] 앱 기동 후 `user01`/`admin01`의 DB `password` 컬럼이 `{bcrypt}$2a$...` 형식으로 자동 갱신된다
- [ ] 갱신 후 `user01`/`admin01`이 기존 평문 비밀번호(`1234`)로 정상 로그인된다
- [ ] 앱을 재기동해도(멱등) 이미 재해시된 값이 다시 바뀌거나 오류가 발생하지 않는다
- [ ] `UserService.changePassword()`로 변경한 새 비밀번호는 저장 시점부터 `{bcrypt}` 형식이다
- [ ] 72바이트(UTF-8) 초과 비밀번호로 `changePassword()` 호출 시 명확히 거부된다
- [ ] `docs/sql/2026-04-15_users_user_id_migration_oracle.sql`을 재실행해도 이미 BCrypt로 재해시된 비밀번호가 평문으로 되돌아가지 않는다
- [ ] `SecurityConfigLoginFlowTest`(수정본), `AuthControllerTest` 포함 기존 테스트(`./gradlew test`)가 전부 통과한다
- [ ] ⑤의 신규 시나리오 테스트가 추가되고 통과한다
- [ ] (N4, 2026-08-10 재검토 추가) 재해시된 BCrypt 계정에 **틀린** 비밀번호로 로그인 시 500이 아니라 정상적으로 `/login?error`로 리다이렉트된다 — 이번 변경에서 "인증 실패"와 "500 에러"의 구분이 가장 중요한 회귀 지점이므로 명시

## ⑦ Out of Scope / 리스크

- **`ensureSeedUserPasswordsHashed()`는 ID 9001/9002에 하드코딩된 1회성 보정**이다. 실사용자가 늘어난 뒤 평문으로 저장된 계정이 생기는 시나리오(예: 향후 다른 경로로 비밀번호가 평문 삽입되는 경우)는 이 메커니즘으로 해결되지 않는다 — 애초에 회원가입(P4-1)이 아직 미개방이라 신규 평문 삽입 경로가 없으므로 현재는 영향 없음. 실사용자 유입 시점에 재검토 필요.
- **시드 자격증명(`user01`/`admin01`의 비밀번호가 `1234`라는 사실) 노출 문제는 이번 변경으로 해결되지 않는다.** 저장 형식만 BCrypt로 바뀔 뿐, 이미 알려진 비밀번호 값 자체는 그대로 유효하다. 별도 로테이션/정책 필요 여부는 이번 범위 밖.
- **로그인 시도에 대한 rate limiting/계정 잠금이 프로젝트 전체에 없음**(security-architect 리뷰에서 grep으로 확인). `mitigateAgainstTimingAttack`으로 인해 미인증 `POST /login` 요청마다 BCrypt 연산이 1회 소모되므로 이론상 CPU 증폭 여지가 있으나, 이는 이번 P4-3 범위가 아니라 별도 항목(로드맵에 백로그 필요 시 추가)으로 다룬다.
- BCrypt cost factor는 기본값(strength 10) 유지 — rate limiting 부재 상태에서 상향은 오히려 DoS 증폭 여지를 키우므로, rate limiting 도입 이후 재검토.
- 회원가입(P4-1), 비밀번호 찾기(P4-2)는 별도 작업. 이번 설계는 P4-3(인코더 교체)에 한정.
- `admin/service`, `admin/controller` 등 다른 패키지는 이 변경과 무관, 손대지 않음.
- **(N1, 2026-08-10 재검토 추가) 재해시 전 상태에서 내부 예외 메시지가 사용자 화면에 노출될 수 있는 이론적 경로.** `MyPageController`의 비밀번호 변경/탈퇴 핸들러가 `IllegalArgumentException.getMessage()`를 그대로 flash attribute로 노출한다. 정상 상태(시드 2건은 기동 즉시 재해시됨)에서는 도달 불가하지만, 향후 P4-1/P4-2로 평문 삽입 경로가 생기면 `matches()`가 던지는 `IllegalArgumentException` 메시지가 그대로 사용자에게 보일 수 있다 — 그 시점에 공통 예외 처리와 함께 재검토.
- **(N2, 2026-08-10 재검토 추가) 시드 SQL 신규 삽입 직후 재기동 전까지의 로그인 불가 창.** `docs/sql/2026-04-15_users_user_id_migration_oracle.sql`의 `WHEN NOT MATCHED THEN INSERT`는 여전히 평문으로 삽입한다. 다음 앱 기동 시 자동 재해시되지만, 삽입 후 재기동 전까지 해당 계정 로그인은 500이 된다 — 신규 삽입 후에는 반드시 앱을 재기동할 것. (`{noop}1234`처럼 접두어를 미리 붙여 우회하지 말 것 — 재해시 로직이 그 리터럴 문자열 자체를 다시 해시해버려 원래 비밀번호를 잃는다.)

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-07 | 최초 설계 작성 — `DelegatingPasswordEncoder` + `UserDetailsPasswordService` 기반 무중단 마이그레이션 전략. security-architect 리뷰 대기 |
| 0.2 | 2026-08-10 | security-architect 리뷰 결과 **No-Go**(B1~B6, 특히 트랜잭션 누락으로 인한 로그인 500 및 신규 사용자열거 채널) 반영, 사용자 승인 하에 리뷰 권고안(폴백/자동재해시 메커니즘 제거, 시드 2건 기동 시 1회성 재해시로 단순화) 채택. `OracleSchemaMigrationRunner`의 기존 멱등 마이그레이션 패턴에 편입시켜 `SecurityConfig`/`CustomUserDetailsService`/필터체인 변경 불필요하도록 재설계. B5(72바이트 절삭) 대응 위해 `UserService` 최대 길이 검증 추가. B6(시드 SQL 롤백 위험) 대응 위해 MERGE 스크립트에서 `password` 갱신 제거 |
| 0.3 | 2026-08-10 | security-architect 재리뷰(v0.2 대상) 결과 **조건부 GO** — B1~B6 v0.2에서 재발 없음 확인, 기동 순서 보장(lazy-init 미설정 조건부) 검증 완료. 필수 수정 2건 반영: D1(테스트 mock 비밀번호에 `{bcrypt}` 접두어 누락 → `PasswordEncoderFactories.createDelegatingPasswordEncoder()`로 정정), D2(`ensureSeedUserPasswordsHashed()`의 JDBC 오버로드 모호성 + 콜백 내 중첩 UPDATE 위험 → `queryForList`+루프 방식으로 정정). 비차단 권고 N1(내부 예외 메시지 노출 이론적 경로)/N2(시드 삽입 후 재기동 전 로그인 불가 창)를 §⑦에, N4(오답 비밀번호 시 500 아님 검증)를 §⑥에 추가. **본 v0.3부터 Codex 구현 착수 가능** |
