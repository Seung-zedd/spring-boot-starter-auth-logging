package com.example.springboot_starter_auth.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      AuthenticationException exception) throws IOException {
        
        String errorCode = "login_failed";
        String errorMessage = "로그인에 실패했습니다.";
        
        // Determine specific error type
        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String errorCodeFromOAuth2 = oAuth2Exception.getError().getErrorCode();
            
            switch (errorCodeFromOAuth2) {
                case "access_denied":
                    errorCode = "access_denied";
                    errorMessage = "사용자가 인증을 취소했습니다.";
                    break;
                case "invalid_request":
                    errorCode = "invalid_request";
                    errorMessage = "잘못된 요청입니다.";
                    break;
                case "unauthorized_client":
                    errorCode = "unauthorized_client";
                    errorMessage = "인증되지 않은 클라이언트입니다.";
                    break;
                case "server_error":
                    errorCode = "server_error";
                    errorMessage = "서버 오류가 발생했습니다.";
                    break;
                default:
                    errorCode = "unknown_error";
                    errorMessage = "알 수 없는 오류가 발생했습니다.";
                    break;
            }
            
            log.warn("OAuth2 authentication failed: {} - {}", errorCodeFromOAuth2, oAuth2Exception.getError().getDescription());
        } else {
            log.error("Authentication failure: {}", exception.getMessage(), exception);
        }

        // Log failure event with request details
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.warn("OAuth2 login failure - IP: {}, User-Agent: {}, Error: {}", 
                clientIp, userAgent, errorCode);

        // Redirect back to main page with error parameters
        String redirectUrl = UriComponentsBuilder.fromUriString("/main.html")
                .queryParam("error", errorCode)
                .queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build()
                .toUriString();
        
        log.info("Redirecting to main page with error: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}