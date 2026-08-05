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

1. [ ] 파일럿 기능 선정 (기존 MVP_SCOPE 중 미구현 항목 또는 소규모 개선 1건)
2. [ ] 해당 기능의 설계 문서를 `docs/02-design/`에 작성
3. [ ] `codex exec`로 구현 위임 후 결과 검증
4. [ ] 결과를 바탕으로 워크플로우 보완 및 `CLAUDE.md`/`AGENTS.md` 갱신

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-03 | 초안 작성 | Claude (대화 기반) |
