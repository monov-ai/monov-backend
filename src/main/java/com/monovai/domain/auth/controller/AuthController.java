package com.monovai.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.auth.dto.request.LoginUriRequest;
import com.monovai.domain.auth.dto.request.SignInRequest;
import com.monovai.domain.auth.dto.request.SignupRequest;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

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

		OAuthUserInformation userInfo = socialService.getUserInfoByAccessToken(request.accessToken());

		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH, authService.login(userInfo)));
	}

	@DisableSwaggerSecurity
	@PostMapping(value = "/sign-up")
	@Operation(summary = "회원 가입", description = "회원 가입을 진행합니다.")
	public ResponseEntity<SuccessResponse<SignUpResponse>> processSignup(
		@RequestHeader("Authorization") @NotEmpty(message = "임시 토큰이 누락되었습니다.") String authorization,
		@Valid @RequestBody SignupRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, authService.register(authorization, request)));
	}

	@DisableSwaggerSecurity
	@GetMapping(value = "/reissue")
	@Operation(summary = "Access Token 재발급", description = "Refresh Token을 통해 Access Token을 재발급합니다.")
	public ResponseEntity<SuccessResponse<JwtResponse>> processReissue(
		@RequestHeader("Authorization") @NotEmpty(message = "해당 api에는 authorization 헤더가 필수입니다.") String authorization
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, authService.reIssueToken(authorization)));
	}

	@DeleteMapping("/withdraw")
	@Operation(summary = "회원탈퇴 API", description = "회원 탈퇴를 진행합니다")
	public ResponseEntity<SuccessResponse<Void>> withdraw(
		@AuthenticationPrincipal Long userId
	) {
		authService.withdraw(userId);
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_DELETE));
	}
}
