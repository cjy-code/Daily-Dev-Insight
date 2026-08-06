# 전체 소급 문서화 1차 사이클 — 코드 발견 사항 (Code Findings)

> **Project**: dailyDevInsight
> **Date**: 2026-08-05
> **출처**: `docs/01-plan/features/full-documentation-initiative.md` 1차 사이클(compact card 17개) 작성 중 발견
> **성격**: 이 문서는 설계-구현 Gap 분석이 아니라, **문서화 작업 중 부산물로 발견된 실제 코드 이슈/리스크 백로그**다. 즉시 수정하지 않고 기록만 한다 (계획 문서 §2.3 Out of Scope 원칙에 따름).

---

## 검증 범위 선언 (Audit Discipline)

- 17개 unit의 compact card 작성 과정에서 정독한 코드 범위 내 발견 사항만 기록. **전수 조사가 아니라 부산물**이므로 여기 없는 이슈가 없다는 뜻은 아님
- 브라우저 실동작 검증은 하지 않음(정적 코드 정독 기준)
- `./gradlew test` 실행 검증 안 함(이 세션 환경의 JDK 버전 불일치, weekly-ai-insight Analysis 문서에서 이미 명시된 것과 동일한 제약)

---

## 우선순위 High

### F-01. 비밀번호가 `NoOpPasswordEncoder`로 처리됨
- **출처**: Unit 8(인증/보안 체계)
- **내용**: `SecurityConfig`가 `NoOpPasswordEncoder`를 `PasswordEncoder` Bean으로 등록 — 로그인·비밀번호 변경·탈퇴 검증 전체가 사실상 평문 비교
- **영향 범위**: 로그인(Unit 5/17), 비밀번호 변경·탈퇴(Unit 6) 전체
- **상태**: 기존에 `docs/MVP_SCOPE.md` §4에 이미 알려진 이슈였으나, 영향 범위가 이번 조사로 구체화됨(단일 지점이 아니라 인증 관련 전체 흐름에 영향)

### F-02. 로그아웃 로직이 최소 2곳에 중복 구현
- **출처**: Unit 5, 6, 8, 17
- **내용**: 일반 로그아웃은 `AuthController`→`AuthService.logout()`(공용 `SecurityContextLogoutHandler` 인스턴스), 회원 탈퇴 로그아웃은 `MyPageController.processWithdraw()`가 그 자리에서 `new SecurityContextLogoutHandler()`를 직접 생성해 처리
- **리스크**: 로그아웃 정책 변경(세션 무효화 방식, 쿠키 정리 등) 시 한쪽만 수정하고 다른 쪽을 놓칠 위험
- **권장**: `AuthService`로 단일화

### F-03. 예외 처리 스타일이 프로젝트 내 최소 3가지로 분산
- **출처**: Unit 2, 6, 11/14/15/16(Admin 전반)
- **내용**:
  1. REST(Unit 2, `InsightDetailService`): `ResponseStatusException`으로 직접 HTTP 상태코드 throw
  2. Admin 전반: 광범위 `catch (Exception)` → flash 메시지 변환
  3. 마이페이지 인증 실패(Unit 6): `IllegalArgumentException` 미포착, 그대로 흘림
- **리스크**: 같은 "사용자 액션 실패" 상황이 화면마다 다르게 처리되어 일관성 없는 UX, 신규 기능 추가 시 어느 패턴을 따라야 할지 판단 근거 없음
- **권장**: 정밀화(2차 사이클) 대상 unit부터 표준 예외 처리 정책 수립 검토

## 우선순위 Medium

### F-04. 게시물 수정/삭제가 홈 화면 캐시를 무효화하는지 미확인
- **출처**: Unit 15(게시물 관리)
- **내용**: `admin/posts/**`의 update/delete 메서드에 `@CacheEvict`가 있는지 확인 안 됨. 없다면 게시물 수정 후 최대 5분(홈 캐시 TTL) 동안 관리자 화면과 사용자 홈 화면 데이터가 불일치할 수 있음
- **권장**: Unit 15 정밀화 시 최우선 확인 항목

