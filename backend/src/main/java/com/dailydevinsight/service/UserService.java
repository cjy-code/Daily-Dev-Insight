package com.dailydevinsight.service;

import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * @date 2026-04-15
     * @desc user_id 기준으로 사용자 정보를 조회합니다.
     */
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }
}
