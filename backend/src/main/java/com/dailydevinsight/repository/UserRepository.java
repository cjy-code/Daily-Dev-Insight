package com.dailydevinsight.repository;

import com.dailydevinsight.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    List<User> findTop30ByOrderByCreatedAtDesc();

    long countByStatusIgnoreCase(String status);
}
