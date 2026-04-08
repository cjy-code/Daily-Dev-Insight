package com.dailydevinsight.service;

import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechNewsService {

    private final TechNewsRepository techNewsRepository;

    public List<TechNews> findNewsByDate(LocalDate targetDate) {
        return techNewsRepository.findByNewsDateOrderByIdDesc(targetDate);
    }
}
