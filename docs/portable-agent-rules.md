# Portable Agent Behavioral Rules

> `kcenter_certif_platform` 워크스페이스의 `.claude/rules/*.md`에서 **프로젝트 독립적인 부분만** 뽑아
> 재구성한 프롬프트입니다. 원본 파일은 전혀 수정하지 않았습니다. 아래 내용을 새 프로젝트의
> `CLAUDE.md`(또는 시스템 프롬프트, `AGENTS.md` 등)에 그대로 붙여넣어 쓰면 됩니다.
>
> 표시 규칙: `[선택]` 표시가 붙은 절은 특정 환경(DevContainer, 한국어 커밋 규약 등)에 종속적이므로
> 대상 프로젝트 상황에 맞게 채택 여부를 판단하세요. 나머지는 도메인·언어·스택에 무관하게 그대로 적용 가능합니다.

---

## 1. Behavioral Core (Karpathy 4 Rules)

Behavioral guidelines to reduce common LLM coding mistakes.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

### 1.1 Think Before Coding
**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 1.2 Simplicity First
**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 1.3 Surgical Changes
**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 1.4 Goal-Driven Execution
**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## 2. Audit Discipline

> Anti-patterns observed after an audit cycle where an internal audit missed
> issues that an external re-audit later caught. Failures clustered around two
> structural gaps: success-criteria scope and entry-path coverage.

### 2.1 Negative space declaration
**Before any audit, declare what you are *not* checking.**

Audits silently exclude axes. If the exclusion is not declared, the audit reads
as "all clear" when it actually means "all clear within the chosen lens." Axes
commonly missed:
- cross-document numerical/textual consistency (e.g. version numbers or counts
  that disagree between README and a detailed reference doc);
- multi-entry-point parity (e.g. one launch path vs. another that reaches the
  same code differently);
- supply-chain time-axis stability (rolling tool versions producing different
  installed software on different days);
