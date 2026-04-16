package com.dailydevinsight.admin.service;

import lombok.Getter;

@Getter
public class LlmClientException extends RuntimeException {

    private final String provider;
    private final String errorCode;
    private final int httpStatus;
    private final String userMessage;

    /**
     * @date 2026-04-16
     * @desc LLM 연동 예외 정보를 표준 구조로 생성합니다.
     */
    public LlmClientException(String provider, String errorCode, int httpStatus, String userMessage, String technicalMessage) {
        super(technicalMessage);
        this.provider = provider;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
    }
}
