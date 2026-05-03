package com.monovai.global.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.monovai.global.error.exception.UnauthorizedException;
import com.monovai.global.jwt.core.JwtExtractor;
import com.monovai.global.jwt.core.JwtValidator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	/** filter 자체를 건너뛰어도 안전한 경로 (swagger / actuator / preSignupToken 쓰는 sign-up).
	 *  인증 게이트는 SecurityConfig 가, preSignupToken 검증은 AuthService 가 별도 처리. */
	private static final List<String> SKIP_PATHS = Arrays.asList(
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/actuator/**",
		"/api/v1/auth/sign-up"
	);

	private static final PathMatcher MATCHER = new AntPathMatcher();

	private final JwtExtractor jwtExtractor;
	private final JwtValidator jwtValidator;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String authorization = request.getHeader("Authorization");

		// 1. Authorization 헤더 자체가 없거나 Bearer 형식이 아니면 → SecurityContext 비워두고 통과
		//    (인증 필요한 엔드포인트는 SecurityConfig 가 거부할 것)
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		// 2. Bearer 토큰이 있으면 검증 + SecurityContext 세팅 시도. 실패하면 명시적으로 401.
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		if (jwtValidator.isExpired(token)) {
			throw new UnauthorizedException();
		}

		Long userId = jwtExtractor.extractUserId(token);
		String role = jwtExtractor.extractRole(token);
		log.info("[JwtAuth] authenticated userId={}, role={}", userId, role);

		authenticate(userId, role);
		filterChain.doFilter(request, response);
	}

	private void authenticate(Long userId, String role) {
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(userId, null, authorities)
		);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return SKIP_PATHS.stream().anyMatch(p -> MATCHER.match(p, path));
	}
}