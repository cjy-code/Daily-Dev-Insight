package com.dailydevinsight.admin.service;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RssNewsCrawlerClient implements NewsCrawlerClient {

    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;
    private static final int MIN_RETRY_COUNT = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int DETAIL_TEXT_MAX_LENGTH = 20000;
    private static final int META_DESCRIPTION_MAX_LENGTH = 3000;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);

    private final String userAgent;

    /**
     * @date 2026-04-17
     * @desc RSS 요청 시 사용할 HTTP User-Agent 값을 초기화합니다.
     */
    public RssNewsCrawlerClient(
            @Value("${crawler.user-agent:DailyDevInsightBot/1.0}") String userAgent
    ) {
        this.userAgent = userAgent;
    }

    /**
     * @date 2026-04-20
     * @desc RSS 목록을 읽고 각 기사 URL을 재호출하여 제목/요약/본문/대표이미지를 보강합니다.
     */
    @Override
    public List<NewsArticleData> crawlArticles(
            String sourceName,
            String sourceUrl,
            int maxArticles,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int retryCount
    ) {
        List<NewsArticleData> articles = new ArrayList<>();
        int validatedMaxArticles = Math.max(1, maxArticles);
        int validatedConnectTimeoutSeconds = normalizeTimeoutSeconds(connectTimeoutSeconds);
        int validatedReadTimeoutSeconds = normalizeTimeoutSeconds(readTimeoutSeconds);
        int validatedRetryCount = normalizeRetryCount(retryCount);

        validateSourceUrl(sourceUrl);

        try {
            Document document = fetchRssDocumentWithRetry(
                    sourceUrl,
                    validatedConnectTimeoutSeconds,
                    validatedReadTimeoutSeconds,
                    validatedRetryCount
            );
            NodeList itemNodes = document.getElementsByTagName("item");
            for (int index = 0; index < itemNodes.getLength() && articles.size() < validatedMaxArticles; index++) {
                Node itemNode = itemNodes.item(index);
                if (!(itemNode instanceof org.w3c.dom.Element itemElement)) {
                    continue;
                }

                String rssTitle = readChildText(itemElement, "title");
                String articleUrl = readChildText(itemElement, "link");
                String description = readChildText(itemElement, "description");
                String rssSummary = stripHtml(description).trim();
                String rssImageUrl = resolveThumbnailImageUrl(itemElement, description);

                if (rssTitle.isBlank() || articleUrl.isBlank()) {
                    continue;
                }

                DetailCrawlData detailCrawlData = fetchArticleDetailWithRetry(
                        articleUrl.trim(),
                        validatedConnectTimeoutSeconds,
                        validatedReadTimeoutSeconds,
                        validatedRetryCount
                );

                articles.add(NewsArticleData.builder()
                        .sourceName(sourceName)
                        .title(resolveFinalTitle(rssTitle, detailCrawlData))
                        .url(articleUrl.trim())
                        .summary(resolveFinalSummary(rssSummary, detailCrawlData))
                        .content(resolveFinalContent(detailCrawlData))
                        .imageUrl(resolveFinalImageUrl(rssImageUrl, detailCrawlData))
                        .build());
            }
            return articles;
        } catch (Exception exception) {
            throw new IllegalStateException("RSS 기사 수집에 실패했습니다: " + exception.getMessage(), exception);
        }
    }

    /**
     * @date 2026-04-17
     * @desc 재시도 횟수만큼 RSS XML 조회를 시도하여 DOM Document로 변환합니다.
     */
    private Document fetchRssDocumentWithRetry(
            String sourceUrl,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int retryCount
    ) throws Exception {
        Exception lastException = null;
        int maxAttemptCount = retryCount + 1;
        for (int attempt = 1; attempt <= maxAttemptCount; attempt++) {
            try {
                return fetchRssDocument(sourceUrl, connectTimeoutSeconds, readTimeoutSeconds);
            } catch (Exception exception) {
                lastException = exception;
                if (attempt == maxAttemptCount) {
                    throw exception;
                }
            }
        }
        throw lastException == null ? new IllegalStateException("RSS 조회에 실패했습니다.") : lastException;
    }

    /**
     * @date 2026-04-20
     * @desc 기사 상세 URL을 재호출하여 제목/요약/본문/대표이미지를 재수집합니다.
     */
    private DetailCrawlData fetchArticleDetailWithRetry(
            String articleUrl,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int retryCount
    ) {
        Exception lastException = null;
        int maxAttemptCount = retryCount + 1;
        for (int attempt = 1; attempt <= maxAttemptCount; attempt++) {
            try {
                return fetchArticleDetail(articleUrl, connectTimeoutSeconds, readTimeoutSeconds);
            } catch (Exception exception) {
                lastException = exception;
                if (attempt == maxAttemptCount) {
                    break;
                }
            }
        }

        if (lastException != null) {
            return DetailCrawlData.empty();
        }
        return DetailCrawlData.empty();
    }

    /**
     * @date 2026-04-20
     * @desc 단일 기사 상세 페이지를 조회하여 메타데이터와 본문 텍스트를 추출합니다.
     */
    private DetailCrawlData fetchArticleDetail(
            String articleUrl,
            int connectTimeoutSeconds,
            int readTimeoutSeconds
    ) throws Exception {
        validateSourceUrl(articleUrl);

        URL url = URI.create(articleUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutSeconds * 1000);
        connection.setReadTimeout(readTimeoutSeconds * 1000);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("상세 페이지 응답 코드가 정상이 아닙니다: " + responseCode);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            org.jsoup.nodes.Document detailDocument = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), url.toString());

            String detailTitle = extractDetailTitle(detailDocument);
            String detailSummary = extractDetailSummary(detailDocument);
            String detailImageUrl = extractDetailImageUrl(detailDocument);
            String detailContent = extractDetailContent(detailDocument);
            return new DetailCrawlData(detailTitle, detailSummary, detailContent, detailImageUrl);
        }
    }

    /**
     * @date 2026-04-17
     * @desc RSS URL에서 XML을 조회해 안전하게 DOM Document로 변환합니다.
     */
    private Document fetchRssDocument(
            String sourceUrl,
            int connectTimeoutSeconds,
            int readTimeoutSeconds
    ) throws Exception {
        URL url = new URL(sourceUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutSeconds * 1000);
        connection.setReadTimeout(readTimeoutSeconds * 1000);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("RSS 조회 응답 코드가 정상이 아닙니다: " + responseCode);
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
     * @desc 입력 URL 형식과 프로토콜(http/https)을 검증합니다.
     */
    private void validateSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("소스 URL이 비어 있습니다.");
        }
        URI uri = URI.create(sourceUrl.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("소스 URL은 http 또는 https만 허용됩니다.");
        }
    }

    /**
     * @date 2026-04-17
     * @desc 타임아웃 값을 최소/최대 범위로 보정합니다.
     */
    private int normalizeTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < MIN_TIMEOUT_SECONDS) {
            return MIN_TIMEOUT_SECONDS;
        }
        return Math.min(timeoutSeconds, MAX_TIMEOUT_SECONDS);
    }

    /**
     * @date 2026-04-17
     * @desc 재시도 횟수를 최소/최대 범위로 보정합니다.
     */
    private int normalizeRetryCount(int retryCount) {
        if (retryCount < MIN_RETRY_COUNT) {
            return MIN_RETRY_COUNT;
        }
        return Math.min(retryCount, MAX_RETRY_COUNT);
    }

    /**
     * @date 2026-04-17
     * @desc 자식 태그의 첫 번째 텍스트 값을 읽어 반환합니다.
     */
    private String readChildText(org.w3c.dom.Element parentElement, String tagName) {
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
     * @desc RSS description HTML에서 태그를 제거해 텍스트로 변환합니다.
     */
    private String stripHtml(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
    }

    /**
     * @date 2026-04-17
     * @desc RSS 아이템에서 대표 이미지 URL을 우선순위(media/enclosure/img)로 추출합니다.
     */
    private String resolveThumbnailImageUrl(org.w3c.dom.Element itemElement, String descriptionHtml) {
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
     * @desc 특정 태그의 첫 번째 요소에서 지정 속성 값을 추출합니다.
     */
    private String readElementAttribute(org.w3c.dom.Element parentElement, String tagName, String attributeName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return "";
        }
        Node node = nodeList.item(0);
        if (!(node instanceof org.w3c.dom.Element element)) {
            return "";
        }
        String value = element.getAttribute(attributeName);
        return value == null ? "" : value.trim();
    }

    /**
     * @date 2026-04-17
     * @desc description HTML의 첫 번째 img src URL을 추출합니다.
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

    /**
     * @date 2026-04-20
     * @desc 상세 페이지 제목이 유효하면 우선 사용하고 아니면 RSS 제목을 사용합니다.
     */
    private String resolveFinalTitle(String rssTitle, DetailCrawlData detailCrawlData) {
        if (detailCrawlData == null || detailCrawlData.title().isBlank()) {
            return rssTitle.trim();
        }
        return detailCrawlData.title().trim();
    }

    /**
     * @date 2026-04-20
     * @desc 상세 요약을 우선 사용하고 없으면 RSS description 요약을 사용합니다.
     */
    private String resolveFinalSummary(String rssSummary, DetailCrawlData detailCrawlData) {
        if (detailCrawlData != null && !detailCrawlData.summary().isBlank()) {
            return limitLength(detailCrawlData.summary(), META_DESCRIPTION_MAX_LENGTH);
        }
        return limitLength(rssSummary == null ? "" : rssSummary.trim(), META_DESCRIPTION_MAX_LENGTH);
    }

    /**
     * @date 2026-04-20
     * @desc 상세 본문 텍스트를 반환하고 길이 제한을 적용합니다.
     */
    private String resolveFinalContent(DetailCrawlData detailCrawlData) {
        if (detailCrawlData == null) {
            return "";
        }
        return limitLength(detailCrawlData.content(), DETAIL_TEXT_MAX_LENGTH);
    }

    /**
     * @date 2026-04-20
     * @desc 상세 이미지가 있으면 우선 사용하고 없으면 RSS 이미지 URL을 사용합니다.
     */
    private String resolveFinalImageUrl(String rssImageUrl, DetailCrawlData detailCrawlData) {
        if (detailCrawlData != null && !detailCrawlData.imageUrl().isBlank()) {
            return detailCrawlData.imageUrl().trim();
        }
        if (rssImageUrl == null) {
            return "";
        }
        return rssImageUrl.trim();
    }

    /**
     * @date 2026-04-20
     * @desc 상세 페이지에서 title/og:title 값을 우선순위로 추출합니다.
     */
    private String extractDetailTitle(org.jsoup.nodes.Document detailDocument) {
        String ogTitle = extractMetaContent(detailDocument, "property", "og:title");
        if (!ogTitle.isBlank()) {
            return ogTitle.trim();
        }
        String documentTitle = detailDocument.title();
        if (documentTitle == null || documentTitle.isBlank()) {
            return "";
        }
        return documentTitle.trim();
    }

    /**
     * @date 2026-04-20
     * @desc 상세 페이지에서 description/og:description 값을 우선순위로 추출합니다.
     */
    private String extractDetailSummary(org.jsoup.nodes.Document detailDocument) {
        String description = extractMetaContent(detailDocument, "name", "description");
        if (!description.isBlank()) {
            return description.trim();
        }
        String ogDescription = extractMetaContent(detailDocument, "property", "og:description");
        if (!ogDescription.isBlank()) {
            return ogDescription.trim();
        }
        return "";
    }

    /**
     * @date 2026-04-20
     * @desc 상세 페이지에서 대표 이미지 URL(og/twitter/img)을 추출합니다.
     */
    private String extractDetailImageUrl(org.jsoup.nodes.Document detailDocument) {
        String ogImage = extractMetaContent(detailDocument, "property", "og:image");
        if (!ogImage.isBlank()) {
            return ogImage.trim();
        }

        String twitterImage = extractMetaContent(detailDocument, "name", "twitter:image");
        if (!twitterImage.isBlank()) {
            return twitterImage.trim();
        }

        List<org.jsoup.nodes.Element> imageElements = detailDocument.select("article img[src], main img[src], img[src]");
        for (org.jsoup.nodes.Element imageElement : imageElements) {
            String imageUrl = imageElement.absUrl("src");
            if (!imageUrl.isBlank()) {
                return imageUrl.trim();
            }
        }
        return "";
    }

    /**
     * @date 2026-04-20
     * @desc 상세 페이지 본문 텍스트를 article/main/body 순서로 추출합니다.
     */
    private String extractDetailContent(org.jsoup.nodes.Document detailDocument) {
        String articleText = extractElementText(detailDocument.selectFirst("article"));
        if (!articleText.isBlank()) {
            return articleText;
        }

        String mainText = extractElementText(detailDocument.selectFirst("main"));
        if (!mainText.isBlank()) {
            return mainText;
        }

        return extractElementText(detailDocument.body());
    }

    /**
     * @date 2026-04-20
     * @desc meta 태그의 속성 조건과 일치하는 content 값을 반환합니다.
     */
    private String extractMetaContent(
            org.jsoup.nodes.Document detailDocument,
            String attributeName,
            String attributeValue
    ) {
        if (detailDocument == null) {
            return "";
        }
        String selector = "meta[" + attributeName + "=\"" + attributeValue + "\"]";
        org.jsoup.nodes.Element metaElement = detailDocument.selectFirst(selector);
        if (metaElement == null) {
            return "";
        }
        String content = metaElement.attr("content");
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.trim();
    }

    /**
     * @date 2026-04-20
     * @desc HTML 요소 텍스트를 공백 정규화하여 반환합니다.
     */
    private String extractElementText(org.jsoup.nodes.Element element) {
        if (element == null) {
            return "";
        }
        String text = element.text();
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * @date 2026-04-20
     * @desc 문자열이 최대 길이를 넘으면 잘라서 반환합니다.
     */
    private String limitLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() <= maxLength) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, maxLength);
    }

    /**
     * @date 2026-04-20
     * @desc 상세 페이지 재수집 결과를 담는 내부 구조체입니다.
     */
    private record DetailCrawlData(String title, String summary, String content, String imageUrl) {

        /**
         * @date 2026-04-20
         * @desc 빈 상세 데이터 객체를 반환합니다.
         */
        private static DetailCrawlData empty() {
            return new DetailCrawlData("", "", "", "");
        }
    }
}
