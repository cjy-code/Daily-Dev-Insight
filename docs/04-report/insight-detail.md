# 인사이트 상세(좋아요·북마크·댓글) PDCA 완료 보고서

> **Project**: dailyDevInsight
> **Date**: 2026-08-06
> **관련 문서**: Plan/Design/Screen/Analysis (Unit 2, §2.4 승격 후보 정밀화)

---

## 1. 요약

`insight-detail`(Unit 2) 기능은 1차 사이클에서 compact card로 가볍게 문서화됐다가, §2.4 리스크 기반 승격 기준(6개 저장소 결합, Unit 3·4의 의존 대상)에 해당해 2차 사이클에서 Plan+Design+Screen+Analysis+Report 전체 세트로 정밀화되었다.

## 2. 진행 경과

| 단계 | 산출물 | 상태 |
|---|---|---|
| Plan | `docs/01-plan/features/insight-detail.md` | ✅ 완료 |
| Design | `docs/02-design/features/insight-detail.md` (v0.1→v0.2 확장) | ✅ 완료 |
| Screen | `docs/02-design/screens/insight-detail.md` | ✅ 완료 |
| Analysis | `docs/03-analysis/insight-detail.md` | ✅ 완료 |
| Report | 본 문서 | ✅ 완료 |

## 3. Gap 분석 결과

**[v0.1 정정]** 최초 자체 검증에서 "Match Rate 100%"로 판정했으나, **Codex 교차검증(`codex exec -s read-only`)에서 이 판정 자체가 오류였음이 드러남**: 정책 미결정 항목(FR-06)을 완료 항목으로 잘못 산입했고, escape 문자 수·SSR 대댓글 표시 여부·"모든 상호작용 재조회 통일" 등 실제로 코드와 다른 서술 3건을 교차검증 없이 "일치"로 넘겼었다. Codex 검증 결과를 반영해 Analysis 문서를 v0.2로 전면 재작성했고, 발견된 불일치는 모두 Design/Screen 문서에 정정 반영했다(자세한 내용은 `docs/03-analysis/insight-detail.md` v0.2 참조).

## 4. 이번 사이클에서 새로 확정/정정된 사실

**자체 검증(Codex 이전) 단계에서 확정된 것**:
- **다단 대댓글이 서버·프론트 양쪽에서 실제로 허용됨** — `insight-detail.js`의 `buildCommentHtml()`이 depth 제한 없이 재귀 렌더링하는 것을 코드로 확인. `MVP_SCOPE.md`의 "대댓글까지만" 기술과 실제 구현이 다름을 확정 (findings F-05, Medium→High로 재평가)

**Codex 교차검증에서 새로 발견/정정된 것**:
- ~~"좋아요/댓글 등 모든 상호작용이 재조회 경로로 통일"~~ **[정정] 좋아요/북마크는 카운트만 부분 갱신하며 재조회를 트리거하지 않음 — 재조회(`renderFromState`) 경로는 댓글 등록/삭제뿐**
- **SSR 초기 페인트는 최상위 댓글만 표시하고 대댓글은 전혀 렌더링하지 않음** (JS 재조회 후에야 표시) — JS 비활성 환경에서는 대댓글이 영구히 안 보임
- 조회수 세션 키가 `type` 대소문자를 정규화하지 않아 중복 증가 가능성
- 부모 댓글 삭제 시 자식 댓글이 최상위로 승격 표시(연쇄 삭제 아님)
- 댓글 escape 대상은 4개가 아니라 5개 문자(`&<>"'`)
- `getEngagementOnly()`가 실제로는 상세 전체를 반환 — 네이밍이 범위를 반영 못함
- 댓글 조회에 페이지네이션/depth 제한 없음(성능 리스크)

## 5. Known Gaps (후속 작업 후보)

| 우선순위(제안) | 항목 |
|---|---|
| High | 다단 대댓글 허용 여부 정책 결정 필요 (findings F-05) |
| Medium | SSR이 대댓글을 표시하지 않는 것에 대한 SEO/접근성 영향 검토 |
| Medium | 조회수 세션키 대소문자 정규화 적용 검토 |
| Medium | 캐시 무효화 `allEntries=true` → 콘텐츠별 키 무효화로 개선 검토 |
| Medium | 좋아요/북마크 로직 중복 구현 → 공통 추상화 검토 |
| Low | 부모 삭제 시 자식 승격 동작이 의도인지 UX 검토 |
| Low | 댓글 조회 페이지네이션/depth 제한 추가 검토 |
| Low | `getEngagementOnly()` 메서드명 재검토 |
| Low | 서비스 레벨 유닛 테스트 부재 (현재 컨트롤러 레벨 4건뿐) |
| Low | 좋아요/북마크 이력 미보존 |

## 6. Next Steps

- [ ] 다단 대댓글 정책을 사용자에게 확인 후 `MVP_SCOPE.md` 기술 정정 또는 서버 검증 추가
- [x] Codex 교차검증 진행 — 완료, 결과 전량 반영
- [ ] Unit 8(인증/보안 체계) 정밀화로 이동

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-06 | PDCA 완료 보고서 초안 작성 — "Match Rate 100%"로 기재(추후 오류로 판명) | Claude (대화 기반) |
| 0.2 | 2026-08-06 | Codex 교차검증 결과 반영 — Match Rate 100% 판정 철회 및 원인 기록, 신규 발견 7건 반영, Known Gaps 10개로 확장 | Claude (Codex `codex exec -s read-only` 검증 결과 반영) |
