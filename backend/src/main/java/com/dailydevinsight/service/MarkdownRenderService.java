package com.dailydevinsight.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * @date 2026-08-14
 * @desc 일일 지식 본문(마크다운)을 새니타이징된 HTML로 변환합니다.
 */
@Service
public class MarkdownRenderService {

    private static final Safelist ALLOWED_TAGS = new Safelist()
            .addTags("h2", "h3", "p", "ul", "ol", "li", "strong", "em", "code", "pre", "br", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https");

    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    /**
     * @date 2026-08-14
     * @desc 마크다운 원문을 HTML로 변환한 뒤 허용 태그만 남기고 새니타이징합니다.
     */
    public String renderSafeHtml(String rawMarkdown) {
        if (rawMarkdown == null || rawMarkdown.isBlank()) {
            return "";
        }
        Node document = markdownParser.parse(rawMarkdown);
        String html = htmlRenderer.render(document);
        return Jsoup.clean(html, ALLOWED_TAGS);
    }
}
