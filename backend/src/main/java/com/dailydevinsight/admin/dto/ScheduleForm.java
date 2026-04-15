package com.dailydevinsight.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleForm {

    private Boolean enabled;
    private String cronExpression;
    private String category;
    private String tone;
    private String difficulty;
}
