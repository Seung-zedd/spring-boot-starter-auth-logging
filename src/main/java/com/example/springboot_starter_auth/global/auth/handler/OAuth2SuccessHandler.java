package com.example.springboot_starter_auth.global.auth.handler;

import com.example.springboot_starter_auth.global.auth.jwt.JwtTokenProvider;
import com.example.springboot_starter_auth.global.auth.user.entity.User;
import com.example.springboot_starter_auth.global.auth.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            // Extract user information from Kakao OAuth2User
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            
            Long kakaoId = oAuth2User.getAttribute("id");
            String email = (String) kakaoAccount.get("email");
            String nickname = (String) profile.get("nickname");

            log.info("OAuth2 Success - Kakao ID: {}, Email: {}, Nickname: {}", kakaoId, email, nickname);

            // Find or create user
            User user = userRepository.findByKakaoId(kakaoId)
                    .orElseGet(() -> createNewUser(kakaoId, email, nickname));

            // Generate JWT tokens
            String accessToken = jwtTokenProvider.createAccessToken(user.getId());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            // Set refresh token as HTTP-only cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofSeconds(1209600)) // 14 days
                    .build();
            
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Redirect to home page with access token as query parameter
            String redirectUrl = UriComponentsBuilder.fromUriString("/home.html")
                    .queryParam("token", accessToken)
                    .build()
                    .toUriString();
            
            log.info("OAuth2 login successful - redirecting user: {} (ID: {}) to {}", 
                    user.getNickname(), user.getId(), redirectUrl);
            
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error processing OAuth2 success for user: {}", oAuth2User.getAttribute("id"), e);
            response.sendRedirect("/main.html?error=auth_processing_failed");
        }
    }

    private User createNewUser(Long kakaoId, String email, String nickname) {
        User newUser = User.builder()
                .kakaoId(kakaoId)
                .email(email)
                .nickname(nickname)
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("New user created: {} (Kakao ID: {})", nickname, kakaoId);
        return savedUser;
    }
}