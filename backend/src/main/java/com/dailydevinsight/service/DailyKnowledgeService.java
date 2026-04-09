package com.dailydevinsight.service;

import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyKnowledgeService {

    private final DailyKnowledgeRepository dailyKnowledgeRepository;

    public Optional<DailyKnowledge> findTodayKnowledge(LocalDate targetDate) {
        return dailyKnowledgeRepository.findTopByKnowledgeDateOrderByIdDesc(targetDate);
    }

    public List<DailyKnowledge> findKnowledgeByDateRange(LocalDate startDate, LocalDate endDate) {
        return dailyKnowledgeRepository.findByKnowledgeDateBetweenOrderByKnowledgeDateDescIdDesc(startDate, endDate);
    }

    public List<DailyKnowledge> findWeeklyHotKnowledgeTop6() {
        return dailyKnowledgeRepository.findTop6ByOrderByViewCountDescIdDesc();
    }
}
