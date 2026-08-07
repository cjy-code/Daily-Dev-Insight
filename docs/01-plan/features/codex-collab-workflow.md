# Claude+Codex 협업 워크플로우 Planning Document

> **Summary**: Claude Code(검증·문서)와 Codex CLI(구현)가 문서를 매개로 협업하는 반복 가능한 개발 파이프라인
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-03
> **Status**: Draft

---

## Executive Summary

| Perspective | Content |
|-------------|---------|
| **Problem** | 기능 구현을 한 에이전트가 기획-구현-검증까지 전부 맡으면 자기검증(self-review)의 한계로 설계-구현 불일치를 놓치기 쉽다 |
| **Solution** | Claude는 요구사항 정리·설계 문서화·구현 결과 검증을 담당하고, Codex CLI(`codex exec`)는 그 설계 문서를 입력받아 실제 코드를 작성하는 역할 분리 파이프라인을 구축한다 |
| **Function/UX Effect** | 개발자(사용자) 입장에서는 문서 기반으로 Claude와 대화하면 되고, 실제 코드 작성은 Codex가 백그라운드로 수행 |
| **Core Value** | 역할 분리를 통한 교차검증(cross-check)으로 설계-구현 gap을 줄이고, 작업 이력을 문서(PDCA)로 남긴다 |

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | 단일 에이전트의 자기검증 한계를 역할 분리로 보완 |
| **WHO** | dailyDevInsight 개발자 (jychoi) |
| **RISK** | Codex의 파괴적 작업(git push, 브랜치 삭제 등) 오남용 |
| **SUCCESS** | 신규 기능 1건을 파이프라인으로 완료 + 설계-구현 일치율 90% 이상 + 빌드/테스트 통과 |
| **SCOPE** | 1 feature 단위 순차 진행 (병렬 위임 없음) |

---

## 1. Overview

### 1.1 Purpose

Claude Code와 Codex CLI가 각자의 강점(Claude: 대화형 요구사항 정리·문서화·리뷰 / Codex: 자율 코드 실행)을 살려 문서를 인터페이스로 협업하는 개발 절차를 확립한다.

### 1.2 Background

- Codex CLI가 로컬에 설치되어 있고(`codex-cli 0.146.0`), `codex exec`을 통한 비대화형 실행이 가능함을 확인함 (`codex doctor` 정상, read-only exec 테스트 성공)
- 이 프로젝트는 이미 `CLAUDE.md`(Claude용)와 `AGENTS.md`(Codex/bkit용) 두 개의 에이전트 설정 파일과 `docs/01-plan~04-report` PDCA 문서 구조를 갖추고 있어, 문서 기반 협업의 토대가 이미 마련되어 있음

### 1.3 Related Documents

- `docs/MVP_SCOPE.md`, `docs/API_SPEC.md`
- `CLAUDE.md`, `AGENTS.md`

---

## 2. Scope

### 2.1 In Scope

- [ ] Claude: 기능 요구사항을 `docs/02-design/{feature}.md`로 설계 문서화
- [ ] Claude → Codex: `codex exec -s workspace-write "docs/02-design/{feature}.md 기준으로 구현"` 형태로 구현 위임
- [ ] Claude: 구현 결과를 `git diff`로 확인, 설계 문서와 대조, 빌드/테스트 실행
- [ ] Claude: 불일치 발견 시 `docs/03-analysis/{feature}.md`에 gap 기록 후 Codex에 재작업 지시
- [ ] Claude: 완료 시 `docs/04-report/{feature}.md`로 결과 요약

### 2.2 Out of Scope

- Codex의 git push / force-push / 브랜치 삭제 등 파괴적 git 작업 자동 실행
- 여러 기능을 동시에 Codex에 병렬 위임하는 것 (1 feature 단위 순차 진행만 다룸)
- CI/CD 파이프라인 자동화 (별도 과제)

---

## 3. Requirements

