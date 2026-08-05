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

### F-05. 대댓글의 대댓글(2단계 중첩)이 서버에서 차단되지 않음
- **출처**: Unit 4(댓글/대댓글)
- **내용**: `validateParentCommentId()`가 부모 댓글의 존재/삭제여부/동일콘텐츠만 검증하고, 부모 자체가 이미 대댓글인지는 검증하지 않음
- **리스크**: 기획상 "대댓글까지만" 지원이 API 레벨에서 강제되지 않음(프론트 UI가 막고 있을 가능성 있으나 미확인)
- **권장**: 프론트 JS 확인 후 서버 검증 추가 여부 결정

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

---

## 요약 통계

| 우선순위 | 건수 |
|---|---|
| High | 3 (F-01~F-03) |
| Medium | 6 (F-04~F-09) |
| Low | 4 (F-10~F-13) |
| **합계** | **13** |

## Next Steps

- [ ] F-01(NoOpPasswordEncoder), F-02(로그아웃 중복), F-03(예외처리 분산)은 §2.4 승격 후보(Unit 2/8/13)와 겹치는 부분이 있어 2차 사이클 정밀 문서화 시 우선 반영
- [ ] F-04, F-08은 다음에 해당 unit(Unit 15, 14)을 실제로 수정할 일이 생기면 착수 전 반드시 재확인
- [ ] 나머지 항목은 백로그로 유지, 우선순위 재조정은 필요 시 별도 논의

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-05 | 최초 작성 — 1차 사이클 17개 compact card에서 발견된 이슈 13건 정리 |
