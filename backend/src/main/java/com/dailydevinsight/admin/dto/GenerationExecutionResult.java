package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerationExecutionResult {

    private boolean success;
    private String message;
    private Long createdKnowledgeId;
}
