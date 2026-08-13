package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.GeneratedKnowledgeResult;
import com.dailydevinsight.admin.dto.GeneratedDailyTrendResult;
import com.dailydevinsight.admin.dto.GeneratedWeeklyInsightResult;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiLlmGenerationClient implements LlmGenerationClient {

    private static final String OPENAI_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String OPENAI_API_KEY_ENV_NAME = "OPENAI_API_KEY";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;
    private final String model;
    private final String baseUrl;

    /**
     * @date 2026-04-16
     * @desc OpenAI API 호출에 필요한 설정값과 의존 객체를 초기화합니다.
     */
    public OpenAiLlmGenerationClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.configuredApiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    /**
     * @date 2026-04-16
     * @desc 렌더링된 프롬프트를 OpenAI Chat Completions API로 전송해 생성 결과를 반환합니다.
     */
    @Override
    public GeneratedKnowledgeResult generateKnowledge(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    ) {
        String resolvedApiKey = validateAndResolveApiKey();
        try {
            HttpEntity<Map<String, Object>> httpEntity = createRequestEntity(
                    prompt,
                    targetDate,
                    category,
                    tone,
                    difficulty,
                    resolvedApiKey
            );
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + OPENAI_CHAT_COMPLETIONS_PATH,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            return parseGeneratedResult(responseEntity.getBody());
        } catch (HttpStatusCodeException exception) {
            throw toLlmClientException(exception);
        } catch (ResourceAccessException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "network_error",
                    0,
                    "OpenAI 서버 연결에 실패했습니다. 네트워크 상태를 확인해주세요.",
                    "OpenAI 네트워크 연결 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-05-08
     * @desc 최근 7일 크롤링 뉴스 목록을 OpenAI에 전달해 주간 개발 트렌드 분석 결과를 생성합니다.
     */
    @Override
    public GeneratedWeeklyInsightResult generateWeeklyInsight(String prompt, LocalDate weekStartDate, LocalDate weekEndDate) {
        String resolvedApiKey = validateAndResolveApiKey();
        try {
            HttpEntity<Map<String, Object>> httpEntity = createWeeklyInsightRequestEntity(
                    prompt,
                    weekStartDate,
                    weekEndDate,
                    resolvedApiKey
            );
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + OPENAI_CHAT_COMPLETIONS_PATH,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            return parseWeeklyInsightResult(responseEntity.getBody());
        } catch (HttpStatusCodeException exception) {
            throw toLlmClientException(exception);
        } catch (ResourceAccessException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "network_error",
                    0,
                    "OpenAI 서버 연결에 실패했습니다. 네트워크 상태를 확인해주세요.",
                    "OpenAI 네트워크 연결 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-08-13
     * @desc 크롤링 뉴스 목록을 OpenAI에 전달해 기준일의 일일 개발 트렌드를 생성합니다.
     */
    @Override
    public GeneratedDailyTrendResult generateDailyTrend(String prompt, LocalDate targetDate) {
        String resolvedApiKey = validateAndResolveApiKey();
        try {
            HttpEntity<Map<String, Object>> httpEntity = createDailyTrendRequestEntity(
                    prompt,
                    targetDate,
                    resolvedApiKey
            );
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    baseUrl + OPENAI_CHAT_COMPLETIONS_PATH,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            return parseDailyTrendResult(responseEntity.getBody());
        } catch (HttpStatusCodeException exception) {
            throw toLlmClientException(exception);
        } catch (ResourceAccessException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "network_error",
                    0,
                    "OpenAI 서버 연결에 실패했습니다. 네트워크 상태를 확인해주세요.",
                    "OpenAI 네트워크 연결 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-04-20
     * @desc 설정값/환경변수/JVM 프로퍼티에서 OpenAI API Key를 조회하고 유효성을 검증합니다.
     */
    private String validateAndResolveApiKey() {
        String resolvedApiKey = resolveApiKey();
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            throw new LlmClientException(
                    "OPENAI",
                    "missing_api_key",
                    0,
                    "OpenAI API Key가 설정되지 않았습니다. 환경변수 적용 후 애플리케이션을 재시작해주세요.",
                    "OpenAI API Key 조회 실패: openai.api-key, OPENAI_API_KEY(환경변수), OPENAI_API_KEY(JVM 프로퍼티) 모두 비어 있습니다."
            );
        }
        return resolvedApiKey.trim();
    }

    /**
     * @date 2026-04-20
     * @desc OpenAI API Key를 설정 프로퍼티, 운영체제 환경변수, JVM 시스템 프로퍼티 순으로 조회합니다.
     */
    private String resolveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey;
        }

        String environmentApiKey = System.getenv(OPENAI_API_KEY_ENV_NAME);
        if (environmentApiKey != null && !environmentApiKey.isBlank()) {
            return environmentApiKey;
        }

        return System.getProperty(OPENAI_API_KEY_ENV_NAME);
    }

    /**
     * @date 2026-04-16
     * @desc OpenAI 요청 헤더와 본문(JSON)을 구성합니다.
     */
    private HttpEntity<Map<String, Object>> createRequestEntity(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty,
            String apiKey
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemInstruction()),
                Map.of("role", "user", "content", buildUserPrompt(prompt, targetDate, category, tone, difficulty))
        ));

        return new HttpEntity<>(body, headers);
    }

    /**
     * @date 2026-05-08
     * @desc 주간 AI 인사이트 생성을 위한 OpenAI 요청 헤더와 본문을 구성합니다.
     */
    private HttpEntity<Map<String, Object>> createWeeklyInsightRequestEntity(
            String prompt,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String apiKey
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.4);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildWeeklyInsightSystemInstruction()),
                Map.of("role", "user", "content", buildWeeklyInsightUserPrompt(prompt, weekStartDate, weekEndDate))
        ));

        return new HttpEntity<>(body, headers);
    }

    /**
     * @date 2026-08-13
     * @desc 일일 개발 트렌드 생성을 위한 OpenAI 요청 헤더와 본문을 구성합니다.
     */
    private HttpEntity<Map<String, Object>> createDailyTrendRequestEntity(
            String prompt,
            LocalDate targetDate,
            String apiKey
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.4);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildDailyTrendSystemInstruction()),
                Map.of("role", "user", "content", buildDailyTrendUserPrompt(prompt, targetDate))
        ));

        return new HttpEntity<>(body, headers);
    }

    /**
     * @date 2026-04-16
     * @desc 모델이 반드시 JSON 형식으로 응답하도록 시스템 지시문을 생성합니다.
     */
    private String buildSystemInstruction() {
        return "당신은 일일 개발 지식 콘텐츠 생성기입니다. 반드시 JSON 객체로만 응답하세요. "
                + "필수 키는 title, summary, detail 입니다.";
    }

    /**
     * @date 2026-04-16
     * @desc 템플릿 기반 프롬프트와 생성 조건을 사용자 메시지로 구성합니다.
     */
    private String buildUserPrompt(
            String prompt,
            LocalDate targetDate,
            String category,
            String tone,
            String difficulty
    ) {
        return "다음 조건으로 일일 개발 지식을 생성하세요.\n"
                + "- 대상일: " + targetDate + "\n"
                + "- 카테고리: " + category + "\n"
                + "- 톤: " + tone + "\n"
                + "- 난이도: " + difficulty + "\n\n"
                + "프롬프트 본문:\n"
                + prompt + "\n\n"
                + "응답 JSON 형식 예시:\n"
                + "{\"title\":\"...\",\"summary\":\"...\",\"detail\":\"...\"}";
    }

    /**
     * @date 2026-04-16
     * @desc OpenAI 응답에서 콘텐츠 문자열을 추출해 JSON으로 파싱합니다.
     */
    private GeneratedKnowledgeResult parseGeneratedResult(String responseBody) {
        try {
            JsonNode responseRoot = objectMapper.readTree(responseBody);
            JsonNode contentNode = responseRoot.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new LlmClientException(
                        "OPENAI",
                        "empty_response",
                        200,
                        "OpenAI 응답이 비어 있습니다. 잠시 후 다시 시도해주세요.",
                        "OpenAI choices.message.content 값이 비어 있습니다."
                );
            }

            JsonNode generatedJson = objectMapper.readTree(contentNode.asText());
            String title = readRequiredValue(generatedJson, "title");
            String summary = readRequiredValue(generatedJson, "summary");
            String detail = readRequiredValue(generatedJson, "detail");

            return GeneratedKnowledgeResult.builder()
                    .title(title)
                    .summary(summary)
                    .detail(detail)
                    .build();
        } catch (JsonProcessingException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "invalid_response_format",
                    200,
                    "OpenAI 응답 형식 파싱에 실패했습니다.",
                    "OpenAI 응답 파싱 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-05-08
     * @desc OpenAI 응답에서 주간 AI 인사이트 JSON을 추출하고 검증합니다.
     */
    private GeneratedWeeklyInsightResult parseWeeklyInsightResult(String responseBody) {
        try {
            JsonNode responseRoot = objectMapper.readTree(responseBody);
            JsonNode contentNode = responseRoot.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new LlmClientException(
                        "OPENAI",
                        "empty_response",
                        200,
                        "OpenAI 응답이 비어 있습니다. 잠시 후 다시 시도해주세요.",
                        "OpenAI choices.message.content 값이 비어 있습니다."
                );
            }

            JsonNode generatedJson = objectMapper.readTree(contentNode.asText());
            String summary = readRequiredValue(generatedJson, "summary");
            String trendAnalysis = readRequiredValue(generatedJson, "trendAnalysis");
            String developerView = readRequiredValue(generatedJson, "developerView");

            return GeneratedWeeklyInsightResult.builder()
                    .summary(summary)
                    .trendAnalysis(trendAnalysis)
                    .developerView(developerView)
                    .build();
        } catch (JsonProcessingException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "invalid_response_format",
                    200,
                    "OpenAI 응답 형식 파싱에 실패했습니다.",
                    "OpenAI 주간 인사이트 응답 파싱 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-08-13
     * @desc OpenAI 응답에서 일일 개발 트렌드 JSON을 추출하고 키워드 개수를 검증합니다.
     */
    private GeneratedDailyTrendResult parseDailyTrendResult(String responseBody) {
        try {
            JsonNode responseRoot = objectMapper.readTree(responseBody);
            JsonNode contentNode = responseRoot.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new LlmClientException(
                        "OPENAI",
                        "empty_response",
                        200,
                        "OpenAI 응답이 비어 있습니다. 잠시 후 다시 시도해주세요.",
                        "OpenAI choices.message.content 값이 비어 있습니다."
                );
            }

            JsonNode generatedJson = objectMapper.readTree(contentNode.asText());
            JsonNode keywordsNode = generatedJson.path("keywords");
            LinkedHashSet<String> normalizedKeywords = new LinkedHashSet<>();
            if (keywordsNode.isArray()) {
                keywordsNode.forEach(keywordNode -> {
                    String keyword = keywordNode.asText("").trim().replace(",", "");
                    if (!keyword.isBlank()) {
                        normalizedKeywords.add(keyword);
                    }
                });
            }
            if (normalizedKeywords.size() < 3) {
                throw new LlmClientException(
                        "OPENAI",
                        "invalid_daily_trend_keywords",
                        200,
                        "OpenAI 응답의 트렌드 키워드가 부족합니다.",
                        "OpenAI 일일 트렌드 응답의 유효 키워드가 3개 미만입니다."
                );
            }

            List<String> keywords = new ArrayList<>(normalizedKeywords).stream().limit(5).toList();
            return GeneratedDailyTrendResult.builder()
                    .keywords(keywords)
                    .summary(readRequiredValue(generatedJson, "summary"))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new LlmClientException(
                    "OPENAI",
                    "invalid_response_format",
                    200,
                    "OpenAI 응답 형식 파싱에 실패했습니다.",
                    "OpenAI 일일 트렌드 응답 파싱 실패: " + exception.getMessage()
            );
        }
    }

    /**
     * @date 2026-05-08
     * @desc 모델이 주간 개발 트렌드 분석을 JSON 형식으로 응답하도록 시스템 지시문을 생성합니다.
     */
    private String buildWeeklyInsightSystemInstruction() {
        return "당신은 개발자를 위한 주간 기술 뉴스 분석가입니다. 반드시 JSON 객체로만 응답하세요. "
                + "필수 키는 summary, trendAnalysis, developerView 입니다. "
                + "각 값은 한국어로 작성하고 실무 개발자가 바로 이해할 수 있게 구체적으로 설명하세요.";
    }

    /**
     * @date 2026-08-13
     * @desc 모델이 일일 개발 트렌드를 JSON 형식으로 응답하도록 시스템 지시문을 생성합니다.
     */
    private String buildDailyTrendSystemInstruction() {
        return "당신은 개발자를 위한 일일 기술 뉴스 분석가입니다. 반드시 JSON 객체로만 응답하세요. "
                + "필수 키는 keywords(문자열 배열, 3~5개), summary(1~2문장 한국어 요약) 입니다. "
                + "keywords는 명사형 기술 키워드로, summary는 실무 개발자가 바로 이해할 수 있게 구체적으로 작성하세요.";
    }

    /**
     * @date 2026-05-08
     * @desc 주간 분석 기간과 크롤링 뉴스 목록을 사용자 프롬프트로 구성합니다.
     */
    private String buildWeeklyInsightUserPrompt(String prompt, LocalDate weekStartDate, LocalDate weekEndDate) {
        return "다음 기간의 크롤링 뉴스 목록을 기반으로 주간 개발 트렌드 인사이트를 생성하세요.\n"
                + "- 분석 기간: " + weekStartDate + " ~ " + weekEndDate + "\n\n"
                + "뉴스 목록:\n"
                + prompt + "\n\n"
                + "응답 JSON 형식:\n"
                + "{\"summary\":\"...\",\"trendAnalysis\":\"...\",\"developerView\":\"...\"}";
    }

    /**
     * @date 2026-08-13
     * @desc 기준일과 크롤링 뉴스 목록을 일일 개발 트렌드 사용자 프롬프트로 구성합니다.
     */
    private String buildDailyTrendUserPrompt(String prompt, LocalDate targetDate) {
        return "다음 크롤링 뉴스 목록을 기반으로 일일 개발 트렌드를 생성하세요.\n"
                + "- 기준일: " + targetDate + "\n\n"
                + "뉴스 목록:\n"
                + prompt + "\n\n"
                + "응답 JSON 형식:\n"
                + "{\"keywords\":[\"...\",\"...\",\"...\"],\"summary\":\"...\"}";
    }

    /**
     * @date 2026-04-16
     * @desc JSON 응답에서 필수 문자열 값을 읽고 누락 여부를 검증합니다.
     */
    private String readRequiredValue(JsonNode generatedJson, String fieldName) {
        String value = generatedJson.path(fieldName).asText("");
        if (value.isBlank()) {
            throw new LlmClientException(
                    "OPENAI",
                    "missing_required_field",
                    200,
                    "OpenAI 응답에 필수 항목이 누락되었습니다.",
                    "OpenAI 응답 필수 필드 누락: " + fieldName
            );
        }
        return value.trim();
    }

    /**
     * @date 2026-04-16
     * @desc OpenAI HTTP 오류 응답을 코드/메시지 형태로 변환합니다.
     */
    private LlmClientException toLlmClientException(HttpStatusCodeException exception) {
        String body = exception.getResponseBodyAsString();
        String errorCode = "http_" + exception.getStatusCode().value();
        String errorMessage = "OpenAI 호출에 실패했습니다.";

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                String parsedCode = errorNode.path("code").asText("");
                String parsedType = errorNode.path("type").asText("");
                String parsedMessage = errorNode.path("message").asText("");
                if (!parsedCode.isBlank()) {
                    errorCode = parsedCode;
                } else if (!parsedType.isBlank()) {
                    errorCode = parsedType;
                }
                if (!parsedMessage.isBlank()) {
                    errorMessage = parsedMessage;
                }
            }
        } catch (Exception ignored) {
            // JSON 파싱이 실패하면 기본 코드/메시지를 유지합니다.
        }

        return new LlmClientException(
                "OPENAI",
                errorCode,
                exception.getStatusCode().value(),
                mapOpenAiUserMessage(errorCode, errorMessage),
                "OpenAI HTTP 오류(" + exception.getStatusCode().value() + "): " + errorMessage
        );
    }

    /**
     * @date 2026-04-16
     * @desc OpenAI 오류 코드를 관리자 화면용 사용자 메시지로 매핑합니다.
     */
    private String mapOpenAiUserMessage(String errorCode, String fallbackMessage) {
        if ("insufficient_quota".equals(errorCode)) {
            return "OpenAI 사용량 한도(Quota)가 부족합니다. 결제/한도 설정을 확인해주세요.";
        }
        if ("invalid_api_key".equals(errorCode)) {
            return "OpenAI API Key가 유효하지 않습니다. 키 값을 다시 확인해주세요.";
        }
        if ("rate_limit_exceeded".equals(errorCode)) {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
        }
        if ("model_not_found".equals(errorCode)) {
            return "설정된 OpenAI 모델을 찾을 수 없습니다. 모델명을 확인해주세요.";
        }
        if ("context_length_exceeded".equals(errorCode)) {
            return "입력 길이가 모델 한도를 초과했습니다. 프롬프트를 줄여주세요.";
        }
        return fallbackMessage == null || fallbackMessage.isBlank()
                ? "OpenAI 호출 중 오류가 발생했습니다."
                : fallbackMessage;
    }
}
