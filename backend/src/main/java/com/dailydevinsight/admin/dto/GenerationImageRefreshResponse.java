package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerationImageRefreshResponse {

    private boolean success;
    private String message;
    private String imageUrl;
}

