package com.dailydevinsight.dto;

/**
 * @date 2026-04-13
 * @desc 상세 페이지에서 사용하는 인사이트 콘텐츠 타입을 정의합니다.
 */
public enum InsightContentType {
    KNOWLEDGE("knowledge"),
    NEWS("news");

    private final String value;

    InsightContentType(String value) {
        this.value = value;
    }

    /**
     * @date 2026-04-13
     * @desc 문자열 타입 값을 enum으로 변환합니다.
     */
    public static InsightContentType from(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("콘텐츠 타입이 비어 있습니다.");
        }

        for (InsightContentType contentType : values()) {
            if (contentType.value.equalsIgnoreCase(type.trim())) {
                return contentType;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 콘텐츠 타입입니다: " + type);
    }

    /**
     * @date 2026-04-13
     * @desc 템플릿/응답에서 사용하는 문자열 타입 값을 반환합니다.
     */
    public String getValue() {
        return value;
    }
}
