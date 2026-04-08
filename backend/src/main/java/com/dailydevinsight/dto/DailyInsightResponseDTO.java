package com.dailydevinsight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyInsightResponseDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private List<DailyInsightDTO> dailyKnowledgeList;
    private List<DailyInsightDTO> techNewsList;
    private List<DailyInsightDTO> weeklyHotList;

    // Backward compatibility fields for existing API/template consumers.
    private DailyInsightDTO todayKnowledge;
    private List<DailyInsightDTO> newsList;
}
