package com.monovai.domain.user.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.user.dto.request.ConsentRequest;
import com.monovai.domain.user.dto.request.LocaleRequest;
import com.monovai.domain.user.dto.request.OnboardingRequest;
import com.monovai.domain.user.dto.response.OnboardingResponse;
import com.monovai.domain.user.service.UserMeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * monov-web 이관 대상: 온보딩 / locale / consents.
 * 이 라우트들은 SDK 가 평문 {@code { ok, ... }} 를 기대하므로 SuccessResponse envelope 를 쓰지 않고 직접 반환.
 */
@Tag(name = "User / Me", description = "온보딩 / 언어 / 동의")
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserMeController {

	private final UserMeService userMeService;

	@GetMapping("/onboarding")
	@Operation(summary = "온보딩 상태 조회")
	public ResponseEntity<OnboardingResponse> getOnboarding(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(userMeService.getOnboarding(userId));
	}

	@PostMapping("/onboarding")
	@Operation(summary = "온보딩 저장 (직군/유입경로)")
	public ResponseEntity<OnboardingResponse> postOnboarding(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody OnboardingRequest request
	) {
		return ResponseEntity.ok(userMeService.completeOnboarding(userId, request));
	}

	@GetMapping("/locale")
	@Operation(summary = "선호 언어 조회")
	public ResponseEntity<Map<String, Object>> getLocale(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(Map.of("ok", true, "locale", userMeService.getLocale(userId)));
	}

	@PatchMapping("/locale")
	@Operation(summary = "선호 언어 변경 (ko | en | ja)")
	public ResponseEntity<Map<String, Object>> patchLocale(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody LocaleRequest request
	) {
		return ResponseEntity.ok(Map.of("ok", true, "locale", userMeService.updateLocale(userId, request)));
	}

	@PostMapping("/consents")
	@Operation(summary = "약관 동의 + 가입 보너스 크레딧 충전")
	public ResponseEntity<Map<String, Object>> postConsents(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody ConsentRequest request
	) {
		int granted = userMeService.recordConsents(userId, request);
		return ResponseEntity.ok(Map.of("ok", true, "grantedCredits", granted));
	}
}
