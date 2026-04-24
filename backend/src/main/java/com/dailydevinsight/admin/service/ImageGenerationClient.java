package com.dailydevinsight.admin.service;

import java.time.LocalDate;

public interface ImageGenerationClient {

    /**
     * @date 2026-04-24
     * @desc 이미지 생성 프롬프트를 기반으로 썸네일 이미지를 생성하고 저장 경로를 반환합니다.
     */
    String generateAndStoreImage(String prompt, LocalDate targetDate, String quality, Integer maxTokens);
}

