package com.monovai.domain.business.recommendation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.recommendation.dto.request.CreateRecommendationRequest;
import com.monovai.domain.business.recommendation.dto.response.RecommendationCreatedResponse;
import com.monovai.domain.business.recommendation.dto.response.RecommendationDetailResponse;
import com.monovai.domain.business.recommendation.service.RecommendationService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;

	@PostMapping
	@Operation(summary = "추천 생성", description = "사용자 입력 + 이미지로 GPT 호출, 3개 추천을 생성하고 requestId 를 반환합니다.")
	public ResponseEntity<SuccessResponse<RecommendationCreatedResponse>> create(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CreateRecommendationRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, recommendationService.create(userId, request)));
	}

	@GetMapping("/{requestId}")
	@Operation(summary = "추천 조회", description = "requestId(slug) 로 추천 결과를 조회합니다.")
	public ResponseEntity<SuccessResponse<RecommendationDetailResponse>> get(
		@AuthenticationPrincipal Long userId,
		@PathVariable String requestId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, recommendationService.get(userId, requestId)));
	}
}