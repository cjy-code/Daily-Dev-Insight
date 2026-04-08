package com.dailydevinsight.repository;

import com.dailydevinsight.entity.TechNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TechNewsRepository extends JpaRepository<TechNews, Long> {

    List<TechNews> findByNewsDateOrderByIdDesc(LocalDate newsDate);
}
