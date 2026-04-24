package com.dailydevinsight.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiImageGenerationClient implements ImageGenerationClient {

    private static final String OPENAI_IMAGE_GENERATIONS_PATH = "/v1/images/generations";
    private static final DateTimeFormatter DIRECTORY_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String imageModel;
    private final Path uploadRootDirectory;

    /**
     * @date 2026-04-24
     * @desc OpenAI 이미지 생성 클라이언트 의존성과 설정값을 초기화합니다.
     */
    public OpenAiImageGenerationClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${openai.image-model:gpt-image-1}") String imageModel,
            @Value("${crawler.thumbnail-upload-dir:./uploads}") String thumbnailUploadDirectory
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.imageModel = imageModel;
        this.uploadRootDirectory = Paths.get(thumbnailUploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * @date 2026-04-24
     * @desc OpenAI 이미지 생성 API를 호출한 뒤 결과 이미지를 로컬에 저장하고 공개 경로를 반환합니다.
     */
    @Override
    public String generateAndStoreImage(String prompt, LocalDate targetDate, String quality, Integer maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (prompt == null || prompt.isBlank() || targetDate == null) {
            return "";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", imageModel);
            requestBody.put("prompt", prompt);
            requestBody.put("size", "1024x1024");
            requestBody.put("quality", normalizeQuality(quality));

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + OPENAI_IMAGE_GENERATIONS_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            String publicPath = saveImageFromResponse(responseEntity.getBody(), targetDate);
            return publicPath == null ? "" : publicPath;
        } catch (Exception ignoredException) {
            return "";
        }
    }

    /**
     * @date 2026-04-24
     * @desc OpenAI 응답의 base64 이미지를 디코딩해 uploads/knowledge/{date} 경로에 저장합니다.
     */
    private String saveImageFromResponse(String responseBody, LocalDate targetDate) throws Exception {
        JsonNode responseRoot = objectMapper.readTree(responseBody);
        JsonNode firstDataNode = responseRoot.path("data").path(0);
        if (firstDataNode.isMissingNode()) {
            return "";
        }
        String dateDirectory = targetDate.format(DIRECTORY_DATE_FORMATTER);
        Path targetDirectory = uploadRootDirectory.resolve("knowledge").resolve(dateDirectory);
        Files.createDirectories(targetDirectory);

        String fileName = UUID.randomUUID() + ".png";
        Path targetFilePath = targetDirectory.resolve(fileName);
        JsonNode b64Node = firstDataNode.path("b64_json");
        if (!b64Node.isMissingNode() && !b64Node.asText("").isBlank()) {
            byte[] decodedImageBytes = Base64.getDecoder().decode(b64Node.asText());
            Files.write(targetFilePath, decodedImageBytes);
            return "/uploads/knowledge/" + dateDirectory + "/" + fileName;
        }

        JsonNode urlNode = firstDataNode.path("url");
        if (!urlNode.isMissingNode() && !urlNode.asText("").isBlank()) {
            downloadImageFromUrl(urlNode.asText(), targetFilePath);
            return "/uploads/knowledge/" + dateDirectory + "/" + fileName;
        }

        return "";
    }

    /**
     * @date 2026-04-24
     * @desc OpenAI 응답 URL 이미지를 다운로드해 지정 경로에 저장합니다.
     */
    private void downloadImageFromUrl(String imageUrl, Path targetFilePath) throws Exception {
        URL url = URI.create(imageUrl).toURL();
        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, targetFilePath);
        }
    }

    /**
     * @date 2026-04-24
     * @desc 이미지 품질 입력값을 OpenAI 지원 값(low/medium/high)으로 정규화합니다.
     */
    private String normalizeQuality(String quality) {
        if (quality == null) {
            return "medium";
        }
        String normalizedQuality = quality.trim().toLowerCase();
        if ("low".equals(normalizedQuality) || "medium".equals(normalizedQuality) || "high".equals(normalizedQuality)) {
            return normalizedQuality;
        }
        return "medium";
    }
}
