package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.dto.CrawlPresetForm;
import com.dailydevinsight.admin.entity.CrawlConditionPreset;
import com.dailydevinsight.admin.repository.CrawlConditionPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlConditionPresetService {

    private static final int MIN_MAX_ARTICLES = 1;
    private static final int MAX_MAX_ARTICLES = 100;
    private static final int MIN_TIMEOUT_SECONDS = 1;
    private static final int MAX_TIMEOUT_SECONDS = 60;
    private static final int MIN_RETRY_COUNT = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int MAX_FILTER_KEYWORD_COUNT = 5;

    private final CrawlConditionPresetRepository crawlConditionPresetRepository;

    /**
     * @date 2026-04-17
     * @desc 활성화된 크롤링 조건 프리셋 목록을 최신 수정 순으로 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrawlConditionPreset> findActivePresets() {
        return crawlConditionPresetRepository.findByActiveTrueOrderByUpdatedAtDescIdDesc();
    }

    /**
     * @date 2026-04-17
     * @desc 크롤링 조건 프리셋을 신규 생성하거나 기존 프리셋을 수정합니다.
     */
    @Transactional
    public CrawlConditionPreset savePreset(CrawlPresetForm form) {
        validatePresetForm(form);

        CrawlConditionPreset originalPreset = resolveOriginalPreset(form.getPresetId());
        LocalDateTime now = LocalDateTime.now();

        CrawlConditionPreset preset = CrawlConditionPreset.builder()
                .id(originalPreset == null ? null : originalPreset.getId())
                .presetName(form.getPresetName().trim())
                .sourceName(form.getSourceName().trim())
                .sourceUrl(form.getSourceUrl().trim())
                .maxArticles(form.getMaxArticles())
                .keywordMatchType(normalizeKeywordMatchType(form.getKeywordMatchType()))
                .includeKeywords(joinAsCsv(normalizeStringList(form.getIncludeKeywords())))
                .includeKeywordOperators(joinAsCsv(normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords())))
                .excludeKeywords(joinAsCsv(normalizeStringList(form.getExcludeKeywords())))
                .targetDomains(joinAsCsv(normalizeStringList(form.getTargetDomains())))
                .connectTimeoutSeconds(form.getConnectTimeoutSeconds())
                .readTimeoutSeconds(form.getReadTimeoutSeconds())
                .retryCount(form.getRetryCount())
                .active(true)
                .createdAt(originalPreset == null ? now : originalPreset.getCreatedAt())
                .updatedAt(now)
                .build();

        return crawlConditionPresetRepository.save(preset);
    }

    /**
     * @date 2026-04-17
     * @desc 프리셋 ID로 크롤링 조건 프리셋을 조회합니다.
     */
    @Transactional(readOnly = true)
    public CrawlConditionPreset findPreset(Long presetId) {
        if (presetId == null) {
            throw new IllegalArgumentException("프리셋 ID는 필수입니다.");
        }
        return crawlConditionPresetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("선택한 프리셋을 찾을 수 없습니다."));
    }

    /**
     * @date 2026-04-17
     * @desc 입력받은 프리셋 요청값의 필수 필드와 허용 범위를 검증합니다.
     */
    private void validatePresetForm(CrawlPresetForm form) {
        if (form == null) {
            throw new IllegalArgumentException("프리셋 요청 값이 없습니다.");
        }
        if (form.getPresetName() == null || form.getPresetName().isBlank()) {
            throw new IllegalArgumentException("프리셋 이름은 필수입니다.");
        }
        if (form.getSourceName() == null || form.getSourceName().isBlank()) {
            throw new IllegalArgumentException("소스 이름은 필수입니다.");
        }
        if (form.getSourceUrl() == null || form.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("소스 URL은 필수입니다.");
        }
        if (form.getMaxArticles() == null
                || form.getMaxArticles() < MIN_MAX_ARTICLES
                || form.getMaxArticles() > MAX_MAX_ARTICLES) {
            throw new IllegalArgumentException("최대 수집 건수는 " + MIN_MAX_ARTICLES + "~" + MAX_MAX_ARTICLES + " 사이여야 합니다.");
        }
        validateKeywordCount(form.getIncludeKeywords(), "포함 키워드");
        validateKeywordCount(form.getExcludeKeywords(), "제외 키워드");
        normalizeKeywordMatchType(form.getKeywordMatchType());
        normalizeIncludeKeywordOperators(form.getIncludeKeywordOperators(), form.getIncludeKeywords());
        validateTimeoutAndRetry(form.getConnectTimeoutSeconds(), form.getReadTimeoutSeconds(), form.getRetryCount());
    }

    /**
     * @date 2026-04-17
     * @desc 연결/응답 타임아웃과 재시도 횟수의 허용 범위를 검증합니다.
     */
    private void validateTimeoutAndRetry(Integer connectTimeoutSeconds, Integer readTimeoutSeconds, Integer retryCount) {
        if (connectTimeoutSeconds == null
                || connectTimeoutSeconds < MIN_TIMEOUT_SECONDS
                || connectTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("연결 타임아웃은 " + MIN_TIMEOUT_SECONDS + "~" + MAX_TIMEOUT_SECONDS + "초 사이여야 합니다.");
        }
        if (readTimeoutSeconds == null
                || readTimeoutSeconds < MIN_TIMEOUT_SECONDS
                || readTimeoutSeconds > MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("응답 타임아웃은 " + MIN_TIMEOUT_SECONDS + "~" + MAX_TIMEOUT_SECONDS + "초 사이여야 합니다.");
        }
        if (retryCount == null || retryCount < MIN_RETRY_COUNT || retryCount > MAX_RETRY_COUNT) {
            throw new IllegalArgumentException("재시도 횟수는 " + MIN_RETRY_COUNT + "~" + MAX_RETRY_COUNT + " 사이여야 합니다.");
        }
    }

    /**
     * @date 2026-04-17
     * @desc 키워드 매칭 타입을 AND/OR 값으로 정규화합니다.
     */
    private String normalizeKeywordMatchType(String keywordMatchType) {
        String normalizedKeywordMatchType = keywordMatchType == null ? "OR" : keywordMatchType.trim().toUpperCase();
        if (!"AND".equals(normalizedKeywordMatchType) && !"OR".equals(normalizedKeywordMatchType)) {
            throw new IllegalArgumentException("키워드 조건은 AND 또는 OR만 허용됩니다.");
        }
        return normalizedKeywordMatchType;
    }

    /**
     * @date 2026-04-20
     * @desc 포함/제외 키워드 개수가 최대 5개를 넘지 않도록 검증합니다.
     */
    private void validateKeywordCount(List<String> keywords, String fieldName) {
        int keywordCount = normalizeStringList(keywords).size();
        if (keywordCount > MAX_FILTER_KEYWORD_COUNT) {
            throw new IllegalArgumentException(fieldName + "는 최대 " + MAX_FILTER_KEYWORD_COUNT + "개까지만 입력할 수 있습니다.");
        }
    }

    /**
     * @date 2026-04-17
     * @desc 포함 키워드 개수에 맞게 연산자 목록을 정규화합니다.
     */
    private List<String> normalizeIncludeKeywordOperators(List<String> operators, List<String> includeKeywords) {
        List<String> normalizedKeywords = normalizeStringList(includeKeywords);
        if (normalizedKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedOperators = normalizeStringList(operators).stream()
                .map(value -> value.toUpperCase())
                .collect(Collectors.toList());

        int requiredOperatorCount = normalizedKeywords.size();
        if (normalizedOperators.size() == requiredOperatorCount - 1) {
            normalizedOperators.add(0, "OR");
        }
        if (normalizedOperators.size() > requiredOperatorCount) {
            normalizedOperators = normalizedOperators.subList(0, requiredOperatorCount);
        }
        while (normalizedOperators.size() < requiredOperatorCount) {
            normalizedOperators.add("OR");
        }

        for (String operator : normalizedOperators) {
            if (!"AND".equals(operator) && !"OR".equals(operator)) {
                throw new IllegalArgumentException("포함 키워드 연산자는 AND 또는 OR만 허용됩니다.");
            }
        }
        return normalizedOperators;
    }

    /**
     * @date 2026-04-17
     * @desc 문자열 목록에서 빈 값과 공백을 제거한 뒤 중복 없는 목록으로 정규화합니다.
     */
    private List<String> normalizeStringList(List<String> sourceList) {
        if (sourceList == null) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-04-17
     * @desc 문자열 목록을 CSV 문자열로 결합합니다.
     */
    private String joinAsCsv(List<String> sourceList) {
        if (sourceList.isEmpty()) {
            return null;
        }
        return String.join(",", sourceList);
    }

    /**
     * @date 2026-04-17
     * @desc CSV 문자열을 문자열 목록으로 분리합니다.
     */
    @Transactional(readOnly = true)
    public List<String> splitCsv(String csvText) {
        if (csvText == null || csvText.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csvText.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * @date 2026-04-17
     * @desc 프리셋 저장 시 수정 대상 원본 프리셋을 조회합니다.
     */
    private CrawlConditionPreset resolveOriginalPreset(Long presetId) {
        if (presetId == null) {
            return null;
        }
        return crawlConditionPresetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("수정 대상 프리셋을 찾을 수 없습니다."));
    }
}