### F-05. [확정, Medium→High 재평가] 대댓글의 대댓글(다단 중첩)이 서버·프론트 모두에서 차단되지 않음
- **출처**: Unit 4(댓글/대댓글) → Unit 2 정밀화 중 `insight-detail.js` 확인으로 확정
- **내용**: `validateParentCommentId()`는 부모 댓글의 존재/삭제여부/동일콘텐츠만 검증(부모가 이미 대댓글인지는 미검증). `insight-detail.js`의 `buildCommentHtml()`도 `depth`와 무관하게 모든 댓글에 답글 버튼을 렌더링하는 재귀 구조라 **UI에서도 무제한 중첩이 실제로 가능함이 확인됨**
- **리스크**: `MVP_SCOPE.md`에 기술된 "대댓글까지만 지원"이 실제 구현과 다름(문서-코드 불일치). 화면 레이아웃도 깊은 중첩을 고려해 설계되지 않았을 가능성(들여쓰기 무한 반복)
- **권장**: (1) 정책 결정 필요 — 다단 중첩을 허용할지, 서버 검증으로 1단계로 제한할지, (2) 결정 후 `MVP_SCOPE.md` 기술 정정

### F-06. `weekly_ai_insight`뿐 아니라 DB 스키마 전체가 `docs/sql/`과 불일치 가능성
- **출처**: Unit 10(DB 스키마)
- **내용**: `OracleSchemaMigrationRunner`의 19개 `ensure*()` 메서드가 유일한 SoR. `docs/sql/`은 10개 파일뿐이라 전체 대응 여부 불확실. 유니크 인덱스 등 제약조건이 테이블 신규 생성 시에만 함께 생성되는 패턴이 여러 테이블에 반복될 가능성
- **권장**: `docs/sql/` 보강 작업을 별도 과제로 분리

### F-07. AI 생성이 "즉시 실행"과 "미리보기 후 저장" 두 플로우로 공존
- **출처**: Unit 13(AI 생성 관리)
- **내용**: `/admin/generate`(즉시)와 `/admin/generate/preview`+`/admin/generate/save`(2단계)가 둘 다 존재. 어느 쪽이 현재 주력 UI인지 코드만으로 불명
- **권장**: Unit 13 정밀화 시 실제 화면에서 어느 경로가 쓰이는지 확인, 미사용 경로면 정리 후보

### F-08. 회원 관리에서 role/status 입력값 서버 검증 여부 미확인
- **출처**: Unit 14(회원 관리)
- **내용**: 관리자가 임의 문자열을 `role`/`status`로 입력하면 `CustomUserDetailsService`가 `"ROLE_" + role`을 그대로 권한 문자열로 사용 — enum 검증이 있는지 `AdminManagementService.updateUser()` 확인 안 됨
- **권장**: 실제 수정 작업 전 반드시 확인 (관리자 권한 오남용/오타 시 영향이 큼)

### F-09. 탈퇴 시 댓글 데이터 정리 로직 없음
- **출처**: Unit 6(마이페이지)
- **내용**: 탈퇴 처리(`MyPageService.withdraw()`)가 좋아요·북마크는 선삭제하지만 `InsightComment`는 정리하지 않음. 작성자가 탈퇴해도 댓글은 그대로 남는 것으로 추정(화면 표시 방식 미확인)
- **권장**: 정책적으로 의도된 것인지(탈퇴해도 댓글 보존) 확인 필요

## 우선순위 Low

### F-10. `Top10`/`Top5` 캐시 TTL은 동일하나 무효화 트리거 확인 안 됨
- **출처**: Unit 9(캐시 정책)
- **내용**: 둘 다 20분 TTL로 캐시됨(최초 조사 오류 정정 완료 — F-11 참조). `@CacheEvict(CACHE_WEEKLY_TOP10)`은 여러 서비스에서 확인되나 `CACHE_WEEKLY_TOP5`의 무효화 트리거는 별도 확인 안 함

### F-11. [조사 오류 정정 기록] Unit 1 최초 작성 시 "Top10 캐시 미적용" 오기재
- **출처**: Unit 1 → Unit 9 작성 중 자체 발견
- **내용**: `grep -A`(뒤쪽 컨텍스트만 확인)로 조사해 메서드 위 `@Cacheable` 어노테이션을 놓쳤던 조사 오류. 실제로는 `findWeeklyHotKnowledgeTop10()`에 `@Cacheable(CACHE_WEEKLY_TOP10)` 적용되어 있음. `home-insight-list.md`에 정정 완료
- **기록 목적**: 경량 조사(compact card)라도 교차 확인 없이는 오류가 남을 수 있다는 근거 사례로 남김 — 2차 사이클(정밀 문서화) 시 유사 조사에 참고

