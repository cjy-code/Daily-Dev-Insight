package com.dailydevinsight.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRenderServiceTest {

    private final MarkdownRenderService markdownRenderService = new MarkdownRenderService();

    /**
     * @date 2026-08-14
     * @desc 소제목/목록/강조 마크다운 문법이 대응하는 HTML 태그로 변환되는지 검증합니다.
     */
    @Test
    void renderSafeHtml_ShouldConvertBasicMarkdownSyntax() {
        String markdown = "## 제목\n- 항목1\n- 항목2\n\n**강조**";

        String html = markdownRenderService.renderSafeHtml(markdown);

        assertTrue(html.contains("<h2>제목</h2>"));
        assertTrue(html.contains("<li>항목1</li>"));
        assertTrue(html.contains("<li>항목2</li>"));
        assertTrue(html.contains("<strong>강조</strong>"));
    }

    /**
     * @date 2026-08-14
     * @desc script 태그와 이벤트 핸들러 속성이 새니타이징으로 제거되는지 검증합니다.
     */
    @Test
    void renderSafeHtml_ShouldStripScriptAndEventHandlerAttributes() {
        String markdown = "<script>alert(1)</script>\n\n일반 텍스트 <img src=x onerror=alert(1)>";

        String html = markdownRenderService.renderSafeHtml(markdown);

        assertFalse(html.contains("<script"));
        assertFalse(html.contains("onerror"));
        assertFalse(html.contains("<img"));
    }

    /**
     * @date 2026-08-14
     * @desc 마크다운 문법이 없는 평문도 오류 없이 문단으로 렌더링되는지 검증합니다.
     */
    @Test
    void renderSafeHtml_ShouldWrapPlainTextInParagraph() {
        String plainText = "이것은 마크다운 문법이 없는 그냥 평문입니다.";

        String html = markdownRenderService.renderSafeHtml(plainText);

        assertTrue(html.contains("<p>"));
        assertTrue(html.contains(plainText));
    }

    /**
     * @date 2026-08-14
     * @desc 빈 입력값은 빈 문자열을 반환해야 합니다.
     */
    @Test
    void renderSafeHtml_ShouldReturnEmptyForBlankInput() {
        assertEquals("", markdownRenderService.renderSafeHtml(null));
        assertEquals("", markdownRenderService.renderSafeHtml("   "));
    }
}
