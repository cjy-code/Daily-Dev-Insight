package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerationPreviewResponse {

    private boolean success;
    private String errorCode;
    private String message;

    private String generatedTitle;
    private String generatedSummary;
    private String generatedDetail;
    private String generatedImageUrl;

    private boolean hasPreviousResult;
    private String previousTitle;
    private String previousSummary;
    private String previousDetail;
    private String previousImageUrl;
}
