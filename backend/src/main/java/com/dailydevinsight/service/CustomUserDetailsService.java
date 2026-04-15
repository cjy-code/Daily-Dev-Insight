package com.dailydevinsight.service;

import com.dailydevinsight.entity.User;
import com.dailydevinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @date 2026-04-15
     * @desc user_id 기준으로 사용자를 조회하고 스프링 시큐리티 사용자로 변환합니다.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        String role = user.getRole() == null ? "USER" : user.getRole().trim().toUpperCase();
        boolean isActive = "ACTIVE".equalsIgnoreCase(user.getStatus());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                .disabled(!isActive)
                .build();
    }
}
