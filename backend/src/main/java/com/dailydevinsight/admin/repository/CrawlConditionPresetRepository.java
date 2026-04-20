package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.CrawlConditionPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawlConditionPresetRepository extends JpaRepository<CrawlConditionPreset, Long> {

    List<CrawlConditionPreset> findByActiveTrueOrderByUpdatedAtDescIdDesc();
}
