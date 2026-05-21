package com.monovai.domain.auth.controller;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.auth.dto.request.LoginUriRequest;
import com.monovai.domain.auth.dto.request.SignInRequest;
import com.monovai.domain.auth.dto.request.SignupRequest;
import com.monovai.domain.auth.dto.response.AuthMeResponse;
import com.monovai.domain.auth.dto.response.AuthResponse;
import com.monovai.domain.auth.dto.response.JwtResponse;
import com.monovai.domain.auth.dto.response.LoginUriResponse;
import com.monovai.domain.auth.dto.response.OAuthUserInformation;
import com.monovai.domain.auth.dto.response.SignUpResponse;
import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.auth.service.AuthService;
import com.monovai.domain.auth.service.SocialService;
import com.monovai.global.annotation.DisableSwaggerSecurity;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Value("${app.frontend-base-url:https://www.monov-ai.com}")
	private String frontendBaseUrl;

	/**
	 * C.6: OAuth 인가코드 GET 콜백. Google/Kakao 가 redirect_uri 로 code 를 GET 으로 보냄.
	 * provider 는 query param 또는 쿠키(mv_oauth_provider)로 식별. 로그인 후 프론트로 redirect.
	 * - 기존 회원: accessToken/refreshToken 을 쿼리로 전달 (프론트가 쿠키 set)
	 * - 신규 회원: needSignUp=true + preSignupToken 전달
	 */
	@DisableSwaggerSecurity
	@GetMapping("/login")
	@Operation(summary = "OAuth 인가코드 콜백 (GET)",
		description = "Google/Kakao 가 redirect 한 code 를 받아 로그인 후 프론트엔드로 리다이렉트합니다.")
	public ResponseEntity<Void> oauthCallback(
		@RequestParam("code") String code,
		@RequestParam(value = "provider", required = false) String providerParam,
		@CookieValue(value = "mv_oauth_provider", required = false) String providerCookie,
		@CookieValue(value = "mv_oauth_cb", required = false) String callbackUrl
	) {
		String provider = providerParam != null ? providerParam
			: (providerCookie != null ? providerCookie : "google");
		SocialType socialType = SocialType.from(provider.toUpperCase());
		SocialService socialService = authService.getSocialServiceByType(socialType);

		String decoded = URLDecoder.decode(code, StandardCharsets.UTF_8).trim();
		OAuthUserInformation userInfo = socialService.getUserInfo(decoded);
		AuthResponse auth = authService.login(userInfo);

		String base = (callbackUrl != null && !callbackUrl.isBlank()) ? callbackUrl : frontendBaseUrl;
		String redirect;
		if (auth.needSignUp()) {
			redirect = base + "?needSignUp=true&preSignupToken=" + enc(auth.preSignupToken());
		} else {
			redirect = base + "?accessToken=" + enc(auth.tokenResponse().accessToken())
				+ "&refreshToken=" + enc(auth.tokenResponse().refreshToken());
		}
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(redirect))
			.build();
	}

	private static String enc(String v) {
		return v == null ? "" : URLEncoder.encode(v, StandardCharsets.UTF_8);
	}

	@DisableSwaggerSecurity
	@GetMapping(value = "/login-uri")
	@Operation(summary = "소셜 로그인 URL 반환", description = "소셜 로그인 URL을 반환합니다.")
	public ResponseEntity<SuccessResponse<LoginUriResponse>> processLoginUri(@Valid LoginUriRequest request) {
		SocialType socialType = SocialType.from(request.socialType());
		SocialService socialService = authService.getSocialServiceByType(socialType);
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH, socialService.getAuthorizationUri()));
	}

	@DisableSwaggerSecurity
	@PostMapping(value = "/sign-in")
	@Operation(summary = "소셜 로그인",
		description = "소셜 로그인에서 발급받은 인가코드를 통해, 로그인을 진행합니다.")
	public ResponseEntity<SuccessResponse<AuthResponse>> processSignIn(@Valid @RequestBody SignInRequest request) {
		SocialType socialType = SocialType.from(request.socialType());
		SocialService socialService = authService.getSocialServiceByType(socialType);

		String code = URLDecoder.decode(request.code(), StandardCharsets.UTF_8).trim();
		OAuthUserInformation userInfo = socialService.getUserInfo(code);

		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH, authService.login(userInfo)));
	}

	@PostMapping(value = "/sign-up")
	@Operation(summary = "회원 가입",
		description = "sign-in 응답으로 받은 preSignupToken 을 Authorization: Bearer <token> 으로 보내야 합니다.")
	public ResponseEntity<SuccessResponse<SignUpResponse>> processSignup(
		@RequestHeader("Authorization") @NotEmpty(message = "임시 토큰이 누락되었습니다.") String authorization,
		@Valid @RequestBody SignupRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, authService.register(authorization, request)));
	}

	@GetMapping(value = "/reissue")
	@Operation(summary = "Access Token 재발급",
		description = "Refresh Token 을 Authorization: Bearer <refreshToken> 으로 보내면 새 access token 을 발급합니다.")
	public ResponseEntity<SuccessResponse<JwtResponse>> processReissue(
		@RequestHeader("Authorization") @NotEmpty(message = "해당 api에는 authorization 헤더가 필수입니다.") String authorization
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, authService.reIssueToken(authorization)));
	}

	@DeleteMapping("/withdraw")
	@Operation(summary = "회원탈퇴 API",
		description = "회원 탈퇴를 진행합니다. 진행 중인 잡 수(pendingResources)와 데이터 잔존 안내를 함께 반환합니다.")
	public ResponseEntity<SuccessResponse<com.monovai.domain.auth.dto.response.WithdrawResponse>> withdraw(
		@AuthenticationPrincipal Long userId
	) {
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_DELETE, authService.withdraw(userId)));
	}

	@PostMapping("/logout")
	@Operation(summary = "로그아웃", description = "refresh token 을 무효화합니다 (access token 은 만료까지 유효).")
	public ResponseEntity<SuccessResponse<Void>> logout(
		@AuthenticationPrincipal Long userId
	) {
		authService.logout(userId);
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_LOGOUT));
	}

	@GetMapping("/me")
	@Operation(summary = "내 정보 조회",
		description = "Authorization 헤더가 있으면 인증된 사용자 정보, 없거나 무효면 user=null.")
	public ResponseEntity<SuccessResponse<AuthMeResponse>> me(
		@AuthenticationPrincipal Long userId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, authService.getMe(userId)));
	}
}