### 3.1 역할/책임 (Functional Requirements)

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | Claude는 구현 전 반드시 설계 문서를 작성/갱신한다 | High | Pending |
| FR-02 | Codex 실행은 `codex exec`로 Claude가 트리거하며, 프롬프트는 설계 문서 경로를 참조한다 | High | Pending |
| FR-03 | Claude는 Codex 산출물을 `git diff` + 빌드/테스트로 검증한다 | High | Pending |
| FR-04 | 파괴적 작업(push, force-push, branch delete, reset --hard 등)은 Codex가 자동 실행하지 않고 사용자 승인을 거친다 | High | Pending |
| FR-05 | 검증 결과(일치/불일치)는 PDCA 문서(`03-analysis`, `04-report`)에 기록한다 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Traceability | 모든 Codex 구현은 대응하는 설계 문서가 존재해야 함 | 문서-커밋 매핑 확인 |
| Safety | Codex는 기본적으로 `workspace-write` 샌드박스로 제한, 승인 없는 확장 금지 | `codex exec` 옵션 점검 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [ ] 신규 기능 1건을 이 파이프라인(설계→Codex 구현→Claude 검증)으로 처음부터 끝까지 완료
- [ ] 설계-구현 gap 분석 결과 90% 이상 일치 (미달 시 반복 수정)
- [ ] `./gradlew build`, `./gradlew test` 통과
- [ ] 절차를 `CLAUDE.md`/`AGENTS.md`에 반영해 재사용 가능하게 문서화

### 4.2 Quality Criteria

- [ ] Codex가 생성한 코드가 `CLAUDE.md`의 레이어드 아키텍처/네이밍 컨벤션을 준수
- [ ] 빌드/테스트 실패 시 자동으로 다음 단계로 넘어가지 않음

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Codex가 설계 문서 범위를 벗어나 과도한 변경을 수행 | Medium | Medium | 1 feature 단위로 좁게 위임, 매 실행 후 diff 리뷰 |
| Codex의 파괴적 git 명령 실행 | High | Low | `workspace-write` 샌드박스 사용, push/reset 등은 수동 승인 |
| 설계 문서와 실제 구현의 지속적 불일치 | Medium | Medium | gap 90% 미만 시 반복 수정 루프 강제 |

---

## 6. Next Steps

1. [x] 파일럿 기능 선정 — Phase 2 UX 개선(F-05/F-15/F-17), `docs/02-design/features/insight-comment.md` §⑩ (2026-08-07)
2. [x] 해당 기능의 설계 문서를 `docs/02-design/`에 작성 — 완료 (2026-08-07)
3. [x] `codex exec`로 구현 위임 후 결과 검증 — **첫 파일럿 실행, 실패로 종료(2026-08-07)**. 아래 §7 참조
4. [x] `CLAUDE.md`/`AGENTS.md` 갱신 여부 — **결정: 지금은 하지 않음 (2026-08-07, 사용자 결정)**. `danger-full-access`를 프로젝트 기본 운영 방식으로 문서화하면 "기본은 안전(workspace-write), 확장은 예외 승인" 원칙이 사실상 폐기되고 앞으로의 모든 세션에 영구 적용되는 정책 변경이라 보류. 매 실행마다 개별 승인을 받는 현재 방식을 유지한다. §7.3에 결정 배경 기록

---

## 7. 첫 파일럿 실행 결과 (2026-08-07)