### F-12. 대시보드/통계 지표가 매 요청 실시간 전체 집계, 캐시 없음
- **출처**: Unit 11(대시보드), Unit 16(통계)
- **내용**: `getAdminStats()`, `getContentViewStats()`, `getBookmarkStats()` 모두 캐시 없이 매번 재집계. 두 화면이 부분적으로 같은 쿼리를 중복 실행
- **권장**: 데이터 규모 증가 시 캐시 적용 검토

### F-13. 스케줄러 로직이 크롤링/생성 두 서비스에 거의 동일하게 중복 구현
- **출처**: Unit 7(스케줄러)
- **내용**: `CrawlScheduleService`와 `GenerationScheduleService`의 `isExecutionDue()`/`markExecuted()`가 공통 추상화 없이 각각 구현됨

## 2차 사이클(Unit 2 정밀화, Codex 교차검증) 추가 발견

### F-14. 조회수 세션 키가 콘텐츠 타입 대소문자를 정규화하지 않음
- **출처**: Unit 2 정밀화, Codex 교차검증
- **내용**: `insight:viewed:{type}:{id}` 세션 키가 원본 `type` 문자열을 그대로 사용. `InsightContentType.from()`은 대소문자 무시하고 해석하므로 `knowledge`/`KNOWLEDGE` 등 다른 표기로 같은 콘텐츠에 접근하면 세션 키가 달라져 조회수가 중복 증가할 수 있음
- **권장**: 세션 키 생성 시 `type`을 정규화(예: 소문자 고정) 후 사용

### F-15. 부모 댓글 삭제 시 자식 댓글이 최상위로 승격되어 표시됨
- **출처**: Unit 2 정밀화, Codex 교차검증
- **내용**: 소프트 삭제된 부모 댓글은 트리 조립(`findCommentDtos`) 시 DTO가 생성되지 않아, 그 자식(대댓글)이 부모 없이 최상위 댓글처럼 표시됨. "삭제된 댓글입니다" 같은 placeholder 없음
- **권장**: UX 관점에서 의도된 동작인지 확인, 필요 시 삭제된 부모의 placeholder 표시 검토

### F-16. 댓글 조회에 페이지네이션/depth 제한 없음
- **출처**: Unit 2 정밀화, Codex 교차검증
- **내용**: `findCommentDtos()`가 콘텐츠의 전체 댓글을 한 번에 로드하고 캐시·직렬화·프론트 재귀 렌더링까지 전부 무제한. 댓글이 많거나 깊게 중첩되면 성능 저하 가능
- **권장**: 데이터 증가 추이를 보며 페이지네이션/depth 제한 도입 검토

### F-17. SSR이 대댓글을 표시하지 않음(JS 의존)
- **출처**: Unit 2 정밀화, Codex 교차검증
- **내용**: `insight-detail.html`은 최상위 댓글만 `th:each`로 렌더링하고 `comment.replies`는 서버 템플릿에서 출력하지 않음. 대댓글은 클라이언트 JS 재조회 후에만 보임
- **권장**: SEO/접근성(JS 비활성 환경) 영향이 있다면 SSR에서도 재귀 렌더링하도록 개선 검토

## 3차 사이클(Unit 8 정밀화, Codex 교차검증) 추가 발견

### F-18. [High] 관리자 세션이 일반 사용자 경로에도 접근 가능
- **출처**: Unit 8 정밀화, Codex 교차검증
- **내용**: `userSecurityFilterChain`이 `/admin/**`을 제외한 모든 경로에 `authenticated()`만 요구하고 `ROLE_USER`를 강제하지 않음. `ROLE_ADMIN`으로 로그인한 세션도 `/mypage/**` 등 일반 사용자 경로에 그대로 접근 가능
- **리스크**: "관리자/사용자 경로가 완전히 분리되어 있다"는 암묵적 전제가 성립하지 않음. 관리자 계정으로 일반 사용자 기능(탈퇴 등)을 실행할 수 있다는 뜻이기도 함
- **권장**: 의도된 설계인지 정책 확인 필요. 의도가 아니라면 `userSecurityFilterChain`에 `hasRole("USER")` 추가 검토
- **✅ 결정 및 수정 완료 (2026-08-06, 사용자 승인)**: `SecurityConfig.userSecurityFilterChain()`에 `hasRole("USER")` 적용, `/auth/logout`은 공용 엔드포인트라 `authenticated()`로 예외 처리(관리자 로그아웃 회귀 방지). 전체 테스트 14클래스 64건 통과 확인(JDK 21). 상태: **완료**

