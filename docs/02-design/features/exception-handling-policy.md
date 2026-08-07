# 예외 처리 표준화 설계 (F-03) — Codex 구현용

> **Project**: dailyDevInsight | **Date**: 2026-08-07
> **정책 결정 근거**: `docs/01-plan/features/service-quality-roadmap.md` §2.1 Phase 3 P3-2 (2026-08-07, 사용자 승인 — "전면 통일")
> **Status**: ✅ 구현·검증 완료 (2026-08-07)
> **담당**: 구현 = Codex(`codex exec -s danger-full-access`), 검증 = Claude(`git diff` 전체 리뷰 + `./gradlew test --rerun` 독립 실행, 71 tests/0 failures 확인)

## ① 목적

`docs/03-analysis/full-documentation-initiative-code-findings.md` F-03에서 지적된 예외 처리 3갈래(REST/Admin/마이페이지)를 하나의 일관된 정책으로 통일한다.

## ② 현재 상태 조사 결과 (Explore 조사, 2026-08-07)

전역 예외 처리기(`@ControllerAdvice`/`@RestControllerAdvice`/`@ExceptionHandler`)는 프로젝트 전체에 **0건**.

| 갈래 | 파일 | 패턴 | 사용자에게 보이는 결과 |
|---|---|---|---|
| 1. REST | `InsightDetailService.java` 등 12건 | `ResponseStatusException(상태코드, msg)` 직접 throw | JSON 에러 응답 + 정확한 HTTP 상태(400/401/403/404). **이미 정상 동작** |
| 2. Admin | `AdminPageController.java` 20개 메서드 | `try { 성공 flash } catch (Exception e) { adminError flash = e.getMessage() }` 반복 | 항상 200(redirect). **문제**: 예외 종류 구분 없이 `e.getMessage()`를 그대로 노출 — `IllegalArgumentException`(의도된 검증 메시지)은 안전하지만, `IOException`/`NullPointerException` 등 예기치 못한 예외의 원시 메시지도 그대로 노출됨 |
| 3. 마이페이지 | `MyPageController.java` | POST 3곳(`/profile`,`/password`,`/withdraw`)은 `catch (IllegalArgumentException)`으로 안전하게 처리. **GET 5곳**(`myPageMain`,`profile`,`password`,`activity`,`withdraw`)은 동일하게 `resolveLoginUserId()`를 호출하지만 try/catch 없음 | GET 실패 시(이론상) 미포착 `IllegalArgumentException` → 전역 핸들러 없어 Spring 기본 Whitelabel 500 페이지 노출 위험 |

**중요 단서**: 마이페이지 GET 5곳은 F-18 보안 수정(`SecurityConfig.userSecurityFilterChain()`에 `hasRole("USER")` 강제, 2026-08-06 완료)으로 인해 이 컨트롤러에 도달하는 시점엔 이미 인증이 보장된다. 즉 이 미포착 경로는 **현재 설정에서는 사실상 도달 불가능한 방어적 코드**일 가능성이 높다 — "활성 버그"가 아니라 "안전망 부재"로 분류한다. 그래도 표준화 관점에서 방어적으로 막아두는 것이 맞다(설정이 바뀌거나 다른 진입 경로가 생겨도 안전).

## ③ 표준 정책 설계

### REST 레이어 — 변경 없음

`ResponseStatusException` 기반 패턴은 이미 명확한 HTTP 상태코드를 반환하고 있어 "일관성 없음" 문제가 없다. 응답 바디 스키마(`errorCode` 등) 표준화는 현재 요구되는 곳이 없어 이번 범위에서 제외(Out of Scope, YAGNI).

### Admin 레이어 — 공용 헬퍼로 20개 중복 제거

`AdminPageController`에 아래 private 헬퍼를 추가하고, 20개 `@PostMapping` 메서드의 try/catch 본문을 이 헬퍼 호출로 교체한다.

```java
@FunctionalInterface
private interface AdminAction {
    void run() throws Exception;
}

private String executeAdminAction(
        RedirectAttributes redirectAttributes,
        String redirectView,
        String successMessage,
        AdminAction action
) {
    try {
        action.run();
        redirectAttributes.addFlashAttribute("adminMessage", successMessage);
    } catch (IllegalArgumentException exception) {
        redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
    } catch (Exception exception) {
        redirectAttributes.addFlashAttribute("adminError", "작업 처리 중 오류가 발생했습니다.");
    }
    return "redirect:" + redirectView;
}
```

- `AdminAction`은 checked exception(`IOException` 등, 예: 썸네일 업로드)을 그대로 던질 수 있어야 하므로 `throws Exception` 시그니처 사용
- **핵심 변경**: `IllegalArgumentException`(서비스가 의도적으로 던지는 검증 실패 메시지)은 기존처럼 그대로 노출하되, 그 외 예외(예상치 못한 오류)는 고정 문구로 감싸 원시 메시지 노출을 막는다 — 조사 중 발견한 부가 리스크 수정
- 각 호출부는 예: `return executeAdminAction(redirectAttributes, "/admin/posts/knowledge", "게시물 정보가 수정되었습니다.", () -> adminManagementService.updateKnowledgePost(postId, category, title));`
- 20개 메서드 전부 동일 패턴으로 교체(대상 서비스: `AdminManagementService`, `CrawlConditionPresetService`, `CrawlScheduleService`, `DailyKnowledgeGenerationService`, `TechNewsCrawlingService`, `PromptTemplateService`, `GenerationScheduleService` 호출부 전부 포함)

