package com.dailydevinsight.service;

import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @date 2026-04-15
     * @desc user_id 기준으로 사용자 정보를 조회합니다.
     */
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    /**
     * @date 2026-04-20
     * @desc user_id 기반 사용자 정보를 조회하고 없으면 예외를 발생시킵니다.
     */
    public User findRequiredByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * @date 2026-04-20
     * @desc 회원정보 수정 입력값을 검증하고 이름/이메일을 수정합니다.
     */
    @Transactional
    public void updateProfile(String loginUserId, String name, String email) {
        User user = findRequiredByUserId(loginUserId);
        String normalizedName = normalizeRequiredValue(name, "이름");
        String normalizedEmail = normalizeRequiredValue(email, "이메일");

        userRepository.findByEmail(normalizedEmail)
                .filter(foundUser -> !foundUser.getId().equals(user.getId()))
                .ifPresent(foundUser -> {
                    throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
                });

        userRepository.updateProfileById(user.getId(), normalizedName, normalizedEmail, LocalDateTime.now());
    }

    /**
     * @date 2026-04-20
     * @desc 현재 비밀번호 검증 후 새 비밀번호로 변경합니다.
     */
    @Transactional
    public void changePassword(String loginUserId, String currentPassword, String newPassword, String newPasswordConfirm) {
        User user = findRequiredByUserId(loginUserId);
        String normalizedCurrentPassword = normalizeRequiredValue(currentPassword, "현재 비밀번호");
        String normalizedNewPassword = normalizeRequiredValue(newPassword, "새 비밀번호");
        String normalizedNewPasswordConfirm = normalizeRequiredValue(newPasswordConfirm, "새 비밀번호 확인");

        if (!passwordEncoder.matches(normalizedCurrentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (!normalizedNewPassword.equals(normalizedNewPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 값이 일치하지 않습니다.");
        }
        if (normalizedNewPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }
        if (passwordEncoder.matches(normalizedNewPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 동일한 값으로 변경할 수 없습니다.");
        }

        userRepository.updatePasswordById(user.getId(), passwordEncoder.encode(normalizedNewPassword), LocalDateTime.now());
    }

    /**
     * @date 2026-04-20
     * @desc 사용자 상태를 WITHDRAWN으로 변경합니다.
     */
    @Transactional
    public void withdraw(String loginUserId, String currentPassword) {
        User user = findRequiredByUserId(loginUserId);
        String normalizedCurrentPassword = normalizeRequiredValue(currentPassword, "비밀번호");

        if (!passwordEncoder.matches(normalizedCurrentPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        userRepository.updateStatusById(user.getId(), "WITHDRAWN", LocalDateTime.now());
    }

    /**
     * @date 2026-04-20
     * @desc 필수 문자열 파라미터를 trim 처리하고 공백 여부를 검증합니다.
     */
    private String normalizeRequiredValue(String rawValue, String fieldName) {
        String normalizedValue = rawValue == null ? "" : rawValue.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "을(를) 입력해주세요.");
        }
        return normalizedValue;
    }
}
