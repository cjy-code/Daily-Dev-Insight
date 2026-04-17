package com.dailydevinsight.repository;

import com.dailydevinsight.entity.TechNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TechNewsRepository extends JpaRepository<TechNews, Long> {

    List<TechNews> findByNewsDateOrderByIdDesc(LocalDate newsDate);

    Optional<TechNews> findTopByOrderByIdDesc();

    boolean existsByUrl(String url);

    @Modifying
    @Query("update TechNews n set n.viewCount = coalesce(n.viewCount, 0) + 1 where n.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