### 마이페이지 — 클래스 스코프 `@ExceptionHandler`로 GET 방어

전역 `@ControllerAdvice`를 새로 만들지 않고, `MyPageController` 안에 로컬 `@ExceptionHandler`를 추가한다(파급 범위를 이 컨트롤러로 한정 — 다른 컨트롤러에 영향 없음).

```java
@ExceptionHandler(IllegalArgumentException.class)
public String handleUnauthenticatedAccess(IllegalArgumentException exception, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    return "redirect:/login";
}
```

- 이미 로컬 `try/catch`로 처리 중인 POST 3곳(`updateProfile`/`changePassword`/`processWithdraw`)의 폼 검증 예외는 메서드 안에서 잡히므로 이 핸들러까지 전파되지 않는다 — **동작 변경 없음**
- GET 5곳에서 `resolveLoginUserId()`가 예외를 던지는 경우에만 이 핸들러가 개입 → `/login`으로 안전하게 리다이렉트(현재는 실제 도달 가능성이 낮지만, 방어적 안전망으로 추가)

## ④ 변경 대상 파일

| 파일 | 변경 |
|---|---|
| `backend/src/main/java/com/dailydevinsight/admin/controller/AdminPageController.java` | `executeAdminAction` 헬퍼 추가, 20개 메서드 리팩터링 |
| `backend/src/main/java/com/dailydevinsight/controller/MyPageController.java` | `@ExceptionHandler(IllegalArgumentException.class)` 추가 |

REST 컨트롤러/서비스는 변경 없음.

## ⑤ 수용 기준 (Acceptance Criteria) — 2026-08-07 전부 충족 확인(Claude 검증)

- [x] Admin의 POST 핸들러(21개, 최초 조사 추정 20개보다 1개 많음)가 모두 `executeAdminAction` 헬퍼를 통해 동작하며, 개별 try/catch 코드가 남아있지 않다 — 남은 `catch (Exception` 1건은 헬퍼 내부 자체 구현뿐
- [x] Admin에서 `IllegalArgumentException`(의도된 검증 실패)은 기존과 동일하게 구체적 메시지가 `adminError`로 노출된다(회귀 없음)
- [x] Admin에서 `IllegalArgumentException`이 아닌 예외는 고정 문구("작업 처리 중 오류가 발생했습니다.")로 노출되고 원본 메시지는 노출되지 않는다 — `AdminPageControllerTest` 신규 테스트로 확인
- [x] 마이페이지 POST 3곳(`/profile`,`/password`,`/withdraw`)의 기존 동작(같은 폼으로 redirect + flash)은 변경되지 않는다(회귀 없음)
- [x] 마이페이지 GET 흐름에서 `IllegalArgumentException`이 발생하면 `/login`으로 redirect + `errorMessage` flash가 설정된다 — `MyPageControllerTest` 신규 테스트로 확인
- [x] 기존 테스트(`./gradlew test`) 전부 통과 + 신규 테스트 추가 — **71 tests, 0 failures, 0 errors**(Claude 독립 재실행 확인, Codex 자체 보고와 일치)

**설계 대비 구현 시 발견된 소소한 차이**: `runManualGeneration` 등 4개 호출부는 성공 시 조건부 메시지(실행 결과에 따라 다른 문구)를 사용하고 있어 `executeAdminAction(..., successMessage=null, ...)`로 호출하고 람다 내부에서 직접 `adminMessage`를 설정하도록 Codex가 판단해 처리함 — 설계 문서에 명시하지 않았던 기존 동작을 보존하기 위한 합리적 확장으로, 검토 후 승인(설계 문서 자체는 갱신하지 않고 여기 기록만 남김)

## ⑥ Out of Scope

- REST 레이어 응답 바디 스키마 표준화(`errorCode` 등 도입) — 현재 요구 없음
- 전역 `@ControllerAdvice` 도입 — Admin/마이페이지 각각 리다이렉트 대상이 다르고 이미 로컬 처리로 충분히 해결되므로, 전역화는 오히려 불필요한 추상화(YAGNI)
- 로그아웃 로직 단일화(F-02/F-22) — 별도 findings 항목, 이번 범위 아님

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-07 | 최초 설계 작성 — F-03 정책 결정 반영, 코드 조사(Explore) 기반 3갈래 실태 및 표준화 방안 확정 |
| 0.2 | 2026-08-07 | 구현·검증 완료 반영 — Codex 구현(`danger-full-access`) + Claude 독립 검증(71 tests 통과) 확인. ⑤ 수용 기준 전부 체크, 설계 대비 구현 차이점 기록 |
