package com.example.springboot_starter_auth.global.auth.user.controller;


import com.example.springboot_starter_auth.global.auth.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    //* This Controller is used for my-page view.

    private final UserService userService;


    // 현재 로그인된 사용자 정보 조회 (JavaScript에서 사용)
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getCurrentUserInfo(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName()); // JWT에서 userId 추출
        String nickname = userService.getUserNickname(userId); // 실제 닉네임 조회
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("nickname", nickname);
        return ResponseEntity.ok(userInfo);
    }

    // 회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdrawUser(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        userService.withdrawUser(userId);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}