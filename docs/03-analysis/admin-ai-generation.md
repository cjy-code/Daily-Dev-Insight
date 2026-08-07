# AI 생성 관리 Gap Analysis

> **Project**: dailyDevInsight
> **Date**: 2026-08-06 (v0.2: Codex 검증 반영 재작성)
> **대상 문서**: `docs/01-plan/features/admin-ai-generation.md`, `docs/02-design/features/admin-ai-generation.md`, `docs/02-design/screens/admin-ai-generation.md`
> **대상 코드**: `DailyKnowledgeGenerationService`(전체), `AdminPageController`, `admin.js`, `admin-generation-compose.js`(전체), 템플릿 2종

---

## 검증 범위 선언 (Audit Discipline)

- **검증한 것**: 코드 정독 + `codex exec -s read-only` 교차검증(서비스 전체, JS 전체 포함해 재확인 요청)
- **검증하지 않은 것**: `./gradlew test` 실행, 브라우저 실동작

> **v0.1 이력**: v0.1은 "Match Rate 확정을 Codex 검증 대기로 보류"하는 방식을 취했다(Unit 2·8의 "자체 100% 오판정" 재발 방지 목적). 실제로 이 판단은 유효했다 — Codex 검증에서 **핵심 서술("두 버튼 병존") 자체가 틀렸음**이 드러났기 때문이다.

---

## 1. 핵심 발견: Plan/Design의 근본 전제 오류

1차 사이클 compact card부터 2차 사이클 자체 검증까지 "즉시 생성(`/admin/generate`)과 미리보기(`/admin/generate/preview`+`save`)가 의도적으로 병존하는 두 경로"라고 판단했다. **Codex 검증 결과 이는 틀렸다**:

- `admin/generation.html`의 실제 생성 버튼은 **"지금 생성" 하나뿐**이고 `type="button"`
- 클릭 시 **항상** `window.open()`으로 compose 창을 여는 코드(`admin.js`)만 존재
- `POST /admin/generate`를 호출하는 폼 submit이나 JS는 화면 어디에도 없음
- 즉 `/admin/generate`(컨트롤러 코드)는 **현재 UI에서 도달 불가능한 고아 엔드포인트**

이 오류는 Plan 문서의 FR-02("수동 즉시 생성은 검증 후 바로 저장")를 "Done"으로 표기한 근거 자체를 흔든다 — **엔드포인트는 구현되어 있으나 실제 사용 경로가 없다.**

---

## 2. Plan(FR) 재평가

| ID | Requirement | 재평가 |
|----|-------------|-----------|
| FR-01 | 예약 생성 스킵+이력 | ✅ 일치(변동 없음) |
| FR-02 | 수동 즉시 생성 | ⚠️ **코드는 구현되어 있으나 UI 경로 없음 — "Done"이 아니라 "구현되었으나 미연결(orphaned)"로 재분류** |
| FR-03 | 미리보기 미저장+비교 | ✅ 일치 |
| FR-04 | 저장은 재생성 없음 | ✅ 일치(단, 같은 날짜는 upsert라는 사실 추가) |
| FR-05 | 활성 템플릿 없으면 생성 불가 | ❌ **불일치** — 저장은 막히지만 미리보기는 LLM 호출이 활성 템플릿 조회보다 먼저 실행됨 |
| FR-06 | 성공/스킵/실패 이력 구분 기록 | ⚠️ **부분 일치** — 검증 실패·활성 템플릿 조회 실패·이미지 새로고침 실패는 FAILED 이력으로 남지 않음 |

**재평가 결과: 완전 일치 3 / 부분·조건부 일치 2 / 불일치 1 (총 6개 중)**

---

## 3. Design/Screen 문서 오류 종합

| 항목 | 최초 서술 | Codex 검증 결과 |
|---|---|---|
| 생성 버튼 구성 | "두 버튼 병존" | ❌ 버튼 1개, 항상 compose 오픈 |
| 이미지 생성 fail-soft | "완전히 fail-soft" | ⚠️ 구현체(`OpenAiImageGenerationClient`) 의존적, 서비스 계약 아님 |
| 프롬프트 텍스트 수정 가능 여부 | "읽기 전용에 가까움" | ❌ 실제로는 `textarea`로 직접 수정 가능 |
| 저장 후 부모창 처리 | "안 읽음, 불명" | ✅ 확정됨(reload+close, postMessage 없음) |
| 같은 날짜 재저장 시 동작 | 언급 없음 | 신규: upsert(덮어쓰기) |
| 콘텐츠·이력 저장 트랜잭션 | 언급 없음 | 신규: 트랜잭션 미적용, 부분 실패 가능 |
| `GenerationHistory` errorCode | 언급 없음 | 신규: 별도 컬럼 없이 문자열 결합 |
| 서비스 테스트 커버리지 | "확인 안 됨" | 확정: 예약 중복 정책 2건뿐 |

모든 항목은 Design/Screen 문서 v0.2→v0.3(Design), v0.1→v0.2(Screen)에서 정정 완료.

---

## 4. 종합 판정

**"Match Rate"를 산식으로 표시하지 않는다.** 이번 사례의 핵심은 숫자가 아니라 **"핵심 전제 자체가 틀렸다"**는 것이다 — 부분적으로 몇 % 맞았는지보다, **"두 경로가 의도적으로 병존한다"는 서사 전체가 재구성되어야 했다**는 사실이 더 중요하다. Unit 2·8과 달리 이번엔 세부 항목의 오류가 아니라 **문서의 중심 주장 자체가 뒤집힌 사례**로 기록한다.

---

## 5. Known Gaps와의 구분

Design 문서 §8에 기록된 항목(고아 엔드포인트, fail-soft 미보장, 트랜잭션 부재, 테스트 공백 등)은 불일치가 아니라 후속 개선 과제. 단 **"`/admin/generate` 고아 엔드포인트"는 버그/청소 대상인지, 의도적으로 남긴 것인지 정책 확인이 우선**이라는 점에서 다른 Known Gap과 성격이 다르다.

---

## 6. 미확인 리스크

- `./gradlew test` 미실행
- `/admin/generate`가 정말 아무 곳에서도 호출되지 않는지 전체 코드베이스 재검색은 안 함(admin.js/generation.html 범위 내에서만 확인)
- 브라우저 실동작(팝업 차단, reload 타이밍 등) 미확인

---

## Version History

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-08-06 | 최초 Gap 분석 — Match Rate 확정 보류(Codex 검증 대기로 명시) |
| 0.2 | 2026-08-06 | Codex 교차검증 반영 전면 재작성 — **"두 버튼 병존" 핵심 전제 오류 확인 및 정정**, FR-02 재분류(구현됨→미연결), FR-05 불일치로 재평가, Design/Screen 오류 8건 종합 |
