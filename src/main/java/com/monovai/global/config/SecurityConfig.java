package com.monovai.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monovai.global.jwt.ExceptionHandlerFilter;
import com.monovai.global.jwt.JwtAuthenticationFilter;
import com.monovai.global.jwt.core.JwtExtractor;
import com.monovai.global.jwt.core.JwtValidator;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	/** 인증 없이 접근 가능한 경로. 진짜 public 만 명시. */
	private static final String[] PUBLIC_PATHS = {
		// 인프라
		"/actuator/health",
		"/actuator/health/**",
		"/actuator/prometheus",   // 호스트 nginx 가 외부 차단, 내부 prometheus 만 scrape
		"/v3/api-docs/**",
		"/swagger-ui/**",
		// 인증 자체에 필요한 엔드포인트
		"/api/v1/auth/login-uri",
		"/api/v1/auth/sign-in",
		"/api/v1/auth/login",     // OAuth 인가코드 GET 콜백 (Google/Kakao)
		"/api/v1/auth/sign-up",
		"/api/v1/auth/reissue",
		"/api/v1/auth/me",
		// webhook (외부 호출 — 자체 시그니처 검증)
		"/api/v1/webhooks/**",
		// 디버그 (운영 전 제거 또는 @Profile("debug") 격리)
		"/api/v1/_debug/**"
	};

	private final JwtExtractor jwtExtractor;
	private final JwtValidator jwtValidator;
	private final ObjectMapper objectMapper;
	private final CorsConfig corsConfig;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.OPTIONS).permitAll()
			.requestMatchers(PUBLIC_PATHS).permitAll()
			// 템플릿 카탈로그 조회는 공개 (C.1)
			.requestMatchers(HttpMethod.GET, "/api/v1/templates", "/api/v1/templates/**").permitAll()
			.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
			// 데모 단계: 비즈니스 라우트는 인증된 모든 사용자에게 개방 (A.6).
			.requestMatchers("/api/v1/business/**").authenticated()
			.anyRequest().authenticated()
		);

		http
			.addFilter(corsConfig.corsFilter())
			.addFilterBefore(new JwtAuthenticationFilter(jwtExtractor, jwtValidator), UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new ExceptionHandlerFilter(objectMapper), JwtAuthenticationFilter.class);

		return http.build();
	}
}
