package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Getter
@Setter
public class GenerationRequestForm {

    @NotNull(message = "대상 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;

    @NotBlank(message = "카테고리는 필수입니다.")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다.")
    private String category;

    @NotBlank(message = "톤은 필수입니다.")
    @Size(max = 50, message = "톤은 50자 이하여야 합니다.")
    private String tone;

    @NotBlank(message = "난이도는 필수입니다.")
    @Size(max = 50, message = "난이도는 50자 이하여야 합니다.")
    private String difficulty;
}