### F-19. [Medium] CSRF 실패 리다이렉트에 잠재적 오픈 리다이렉트 가능성
- **출처**: Unit 8 정밀화, Codex 교차검증
- **내용**: `SecurityConfig.processAdminAccessDenied()`가 CSRF 실패 시 `Referer` 헤더에 `/admin/posts/` 문자열이 포함되면 검증 없이 그대로 리다이렉트. `Referer`는 클라이언트가 임의 조작 가능한 헤더
- **권장**: 정확한 경로 화이트리스트 매칭으로 전환 검토

### F-20. [Medium] `role` 필드에 서버 측 검증 없음
- **출처**: Unit 8 정밀화, Codex 교차검증 (Unit 14 F-08과 연관)
- **내용**: `role`이 null이면 `"USER"` 기본 처리, 임의 문자열도 검증 없이 `"ROLE_" + 값`으로 변환됨
- **권장**: enum 기반 검증 추가, Unit 14(회원 관리) 개선과 함께 검토

### F-21. [Low] 동시 세션 제한 없음
- **출처**: Unit 8 정밀화, Codex 교차검증
- **내용**: 비밀번호 변경이나 탈퇴가 발생해도 다른 브라우저의 기존 로그인 세션이 즉시 무효화되지 않음(현재 요청의 세션만 처리)
- **권장**: 필요 시 세션 레지스트리 도입 검토

### F-22. [Low] 로그아웃 지점이 실제로는 3곳 + Spring 기본 `/logout` 잔존
- **출처**: Unit 8 정밀화, Codex 교차검증 (F-02의 정확도 보완)
- **내용**: `AuthService`, `MyPageController.processWithdraw()` 외에 `SecurityConfig`의 역할불일치 처리도 별도 로그아웃 지점. `.logout()`을 커스터마이징하지 않아 Spring Security 기본 `/logout` 엔드포인트도 비활성화되지 않은 채 남아있음
- **권장**: F-02와 함께 로그아웃 로직 단일화 시 이 두 가지도 함께 정리

## 4차 사이클(Unit 13 정밀화, Codex 교차검증) 추가 발견

### F-23. [High] `/admin/generate`(즉시 생성)가 현재 UI에서 호출되지 않는 고아 엔드포인트로 추정됨
- **출처**: Unit 13 정밀화, Codex 교차검증
- **내용**: `admin/generation.html`의 생성 버튼은 하나뿐이고 항상 `/admin/generation/compose`(미리보기)를 새창으로 연다. `POST /admin/generate`를 호출하는 폼 submit이나 JS가 화면 어디에도 없음. 컨트롤러·서비스 코드(`executeManualGeneration`)는 살아있으나 도달 경로가 없음
- **리스크**: 죽은 코드가 유지보수 부담으로 남거나, 반대로 의도적으로 남긴 대체 경로(API 직접 호출용 등)를 아무도 모르게 방치하는 상황일 수 있음
- **권장**: 삭제할지, 문서화해서 남길지, UI에 다시 연결할지 정책 결정 필요(2차 사이클 정밀화 중 가장 우선순위 높은 후속 질문)
- **✅ 결정 (2026-08-06, 사용자 승인)**: **일단 보류(삭제 안 함).** 코드는 그대로 유지하고 Known Gap으로만 기록. 이 영역을 실제로 손댈 일이 생기면 그때 재판단. 상태: **결정 완료, 조치 없음(현행 유지)**

### F-24. [Medium] 이미지 생성 fail-soft가 서비스 계약이 아니라 구현체 의존적
- **출처**: Unit 13 정밀화, Codex 교차검증
- **내용**: `tryGeneratePreviewImage()`에 `try/catch`가 없음. 현재 활성 구현체 `OpenAiImageGenerationClient`가 내부에서 예외를 잡아 빈 문자열을 반환하기 때문에 결과적으로 안전할 뿐, 다른 구현체가 예외를 던지면 전체 생성이 실패할 수 있음
- **권장**: 서비스 레이어에서 명시적으로 이미지 생성 실패를 격리하도록 개선

