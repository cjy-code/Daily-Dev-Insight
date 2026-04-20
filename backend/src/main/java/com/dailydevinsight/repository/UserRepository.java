package com.dailydevinsight.repository;

import com.dailydevinsight.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    List<User> findTop30ByOrderByCreatedAtDesc();

    long countByStatusIgnoreCase(String status);

    @Modifying
    @Query("update User u set u.name = :name, u.email = :email, u.updatedAt = :updatedAt where u.id = :id")
    int updateProfileById(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("email") String email,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("update User u set u.password = :password, u.updatedAt = :updatedAt where u.id = :id")
    int updatePasswordById(
            @Param("id") Long id,
            @Param("password") String password,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("update User u set u.status = :status, u.updatedAt = :updatedAt where u.id = :id")
    int updateStatusById(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
