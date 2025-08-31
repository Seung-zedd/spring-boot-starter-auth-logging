package com.example.springboot_starter_auth.global.auth.user.service;


import com.example.springboot_starter_auth.global.auth.user.entity.User;
import com.example.springboot_starter_auth.global.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    // Get user nickname by ID (for JavaScript display)
    public String getUserNickname(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return user.getNickname();
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 사용자 삭제
        userRepository.delete(user);
    }
}