- **명령**: `codex exec -s workspace-write "docs/02-design/features/insight-comment.md ⑩ 섹션 기준 구현"`
- **결과**: 구현 실패, 코드 변경 0건. `git status`/`git diff`로 대상 6개 파일 무변경 확인
- **원인**: `codex doctor`는 `filesystem sandbox: restricted`(workspace-write 정상 선언)로 보고하지만, 실제 하위 프로세스가 파일 쓰기 시도 시 전부 `Access denied`(`apply_patch`, `git apply`, 직접 `WriteAllText` 모두 동일 실패). 대상 파일 ACL은 `Authenticated Users:(M)` 쓰기 권한 보유 확인됨(Claude/사용자 계정은 정상 쓰기 가능) — Codex 샌드박스 하위 프로세스가 제한된 토큰으로 실행되어 해당 권한을 상속받지 못하는 것으로 추정(Windows 샌드박스 구현 이슈로 추정, 미확정)
- **Codex의 대응은 적절했음**: 실패를 숨기거나 거짓 성공 보고 없이 정확히 보고, `git add/commit/push` 등 부작용 없음 확인
- **시사점**: `1.2 Background`의 "read-only exec 테스트 성공" 확인만으로는 부족했음 — write 경로(workspace-write)는 별도 검증이 필요했음. 이 프로젝트에서 Windows 환경의 Codex `workspace-write` 샌드박스는 **아직 실사용 가능 여부 미검증 상태**로 재분류
- **미결 사항**: `danger-full-access` 전환 여부는 사용자 승인 필요(§3.2 Safety NFR과 상충하는 확장이므로 임의 실행하지 않음)

### 7.1 재시도 결과 (2026-08-07, 사용자 승인 후 `danger-full-access`)

- **명령**: `codex exec -s danger-full-access "..."` (동일 설계 문서, git add/commit/push 금지 명시)
- **결과**: **성공.** 대상 6개 파일(`InsightCommentRepository.java`, `InsightDetailService.java`, `InsightCommentDTO.java`, `insight-detail.html`, `insight-detail.js`, `insight-detail.css`) + 테스트 2개(`InsightDetailServiceTest.java` 신규, `InsightPageControllerTest.java` 수정) 변경. `git add/commit/push` 미실행 확인, 리포 밖 파일 미변경 확인
- **Claude 독립 검증**: `git diff` 전체 리뷰 완료 + `./gradlew test --rerun` 직접 실행(Codex 자체 보고를 그대로 신뢰하지 않음) — **68 tests, 0 failures, 0 errors**, Codex 자체 보고(68건 통과)와 일치
  - 첫 검증 시도는 `BUILD FAILED`(1초 만에 실패) — 원인은 코드 문제가 아니라 기본 `JAVA_HOME`이 Java 8이라 Spring Boot 3.2 플러그인 해석 실패. `JAVA_HOME`을 JDK 21로 지정 후 재실행하여 정상 확인(Codex도 동일 문제를 겪고 스스로 JDK 21로 전환했다고 보고함 — 이 환경의 알려진 특성으로 별도 기록)
- **설계 문서(`insight-comment.md` §⑩) 수용 기준 7개 전부 충족 확인**(코드 diff 직접 대조)
- **결론**: `codex-collab-workflow.md` 파이프라인(Claude 설계 → Codex 구현 → Claude 검증) 첫 성공 사례. 단, Windows 환경에서는 `workspace-write`가 아니라 `danger-full-access`가 필요했다는 점이 이 프로젝트의 기본 운영 방식으로 굳어질지는 별도 결정 필요(§7의 원인 미확정 문제와 연결 — 근본 원인 조사 없이 `danger-full-access`를 기본값으로 굳히면 안전장치 없이 운영하는 셈)

### 7.2 두 번째 파일럿 (2026-08-07, F-03 예외 처리 표준화)

- **명령**: `codex exec -s danger-full-access "docs/02-design/features/exception-handling-policy.md 기준 구현"`
- **대상**: `AdminPageController.java`(POST 21곳을 `executeAdminAction` 헬퍼로 통합), `MyPageController.java`(로컬 `@ExceptionHandler` 추가) — 사전에 `git diff`로 baseline을 떠서 이미 존재하던 무관한 수정사항과 Codex의 신규 변경을 구분함
- **결과**: 성공. 설계 문서 수용 기준 6개 전부 충족(코드 diff 직접 대조), 71 tests 통과(Claude 독립 재실행 확인). 설계에 없던 4개 호출부(조건부 성공 메시지)를 Codex가 합리적으로 확장 처리 — 검토 후 승인
- **시사점**: 두 번째 파일럿도 `danger-full-access` 필요, `workspace-write` 문제가 일회성이 아니라 이 환경의 일관된 특성임을 재확인. baseline diff 기록 방식이 "이미 더러운 워킹 디렉토리에서 Codex 변경분만 분리 검증"하는 데 유효함을 확인(향후 파일럿에도 재사용 권장)

