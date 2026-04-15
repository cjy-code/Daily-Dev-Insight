package com.dailydevinsight.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeneratedKnowledgeResult {

    private String title;
    private String summary;
    private String detail;
}
