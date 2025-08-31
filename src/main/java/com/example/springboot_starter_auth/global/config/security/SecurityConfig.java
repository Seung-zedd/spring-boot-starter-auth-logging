package com.example.springboot_starter_auth.global.config.security;

import com.example.springboot_starter_auth.global.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.profiles.active:local}")  // 기본 local
    private String activeProfile;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. REST API 이므로, CSRF 보안 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정 (프론트엔드 연동 시 필요)
                // CORS 설정 활성화 (preflight 허용 – 당신의 질문에 따라 수정)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // disable 대신 이걸로 교체

                // 2. 세션을 사용하지 않으므로, 세션 관리 정책을 STATELESS로 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 예외 처리 설정 (인증/인가 실패 시)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "접근이 거부되었습니다"))
                )


                // 4. HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authorize -> {
                    if ("local".equals(activeProfile)) {
                        authorize
                                .requestMatchers("/v3/api-docs", "/swagger-ui/**").permitAll();
                    }


                    // OPTIONS 메서드 (CORS preflight) 전체 허용 – 401 에러 방지
                    authorize
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/main.html").permitAll()

                            // 카카오 로그인 처리 API 경로는 인증 없이 모두 허용
                            .requestMatchers("/auth/kakao/callback").permitAll()
                            .requestMatchers("/auth/kakao/login-url").permitAll()
                            .requestMatchers("/api/check-auth").permitAll();

                    // 테스트용 인증 API 허용
                    // local 환경에서만 공개
                    if ("local".equals(activeProfile)) {
                        authorize.requestMatchers("/test/auth/**").permitAll();
                    }

                    authorize


                            // 익명 사용자용 리소스 (메인 랜딩 페이지용만)
                            .requestMatchers("/", "/main.html", "/app.js", "/*.css", "/img/**", "/music/**", "/css/**", "/error", "/favicon.ico").permitAll()
                            
                            // 인증된 사용자용 리소스 (로그인 후 접근 가능)
                            .requestMatchers("/home.html", "/app_*.js", "/views/**").authenticated()

                            // /home과 .well-known 경로 허용 추가 (에러 방지)
                            .requestMatchers("/.well-known/**").permitAll()

                            // 위에서 지정한 경로 외의 모든 요청은 반드시 인증(로그인) 필요
                            .anyRequest().authenticated();

                })

                //* 5. (중요) 우리가 직접 만든 JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 세부 설정 (허용 origin, method 등)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:*", "http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));  // OPTIONS 명시 허용 (preflight)
        configuration.setAllowedHeaders(List.of("*"));  // 모든 헤더 허용
        configuration.setAllowCredentials(true);  // 쿠키/credentials 허용
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));  // 노출 헤더 추가 (필요 시)
        configuration.setMaxAge(3600L);  // preflight 캐시 시간 (1시간)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 적용
        return source;
    }

    // .well-known 완전 무시 (옵션, Security 우회)
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/.well-known/**");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // getClaim 메서드 사용 – Object로 가져와 안전 캐스팅
            Object scopeClaim = jwt.getClaim("scope");  // "scope" 클레임 가져오기 (Kakao 토큰 맞춤)
            List<String> scopes = new ArrayList<>();

            if (scopeClaim instanceof List<?> list) {
                scopes = list.stream()
                        .filter(item -> item instanceof String)
                        .map(item -> (String) item)
                        .collect(Collectors.toList());
            } else if (scopeClaim instanceof String str) {
                scopes = List.of(str.split(" "));  // 문자열 split (e.g., "scope1 scope2")
            }  // else: 빈 리스트 (클레임 없음 – 에러 방지)

            // scopes를 GrantedAuthority로 변환
            return scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))  // prefix 커스텀 (필요 시 "ROLE_")
                    .collect(Collectors.toList());
        });

        return converter;
    }

}
