package com.dailydevinsight.admin.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RssNewsCrawlerClientTest {

    private RssNewsCrawlerClient rssNewsCrawlerClient;
    private HttpServer httpServer;

    /**
     * @date 2026-08-10
     * @desc RSS 크롤러와 로컬 테스트용 HTTP 서버를 초기화합니다.
     */
    @BeforeEach
    void setUp() throws IOException {
        rssNewsCrawlerClient = new RssNewsCrawlerClient("DailyDevInsightTestBot/1.0");
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.start();
    }

    /**
     * @date 2026-08-10
     * @desc 테스트 종료 후 로컬 HTTP 서버를 중지합니다.
     */
    @AfterEach
    void tearDown() {
        httpServer.stop(0);
    }

    /**
     * @date 2026-08-10
     * @desc 공개 IP를 사용하는 정상 HTTP URL이 검증을 통과하는지 확인합니다.
     */
    @Test
    void validateSourceUrl_ShouldAllowPublicHttpAddress() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                rssNewsCrawlerClient,
                "validateSourceUrl",
                "https://8.8.8.8/rss"
        ));
    }

    /**
     * @date 2026-08-10
     * @desc 루프백, 링크로컬, 사설 IP 주소가 SSRF 검증에서 거부되는지 확인합니다.
     */
    @Test
    void validateSourceUrl_ShouldRejectInternalAddresses() {
        List<String> internalUrls = List.of(
                "http://127.0.0.1/rss",
                "http://169.254.169.254/latest/meta-data",
                "http://10.0.0.1/rss",
                "http://172.16.0.1/rss",
                "http://192.168.0.1/rss"
        );

        for (String internalUrl : internalUrls) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReflectionTestUtils.invokeMethod(
                            rssNewsCrawlerClient,
                            "validateSourceUrl",
                            internalUrl
                    )
            );
        }
    }

    /**
     * @date 2026-08-10
     * @desc 보안 XML 기능을 적용한 뒤에도 정상 RSS 문서가 기존처럼 파싱되는지 확인합니다.
     */
    @Test
    void fetchRssDocument_ShouldParseNormalRssDocument() {
        String rssXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>Daily Dev Insight</title>
                        <item><title>Security Update</title><link>https://example.com/news</link></item>
                    </channel>
                </rss>
                """;
        httpServer.createContext("/rss", exchange -> writeXmlResponse(exchange, 200, rssXml));

        Document document = ReflectionTestUtils.invokeMethod(
                rssNewsCrawlerClient,
                "fetchRssDocument",
                createLocalUrl("/rss"),
                1,
                1
        );

        assertNotNull(document);
        assertEquals("Security Update", document.getElementsByTagName("title").item(1).getTextContent().trim());
    }

    /**
     * @date 2026-08-10
     * @desc DOCTYPE 선언이 포함된 RSS XML이 파서에서 전면 거부되는지 확인합니다.
     */
    @Test
    void fetchRssDocument_ShouldRejectDoctypeDeclaration() {
        String xxeXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss version="2.0"><channel><title>&xxe;</title></channel></rss>
                """;
        httpServer.createContext("/xxe", exchange -> writeXmlResponse(exchange, 200, xxeXml));

        assertThrows(
                Exception.class,
                () -> ReflectionTestUtils.invokeMethod(
                        rssNewsCrawlerClient,
                        "fetchRssDocument",
                        createLocalUrl("/xxe"),
                        1,
                        1
                )
        );
    }

    /**
     * @date 2026-08-10
     * @desc RSS 응답의 리다이렉트 대상이 내부 주소이면 재요청 전에 차단되는지 확인합니다.
     */
    @Test
    void fetchRssDocument_ShouldRejectRedirectToInternalAddress() {
        httpServer.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", createLocalUrl("/rss"));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        rssNewsCrawlerClient,
                        "fetchRssDocument",
                        createLocalUrl("/redirect"),
                        1,
                        1
                )
        );
    }

    /**
     * @date 2026-08-10
     * @desc 테스트 HTTP 서버의 로컬 URL을 생성합니다.
     */
    private String createLocalUrl(String path) {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + path;
    }

    /**
     * @date 2026-08-10
     * @desc 테스트 HTTP 요청에 UTF-8 XML 응답을 반환합니다.
     */
    private void writeXmlResponse(HttpExchange exchange, int responseCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/xml; charset=UTF-8");
        exchange.sendResponseHeaders(responseCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }
}
