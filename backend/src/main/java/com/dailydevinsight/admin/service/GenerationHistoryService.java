package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.entity.GenerationHistory;
import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerationHistoryService {

    private final GenerationHistoryRepository generationHistoryRepository;

    /**
     * @date 2026-04-15
     * @desc 최근 생성 이력 20건을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<GenerationHistory> findRecentHistory() {
        return generationHistoryRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
