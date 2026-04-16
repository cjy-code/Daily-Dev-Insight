package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GenerationPreviewRequest {

    private LocalDate targetDate;
    private String category;
    private String tone;
    private String difficulty;
    private String promptContent;
}
