# 일일 지식 본문 마크다운 렌더링 Design Document

> **Summary**: `DailyKnowledge.detail`을 마크다운으로 생성/저장하고, KNOWLEDGE 상세 화면에서만 새니타이징된 HTML로 렌더링
>
> **Project**: dailyDevInsight
> **Date**: 2026-08-14
> **Status**: Approved

---

## Context Anchor

| Key | Value |
|-----|-------|
| **WHY** | `docs/01-plan/features/knowledge-detail-markdown-rendering.md` 참조 |
| **RISK** | `InsightDetailService.buildDetailResponse()`가 KNOWLEDGE/NEWS 공용 — 조건부 분기 누락 시 뉴스 화면 회귀 |
| **SCOPE** | Plan §2.1과 동일 |

---

## 1. 관련 파일

| 레이어 | 파일 | 변경 유형 |
|--------|------|-----------|
| Dependency | `build.gradle` | `org.commonmark:commonmark` 추가 |
| Service (신규) | `service/MarkdownRenderService.java` | 마크다운→HTML 변환 + 새니타이징 |
| Service | `service/InsightDetailService.java` | `buildDetailResponse()`에서 조건부 `detailHtml` 계산 |
| DTO | `dto/InsightDetailResponseDTO.java` | `detailHtml` 필드 추가 |
| LLM 클라이언트 | `admin/service/OpenAiLlmGenerationClient.java` | 시스템 지시문에 마크다운 규칙 추가 |
| Template | `templates/insight-detail.html` | `detailHtml` 있으면 `th:utext`, 없으면 기존 `th:text` 폴백 |
| CSS | `static/css/insight-detail.css` | 마크다운 요소 스타일 추가 |
| Test | `test/.../service/MarkdownRenderServiceTest.java` | XSS/기본 변환 시나리오 |

---

## 2. MarkdownRenderService 설계

```java
@Service
public class MarkdownRenderService {

    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private static final Safelist ALLOWED_TAGS = new Safelist()
            .addTags("h2", "h3", "p", "ul", "ol", "li", "strong", "em", "code", "pre", "br", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https");

    public String renderSafeHtml(String rawMarkdown) {
        if (rawMarkdown == null || rawMarkdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(rawMarkdown);
        String html = htmlRenderer.render(document);
        return Jsoup.clean(html, ALLOWED_TAGS);
    }
}
```

- `h1`은 허용하지 않는다 — 상세 화면 최상단 `<h1>{title}</h1>`과 중복되는 걸 막기 위해 본문 소제목은 `##`(h2)부터 쓰도록 프롬프트에서 안내
- `a` 태그는 허용하되 `href`만 허용(스크립트 URL 방지는 `addProtocols`로 http/https만 통과)
- 이미지 태그(`img`)는 허용 목록에서 제외 — 본문 이미지 삽입은 이번 범위 밖

## 3. InsightDetailService 변경

`buildDetailResponse()` 내 기존:
```java
.detail(baseData.detail())
```
변경 후:
```java
.detail(baseData.detail())
.detailHtml(contentType == InsightContentType.KNOWLEDGE
        ? markdownRenderService.renderSafeHtml(baseData.detail())
        : null)
```

## 4. 템플릿 변경

`insight-detail.html` 기존:
```html
<p class="detail-text" th:text="${detail.detail}">본문 내용</p>
```
변경 후:
```html
<div class="detail-text detail-markdown"
     th:if="${detail.detailHtml != null and !#strings.isEmpty(detail.detailHtml)}"
     th:utext="${detail.detailHtml}">본문 내용</div>
<p class="detail-text"
   th:unless="${detail.detailHtml != null and !#strings.isEmpty(detail.detailHtml)}"
   th:text="${detail.detail}">본문 내용</p>
```

## 5. 프롬프트 지시문 추가 (OpenAiLlmGenerationClient)

기존 `buildSystemInstruction()`에 다음 규칙을 덧붙인다:

> detail 필드는 마크다운 문법을 사용해 작성하세요. 소제목은 `##`, 목록은 `-`, 강조는 `**굵게**`만 사용하고 그 외 문법(표, 이미지, 링크)은 쓰지 마세요. `#`(h1)은 사용하지 마세요.

## 6. CSS 추가 (insight-detail.css)

```css
.detail-markdown h2 { margin: 24px 0 10px; font-size: 19px; color: #1f2b46; }
.detail-markdown h3 { margin: 18px 0 8px; font-size: 16px; color: #1f2b46; }
.detail-markdown ul, .detail-markdown ol { margin: 8px 0; padding-left: 22px; }
.detail-markdown li { margin: 4px 0; }
.detail-markdown strong { color: #16213f; }
.detail-markdown pre { background: #f5f7fb; padding: 12px; border-radius: 8px; overflow-x: auto; }
.detail-markdown code { background: #f0f2f7; padding: 2px 5px; border-radius: 4px; font-size: 0.92em; }
.detail-markdown p { margin: 0 0 12px; }
```

## 7. 테스트 시나리오

- 기본 변환: `## 제목\n- 항목1\n- 항목2\n**강조**` → 대응하는 h2/ul/li/strong 태그 확인
- XSS 방어: `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>` 입력이 결과 HTML에 남지 않는지 확인
- 평문 폴백: 마크다운 문법이 없는 일반 텍스트가 오류 없이 `<p>`로 감싸져 렌더링되는지 확인
- NEWS 회귀 확인: `detailHtml`이 NEWS 응답에서 null이고 템플릿이 기존 `th:text` 경로를 타는지 확인

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-08-14 | 최초 작성 | Claude |
