package com.dailydevinsight.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class RssNewsCrawlerClient implements NewsCrawlerClient {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5000;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);

    private final String userAgent;

    /**
     * @date 2026-04-17
     * @desc RSS 수집 시 사용할 HTTP User-Agent 값을 초기화합니다.
     */
    public RssNewsCrawlerClient(
            @Value("${crawler.user-agent:DailyDevInsightBot/1.0}") String userAgent
    ) {
        this.userAgent = userAgent;
    }

    /**
     * @date 2026-04-17
     * @desc RSS XML을 호출하고 item 목록을 최대 건수만큼 파싱하여 반환합니다.
     */
    @Override
    public List<NewsArticleData> crawlArticles(String sourceName, String sourceUrl, int maxArticles) {
        List<NewsArticleData> articles = new ArrayList<>();
        int validatedMaxArticles = Math.max(1, maxArticles);

        try {
            Document document = fetchRssDocument(sourceUrl);
            NodeList itemNodes = document.getElementsByTagName("item");
            for (int index = 0; index < itemNodes.getLength() && articles.size() < validatedMaxArticles; index++) {
                Node itemNode = itemNodes.item(index);
                if (!(itemNode instanceof Element itemElement)) {
                    continue;
                }
                String title = readChildText(itemElement, "title");
                String url = readChildText(itemElement, "link");
                String description = readChildText(itemElement, "description");
                String imageUrl = resolveThumbnailImageUrl(itemElement, description);
                if (title.isBlank() || url.isBlank()) {
                    continue;
                }
                articles.add(NewsArticleData.builder()
                        .sourceName(sourceName)
                        .title(title.trim())
                        .url(url.trim())
                        .summary(stripHtml(description).trim())
                        .imageUrl(imageUrl)
                        .build());
            }
            return articles;
        } catch (Exception exception) {
            throw new IllegalStateException("RSS 뉴스 수집에 실패했습니다: " + exception.getMessage(), exception);
        }
    }

    /**
     * @date 2026-04-17
     * @desc RSS URL에서 XML 문서를 내려받아 DOM Document로 반환합니다.
     */
    private Document fetchRssDocument(String sourceUrl) throws Exception {
        URL url = new URL(sourceUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("RSS 호출 응답 코드가 비정상입니다: " + responseCode);
        }

        try (InputStream inputStream = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setXIncludeAware(false);

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new InputSource(reader));
        }
    }

    /**
     * @date 2026-04-17
     * @desc 지정한 태그의 첫 번째 텍스트 값을 안전하게 읽어옵니다.
     */
    private String readChildText(Element parentElement, String tagName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return "";
        }
        Node targetNode = nodeList.item(0);
        if (targetNode == null || targetNode.getTextContent() == null) {
            return "";
        }
        return targetNode.getTextContent();
    }

    /**
     * @date 2026-04-17
     * @desc RSS description의 HTML 태그를 제거하여 평문 요약으로 변환합니다.
     */
    private String stripHtml(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
    }

    /**
     * @date 2026-04-17
     * @desc RSS item에서 대표 이미지 URL(media/enclosure/img 태그)을 우선순위로 추출합니다.
     */
    private String resolveThumbnailImageUrl(Element itemElement, String descriptionHtml) {
        String mediaContentUrl = readElementAttribute(itemElement, "media:content", "url");
        if (!mediaContentUrl.isBlank()) {
            return mediaContentUrl;
        }

        String mediaThumbnailUrl = readElementAttribute(itemElement, "media:thumbnail", "url");
        if (!mediaThumbnailUrl.isBlank()) {
            return mediaThumbnailUrl;
        }

        String enclosureUrl = readElementAttribute(itemElement, "enclosure", "url");
        String enclosureType = readElementAttribute(itemElement, "enclosure", "type");
        if (!enclosureUrl.isBlank() && enclosureType.toLowerCase().startsWith("image/")) {
            return enclosureUrl;
        }

        return readImageUrlFromDescription(descriptionHtml);
    }

    /**
     * @date 2026-04-17
     * @desc 특정 태그의 첫 번째 엘리먼트에서 지정한 속성 값을 읽어옵니다.
     */
    private String readElementAttribute(Element parentElement, String tagName, String attributeName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return "";
        }
        Node node = nodeList.item(0);
        if (!(node instanceof Element element)) {
            return "";
        }
        String value = element.getAttribute(attributeName);
        return value == null ? "" : value.trim();
    }

    /**
     * @date 2026-04-17
     * @desc description HTML 내 img 태그에서 첫 번째 src URL을 추출합니다.
     */
    private String readImageUrlFromDescription(String descriptionHtml) {
        if (descriptionHtml == null || descriptionHtml.isBlank()) {
            return "";
        }
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(descriptionHtml);
        if (!matcher.find()) {
            return "";
        }
        String imageUrl = matcher.group(1);
        return imageUrl == null ? "" : imageUrl.trim();
    }
}
