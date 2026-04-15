package com.dailydevinsight.admin.repository;

import com.dailydevinsight.admin.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findTopByOrderByIdDesc();

    Optional<PromptTemplate> findTopByActiveTrueOrderByUpdatedAtDesc();

    List<PromptTemplate> findAllByOrderByUpdatedAtDesc();
}
