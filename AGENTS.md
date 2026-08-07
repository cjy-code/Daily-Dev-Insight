# bkit Project Configuration

## Project Level

This project uses bkit with automatic level detection.
Call `bkit_detect_level` at session start to determine the current level.

### Level-Specific Guidance

**Starter** (beginners, static websites):
- Use simple HTML/CSS/JS or Next.js App Router
- Skip API and database phases
- Pipeline phases: 1 -> 2 -> 3 -> 6 -> 9
- Use `$starter` skill for beginner guidance

**Dynamic** (fullstack with BaaS):
- Use bkend.ai for backend services
- Follow phases: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 9 (phase 8 optional)
- Use `$dynamic` skill for fullstack guidance

**Enterprise** (microservices, K8s):
- All 9 phases required
- Use `$enterprise` skill for MSA guidance

## PDCA Status

ALWAYS check `docs/.pdca-status.json` for current feature status.
Use `bkit_get_status` MCP tool for parsed status with recommendations.

## Key Skills

| Skill | Purpose |
|-------|---------|
| `$pdca` | Unified PDCA workflow (plan, design, do, analyze, iterate, report) |
| `$plan-plus` | Brainstorming-enhanced planning (6 phases, HARD GATE) |
| `$starter` / `$dynamic` / `$enterprise` | Level-specific guidance |
| `$development-pipeline` | 9-phase pipeline overview |
| `$code-review` | Code quality analysis with static analysis patterns |
| `$bkit-templates` | PDCA document template selection |

## Response Format (MANDATORY)

### Starter Level (bkit-learning style)
ALWAYS include at the end of each response:
- **Learning Points**: 3-5 key concepts the user should learn
- **Next Learning Step**: What to study or practice next
- Use simple terms, avoid jargon. Use "Did you know?" callouts.

### Dynamic Level (bkit-pdca-guide style)
ALWAYS include at the end of each response:
- **PDCA Status Badge**: `[Feature: X | Phase: Y | Progress: Z%]`
- **Checklist**: What's done and what remains
- **Next Step**: Specific action with command/tool suggestion

### Enterprise Level (bkit-enterprise style)
ALWAYS include at the end of each response:
- **Tradeoff Analysis**: Pros/Cons of the approach taken
- **Cost Impact**: Development time, infrastructure cost, maintenance burden
- **Deployment Considerations**: Environment-specific notes

## Team Workflow (Single Agent Mode)

When working on complex features:
1. Break the task into PDCA phases (Plan -> Design -> Do -> Check -> Report)
2. For each phase, apply the relevant specialist perspective:
   - Plan: Product Manager + CTO perspective
   - Design: Architect + Security perspective
   - Do: Developer + Frontend/Backend perspective
   - Check: QA + Code Reviewer perspective
   - Report: Documentation perspective
3. Use `bkit_pdca_next` to transition between phases
4. Quality gates: Each phase must be documented before proceeding

## Verification Discipline (Audit + Anchor)

> `docs/portable-agent-rules.md`의 Audit Discipline(§2)/Anchor Discipline(§5)에서
> Claude+Codex 교차검증 워크플로우에 실제로 적용 가능한 부분만 뽑아 압축. 전체 원문은 해당 파일 참고.

### 검증 범위 선언 (Audit)
- `codex exec`로 검증/리뷰를 수행할 때 **검사하지 않는 범위를 한 문장으로 먼저 밝힌다** (예: "테스트 실제 실행은 안 함").
- 밝히지 않은 범위에서 나중에 문제가 나오면 새 이슈가 아니라 **스코프 누락**으로 취급한다.
- 수정 후에는 같은 파일/연관 파일에서 함께 유지되어야 하는 다른 주장도 같이 확인한다.
- Claude가 작성한 설계/계획 문서를 검증할 때는 이것이 **외부 교차검증** 역할을 한다는 점을 인지하고, 코드와 문서 양쪽을 실제로 대조한다 (문서만 보고 그럴듯하다고 판단하지 않는다).

### 원본 취지 고정 (Anchor)
- Plan → Design → Report로 이어지는 작업에서 사용자의 원래 요구사항이 축소·누락되지 않았는지 확인한다.
- **Claude와 Codex가 합의했다고 검증이 끝난 게 아니다** — 실질적 영향을 주는 반영은 사용자 승인 후 진행한다.
- 파일 수정을 요청받지 않았다면 리뷰 코멘트만 반환하고 파일은 건드리지 않는다 (읽기 전용 검증과 수정 작업을 명확히 구분).