### F-25. [Medium] 콘텐츠 저장과 생성 이력 저장이 트랜잭션으로 묶여있지 않음
- **출처**: Unit 13 정밀화, Codex 교차검증
- **내용**: `DailyKnowledge` 저장 후 `GenerationHistory` 저장이 실패하면, 실제 콘텐츠는 저장됐는데 호출 결과는 실패로 보일 수 있음
- **권장**: `@Transactional`로 묶거나, 실패 시 보정 로직 추가

### F-26. [Low] AI 생성 서비스 레벨 테스트가 예약 중복 정책 2건뿐
- **출처**: Unit 13 정밀화, Codex 교차검증
- **내용**: 수동 생성, 미리보기, 이미지 생성 실패, FAILED 이력 저장 경로가 전혀 테스트되지 않음
- **권장**: 핵심 경로 위주로 테스트 확충

### F-27. [Low] `GenerationHistory`에 별도 `errorCode` 컬럼 없음
- **출처**: Unit 13 정밀화, Codex 교차검증
- **내용**: 실패 코드가 `[errorCode] 메시지` 형태 문자열로 `errorMessage` 필드에 합쳐져 저장됨 — 코드 기준 집계/필터링이 어려움
- **권장**: 별도 컬럼 분리 검토

---

## 요약 통계

| 우선순위 | 건수 |
|---|---|
| High | 6 (F-01~F-03, F-05, F-18, F-23) |
| Medium | 13 (F-04, F-06~F-09, F-14, F-16, F-17, F-19, F-20, F-24, F-25) |
| Low | 8 (F-10~F-13, F-21, F-22, F-26, F-27) |
| **합계** | **27** |

> F-05는 1차 사이클에서 Medium으로 기록했으나 Unit 2 정밀화(Codex 교차검증)에서 서버·프론트 양쪽 실제 허용이 확정되어 **High로 재평가**됨. F-18(관리자 세션의 사용자 경로 접근)은 Unit 8 정밀화, F-23(`/admin/generate` 고아 엔드포인트)은 Unit 13 정밀화에서 신규 발견된 High 항목.

## Next Steps

- [x] F-01(NoOpPasswordEncoder), F-05(다단 대댓글)는 Unit 2/8 정밀화 과정에서 우선 반영 시작
- [x] **F-18, F-23 사용자 결정 완료 (2026-08-06)**: F-18 → 차단, F-23 → 일단 보류(현행 유지)
- [x] F-18 실제 코드 수정 완료(`SecurityConfig`에 `hasRole("USER")` 적용, 전체 테스트 통과)
- [ ] F-02(로그아웃 중복), F-03(예외처리 분산)은 Unit 8 정밀화 시 반영 예정
- [ ] F-04, F-08은 다음에 해당 unit(Unit 15, 14)을 실제로 수정할 일이 생기면 착수 전 반드시 재확인
- [ ] F-14~F-17(Unit 2), F-24~F-27(Unit 13) 신규 발견은 백로그로 유지
- [ ] 나머지 항목은 백로그로 유지, 우선순위 재조정은 필요 시 별도 논의

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 — 1차 사이클 17개 compact card에서 발견된 이슈 13건 정리 |
| 0.2 | 2026-08-06 | Unit 2 정밀화(Codex 교차검증) 반영 — F-05를 Medium→High 재평가, 신규 발견 F-14~F-17 추가(조회수 세션키 대소문자, 부모삭제시 자식승격, 댓글 페이지네이션 부재, SSR 대댓글 미표시) |
| 0.3 | 2026-08-06 | Unit 8 정밀화(Codex 교차검증) 반영 — 신규 발견 F-18~F-22 추가, **F-18(관리자 세션의 사용자 경로 접근, High)은 정책 확인이 필요한 최우선 항목으로 표시** |
| 0.4 | 2026-08-06 | Unit 13 정밀화(Codex 교차검증) 반영 — 신규 발견 F-23~F-27 추가, **F-23(`/admin/generate` 고아 엔드포인트, High)을 F-18과 함께 최우선 확인 항목으로 표시**. 2차 사이클(Unit 2/8/13) 정밀화 전체 완료 |
