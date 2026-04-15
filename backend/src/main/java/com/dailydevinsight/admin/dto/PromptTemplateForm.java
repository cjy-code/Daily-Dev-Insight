package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromptTemplateForm {

    private Long id;
    private String name;
    private String description;
    private String templateContent;
}
