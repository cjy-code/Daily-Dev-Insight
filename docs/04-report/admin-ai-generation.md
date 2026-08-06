# AI 생성 관리 PDCA 완료 보고서

> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **관련 문서**: Plan/Design/Screen/Analysis (Unit 13, §2.4 승격 후보 정밀화)

---

## 1. 요약

`admin-ai-generation`(Unit 13) 기능은 1차 사이클 compact card에서 "즉시생성/미리보기 두 플로우가 왜 공존하는지 불명"으로 남겨뒀던 질문을, 2차 사이클 자체 검증에서 **"의도된 병존"으로 잘못 결론 내렸다가 Codex 교차검증에서 다시 뒤집힌 사례**다. 실제로는 화면에 생성 버튼이 하나뿐이고 항상 미리보기(compose) 경로로만 이어지며, `/admin/generate`(즉시 생성) 엔드포인트는 현재 UI에서 호출되지 않는 고아 코드로 추정된다.

## 2. 진행 경과

| 단계 | 산출물 | 상태 |
|---|---|---|
| Plan | `docs/01-plan/features/admin-ai-generation.md` | ✅ 완료 |
| Design | `docs/02-design/features/admin-ai-generation.md` (v0.1→v0.2 확장) | ✅ 완료 |
| Screen | `docs/02-design/screens/admin-ai-generation.md` | ✅ 완료 |
| Analysis | `docs/03-analysis/admin-ai-generation.md` | ✅ 완료(Match Rate 확정 보류) |
| Report | 본 문서 | ✅ 완료 |
| Codex 교차검증 | — | 진행 예정 |

## 3. Gap 분석 결과

**Unit 2·8 선례를 반영해 자체 검증 단계에서 Match Rate 숫자를 확정하지 않았던 판단이 이번엔 특히 유효했다.** Codex 교차검증 결과 "즉시생성/미리보기 두 버튼 병존"이라는 **핵심 전제 자체가 틀렸음**이 드러나, 단순 세부 오류가 아니라 문서의 중심 서사를 재구성해야 했다. 상세는 `docs/03-analysis/admin-ai-generation.md` v0.2 참조.

## 4. 이번 사이클에서 확정/정정된 사실

**자체 검증 단계에서 확정(이후 유지)**:
- 저장은 재생성 없이 미리보기 시점의 텍스트를 그대로 사용(readonly 필드)
- 미리보기 경로는 `LlmClientException.getUserMessage()`를 실제로 사용 — weekly-ai-insight Design 문서가 지적한 "getUserMessage 미사용" 문제를 이 unit은 이미 회피

**Codex 교차검증에서 뒤집히거나 신규 발견된 것**:
- ~~즉시생성/미리보기가 의도적으로 병존~~ **[정정] 생성 버튼은 하나뿐이고 항상 미리보기(compose)로 이어짐. `/admin/generate`는 UI에서 호출되지 않는 고아 엔드포인트로 추정**
- ~~이미지 생성은 완전히 fail-soft~~ **[정정] 현재 구현체(`OpenAiImageGenerationClient`)가 우연히 예외를 내부에서 잡아주는 것일 뿐, 서비스 레이어가 보장하는 계약이 아님**
- ~~프롬프트 미리보기는 읽기 전용에 가까움~~ **[정정] 프롬프트/이미지프롬프트 textarea는 실제로 관리자가 직접 수정 가능**
- 미리보기는 LLM 호출이 활성 템플릿 조회보다 먼저 실행됨(활성 템플릿 없이도 LLM 비용 발생 가능)
- 같은 날짜 재저장은 upsert(기존 row 덮어쓰기)
- 콘텐츠 저장과 이력 저장이 트랜잭션으로 묶여있지 않아 부분 실패 가능
- 저장 성공 후 부모창은 `location.reload()` + compose 창 `close()`, `postMessage` 없음(최초엔 "불명"으로 남겼던 것 확정)
- 서비스 레벨 테스트는 예약 중복 정책 2건뿐(수동/미리보기/이미지실패/FAILED 이력 미검증)

## 5. Known Gaps (후속 작업 후보)

| 우선순위(제안) | 항목 |
|---|---|
| High | `/admin/generate` 고아 엔드포인트 — 삭제 대상인지 의도적으로 남긴 것인지 정책 확인 필요 |
| Medium | 이미지 생성 fail-soft를 서비스 계약으로 명시적으로 보장하도록 개선 |
| Medium | 미리보기의 LLM 호출/활성 템플릿 조회 순서 변경(비용 절감) |
| Medium | 콘텐츠·이력 저장 트랜잭션 처리 |
| Medium | 서비스 레벨 테스트 확충(수동/미리보기/이미지실패/FAILED 이력) |
| Low | `GenerationHistory` errorCode 별도 컬럼화 |
| Low | 검증 로직(`validateManualRequest`/`validatePreviewRequest`/`validateSaveRequest`) 중복 → `/admin/generate` 정책 확정 후 공통화 재검토 |
| Low | 프롬프트 템플릿 "activate" vs "toggle-active" 의미 명확화 |
| Low | compose 새창의 팝업 차단 리스크 — 대안 UX 검토 |

## 6. Next Steps

- [x] Codex 교차검증 진행 — 완료, 핵심 전제 오류 정정 반영
- [ ] **`/admin/generate` 고아 엔드포인트 처리 방향을 사용자에게 확인 필요**(삭제 vs 유지 vs 재연결)
- [ ] Unit 2/8/13 3개 승격 후보 정밀화 완료 — **2차 사이클 종료, 전체 마무리 보고로 이동**

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | PDCA 완료 보고서 초안 작성 — Match Rate 확정 보류 | Claude (대화 기반) |
| 0.2 | 2026-08-06 | Codex 교차검증 결과 반영 — **"두 버튼 병존" 핵심 전제 오류 정정**(고아 엔드포인트로 재규명), fail-soft/프롬프트 수정가능여부/저장후 처리 등 정정 및 신규 발견 반영, Known Gaps 9개로 확장 | Claude (Codex `codex exec -s read-only` 검증 결과 반영) |