### 7.3 `danger-full-access` 기본값 전환 여부 — 보류 결정 (2026-08-07)

- **논의 배경**: 2회 연속 파일럿이 전부 `danger-full-access`로만 성공해, 매번 개별 승인받는 대신 `CLAUDE.md`/`AGENTS.md`에 "이 프로젝트에서 Codex는 `danger-full-access`로 실행한다"고 기본값을 못박을지 사용자에게 확인함
- **결정: 지금은 하지 않는다.** 이유:
  - `workspace-write`(기본 안전) → `danger-full-access`(제한 없음)로의 전환은 **일회성 승인이 아니라 앞으로의 모든 세션에 영구 적용되는 안전 정책 변경**이라, 매번의 실행 승인과는 무게가 다름
  - 지금까지의 안전장치는 "OS가 강제하는 workspace-write 경계"였는데, `danger-full-access`에서는 프롬프트 내 지시("git commit/push 금지" 등)에만 의존하게 됨 — Codex가 지시를 잘 따른 것은 맞지만, 이것이 구조적 안전장치를 대체할 근거는 아님
  - `workspace-write`가 왜 이 Windows 환경에서 막히는지 근본 원인이 아직 미확정(§7 참조) — 원인 규명 없이 우회를 기본값으로 굳히면 향후 원인이 해결되어도 다시 안전한 기본값으로 돌아갈 유인이 사라짐
- **당분간 운영 방식**: 매 `codex exec` 실행마다 개별적으로 사용자 승인을 받고, baseline diff 기록(§7.2 시사점)으로 변경 범위를 분리 검증하는 현재 절차를 유지한다
- **재검토 트리거**: (1) `workspace-write` 실패의 근본 원인이 파악되거나, (2) Codex 파일럿 빈도가 늘어 매번 승인받는 비용이 커지면 재논의

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-03 | 초안 작성 | Claude (대화 기반) |
| 0.2 | 2026-08-07 | 첫 파일럿 실행(Feature A: insight-comment §⑩) — Windows 샌드박스 쓰기 차단으로 실패, 원인·시사점 §7에 기록. Next Steps 갱신 | Claude (파일럿 진행·검증) |
| 0.3 | 2026-08-07 | `danger-full-access` 재시도 성공(§7.1) — 6개 대상 파일 구현 완료, Claude 독립 검증(`git diff`+`gradlew test`)으로 68건 전부 통과 확인. 파이프라인 첫 성공 사례로 기록. JDK 8/21 혼재 환경 특성 기록 | Claude (검증) |
| 0.4 | 2026-08-07 | 두 번째 파일럿 성공(§7.2, exception-handling-policy) — Admin 21개 호출부 통합 + 마이페이지 방어 핸들러, 71 tests 통과 확인. `danger-full-access` 필요성이 일회성이 아님을 재확인. `CLAUDE.md`/`AGENTS.md` 갱신 여부는 사용자 결정 대기로 Next Steps #4 갱신 | Claude (파일럿 진행·검증) |
| 0.5 | 2026-08-07 | §7.3 추가 — `danger-full-access` 기본값 전환 여부 논의 결과 **보류 결정** 기록(영구 정책 변경이라 근본 원인 규명 전까지 매 실행 개별 승인 유지). Next Steps #4 완료 처리(결정 자체는 완료, 실행은 보류) | Claude (결정 기록) |