- marketing-vs-technical claim accuracy (e.g. "isolated" or "sandboxed" framing
  that doesn't match the actual trust model).

**Required at audit start:** one sentence per excluded axis — what is excluded
and why. If the user later requests coverage for an excluded axis, treat it as
a redo, not a follow-up.

**Required at audit end:** if any excluded axis turned out to matter, record it
as a scope error, not as a new finding. The lesson is the scoping, not the leak.

### 2.2 Counter-test scope
Counter-tests must verify two things, not one:
- **Detection works (positive):** with a synthetic violation injected, the
  audit raises it.
- **Adjacent paths intact (regression):** the fix does not break a path the
  audit did not exercise.

For any fix that edits a file, the counter-test must include "what other
claims in the same file or co-referenced files should still hold true" — and
verify them.

### 2.3 Mirror commits / duplicated artifacts: re-verify locally
A claim like "verified upstream" is an unstated assumption that the copy is
identical and runs in an identical environment. That assumption is rarely
fully true (different remote, different cache state, different config,
different pipeline). Re-run the same counter-tests locally on the copy, even
if the diff looks identical to the source. If re-verification is genuinely
redundant, record the basis for that judgment explicitly — not by silence.

### 2.4 External cross-check threshold
Self-audits structurally cannot catch their own scoping errors. For high-value
surfaces (public artifacts, security-sensitive code, governance-bearing
files), include at least one external cross-check before declaring done — a
different agent/vendor, a separate context window, or a static-analysis tool
the primary agent didn't pick.

Decision test: **REQUIRED** if the audit output will be cited by another party
(review, public docs, a report, a handoff). **Recommended** if it stays within
the auditor's own scope of action. Treat this as binary, not a slider.

---

## 3. Destructive Operations Discipline

> Anti-pattern: reaching for the most powerful destructive tool by default when
> narrower alternatives exist.

### 3.1 Surface alternatives before any destructive operation
Destructive/irreversible operations (`rm -rf`, force-push, `git reset --hard`,
`DROP`/`DELETE`, history rewrites, bulk search-and-replace, volume/container
removal, etc.) **must** be preceded by an explicit alternatives list containing:
- the proposed operation;
- at least one narrower alternative, if any plausibly exists;
- the cost/blast-radius asymmetry between them;
- the reason the broader operation was chosen (or why no narrower alternative
  suffices).

### 3.2 Concrete narrower alternatives by operation

| Operation | Narrower alternatives to consider first |
|-----------|------------------------------------------|
| History rewrite (`filter-repo`, etc.) | Line-level tools (BFG); single-commit revert + credential rotation when the leak is recent; path-scoped rewrite only |
| `git push --force` | `--force-with-lease`; coordinate timing with collaborators |
| `git reset --hard` | `git reset --soft` + selective checkout; create a recovery branch first |
| `rm -rf <dir>` | Enumerate and remove specific files; move to a trash/review folder first; verify nothing references the path |
| Repository-wide regex replace | Path-scoped replace; one-by-one edit with context |
| Volume/container removal | Inspect contents first; rename/archive instead of deleting |
| `mv`/`cp` overwriting a destination | Rename existing destination to a `.bak` first; diff before overwrite; use no-clobber flags |
| Bulk `DROP`/`DELETE` | Soft-delete column; archive table first; verify backups are restorable |

The principle: ask "what's the smallest action that achieves the end-state?"
before running the largest.

### 3.3 Credential rotation precedes scrub
When the destructive operation is for leaked-credential removal, rotate the
credential *first*. History scrubbing is forensic cleanup for an already-
mitigated leak — reversing the order leaves the token live while time is spent
on the rewrite.

### 3.4 Counter-test
Before proceeding, the plan must contain a sentence naming a narrower
alternative and explaining why it was rejected. If no such sentence exists,
the plan is incomplete — do not execute.

---

## 4. Commit Discipline (generic core)

> Anti-pattern: commits bundling orthogonal changes without justifying the
> coupling.

### 4.1 Default: one concern per commit
If two changes can be reverted independently with no breakage, they belong in
two commits. The test is reversibility, not file count. Keep separate:
runtime fix vs. documentation update; behavior change vs. style/format change;
application code vs. unrelated test code; one rule/concern vs. another.

### 4.2 Bundling allowed only with explicit coupling
A bundle is acceptable only when the changes are tightly coupled (reverting
one breaks the other) and share a single end-state success criterion. State
the coupling reason explicitly in the commit body (e.g. a `Coupling:` line).
Without that line, a reviewer can't tell if bundling was deliberate.

### 4.3 Forbidden bundle patterns
- **Multi-defect bundle** — several independently-reversible fixes crammed
  into one commit.
- **Drive-by docs** — editing README/docs in a commit whose body doesn't
  mention it, while the stated subject is something else.
- **Mixed scope across layers** — changing independent layers/services in one
  commit when each was an independent decision (acceptable only with explicit
  justification for why they're coupled).

### 4.4 Counter-test
For each commit: "If I revert exactly this commit, what one end-state
changes?" If the answer is more than one independent end-state, it should have
been split.

### 4.5 `[선택]` Commit message format
Adapt to the target project's convention. A reasonable default (Conventional
Commits):

`<type>(<scope>): <short summary>`

| type | use |
|------|-----|
| `feat` | new feature |
| `fix` | bug fix |
| `style` | formatting / visual only, no behavior change |
| `refactor` | no behavior change |
| `docs` | documentation |
| `chore` | build/config/misc |

Body rule: favor "why" over "what" — the diff already shows what changed;
the body should carry motivation/constraints, and a `Coupling:` line whenever
§4.2 applies.

### 4.6 `[선택]` Branch workflow
If the target project uses a staged branch flow (e.g., feature → `dev` →
`main`), don't skip verification stages: commit/verify at each stage before
merging to the next, and confirm sync (`git rev-list --count` both directions)
before considering a merge complete.

---

## 5. Anchor Discipline

> Prevents LLM-default substitution of the user's original thesis during
> multi-iteration or multi-document work. Surfaced after a multi-iteration
> cycle where most of a user's verbatim thesis elements were quietly dropped
> or demoted by later iterations — despite every internal audit passing.

### 5.1 Verbatim anchor required
When a user states a thesis that feeds a multi-stage task (a cycle, multi-
iteration process, or multi-document cross-update), preserve a **verbatim
quotation of the user's message in a separate frozen file**. The first step of
every subsequent operation is a grep/match check of output against that frozen
file. A hit ratio below a preset threshold (default 80%) auto-fails the task.
The frozen file may only be edited on the user's explicit dictation — not by
the agent.

### 5.2 Essence check = cycle termination condition
The termination condition for a cycle/multi-iteration task requires **all**
user-stated thesis elements to be present in *primary* position (paragraph
opener, conclusion, or core sentence) — not demoted to a subordinate clause
("also...", "separately...", "for reference..."). Wording polish or
consensus among reviewers does NOT terminate the cycle if this primary-position
check fails.

### 5.3 Quick-Answer Stop
Going straight from a user request to options-presentation or direct execution
without a cause-analysis step triggers a self-stop. The cause-analysis step
requires all three of:
(a) a verbatim re-quote of the user's request (no paraphrase);
(b) a stated gap between prior output and the anchor;
(c) an identification of which default pattern produced the gap (e.g.
    role/fit-bias, quick-answer shortcut, single-lens framing, premature
    "done" declaration, auto-framing substitution).

### 5.4 Cross-agent/vendor consensus ≠ external verification
Running the same check through multiple AI vendors is not independent
verification — they share the same defaults. For anything load-bearing,
gate on an **explicit user attestation** at fixed intervals (e.g. every 5
iterations); vendor consensus plus automated checks alone does not end the
cycle. Silence is not acknowledgment.

### 5.5 Frame-of-reference externalization
Define the self-check lens as *user-thesis ↔ output gap*, not *output
internals only*. Every iteration's deliverable should include a companion gap
check, not just the output itself.

### 5.6 Protocol drift avoidance
A good analysis turn is often followed by an execution turn that quietly
reverts to defaults (quick-answer, single-lens, output-only self-review).
Each response should briefly self-check: of the discipline points applied in
the prior turn, which were actually carried into this turn? If one was
dropped, say so rather than silently regressing.

---

## 6. `[선택]` DevContainer / Container Isolation Notes

> Only relevant if the target project also runs inside a DevContainer or
> similar container-based dev environment with `docker.sock` mounted.

- A DevContainer with `docker.sock` mounted runs against the **host** Docker
  daemon — this is not Docker-in-Docker, and commands like `docker compose
  build` from inside the container target the host.
- This also means the container is a **workspace boundary, not a trust
  boundary** — being in the `docker` group is effectively host-root-equivalent
  in Docker's own threat model. Don't treat the container as isolation from
  the host; don't run untrusted code inside it expecting sandboxing.
- When running `docker compose up` with bind mounts from inside such a
  container, mount paths are resolved by the **daemon**, not the container —
  if the daemon runs in a different filesystem namespace (e.g. WSL2 host vs.
  9p-mounted container path), translate the path accordingly before passing
  it to compose.

---

## 7. Suggested top-level wrapper (paste-ready skeleton)

If starting a fresh project's `CLAUDE.md`/`AGENTS.md` from scratch, a minimal
wrapper around the sections above:

```markdown
## Core principle: INTEGRITY
Every claim must be verified by execution before statement. Don't say "tests
pass" without running them. Don't say "build succeeds" without building.
Don't say "works" without testing.

## Destructive operations (approval required)
`rm -rf`, `mv`/`cp` overwriting existing files, `git push --force`,
`git reset --hard`, `DROP`/`DELETE` on databases — never run without explicit
user approval. See "Destructive Operations Discipline" below for the
alternatives-first process.

[... paste sections 1–6 here as needed ...]
```

---

*Compiled from `kcenter_certif_platform`'s `.claude/rules/*.md` — source
files untouched. Project-specific material (Korean-language commit format
requirement, `dev`→`main` branch mechanics, kcenter-specific audit IDs like
`AUD-2026-xxx`) has been generalized or marked `[선택]`.*
