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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Business / Recommendations", description = "비즈니스 추천 생성 / 조회")
@RestController
@RequestMapping("/api/v1/business/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;

	@PostMapping
	@Operation(
		summary = "추천 생성",
		description = """
			사용자 입력 + 이미지(들) 로 GPT 를 호출하여 3개의 컨셉 추천을 생성합니다.

			- `style` 이 `studio` 면 productImage(URL 또는 path) 가 1개 이상 필수입니다.
			- v2.0: 슬롯당 ≤4장의 다중 이미지를 지원합니다 (`productImageUrls`, `referenceImageUrls`).
			- v1 호환: `productImageUrl` / `referenceImageUrl` 단수형도 받습니다 (1-element 로 승격).
			- 응답으로 `requestId` 만 돌려주며, 결과는 `GET /api/v1/business/recommendations/{requestId}` 로 조회합니다.
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(name = "studio 예시", value = """
				{
				  "style": "studio",
				  "description": "고급스럽고 따뜻한 느낌, 자연광",
				  "productImageUrls": ["https://.../p1.jpg", "https://.../p2.jpg"],
				  "productImagePaths": ["user_uploads/.../p1.jpg"],
				  "referenceImageUrls": ["https://.../r1.jpg"]
				}
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "추천 생성 시작/성공 (requestId 반환)"),
		@ApiResponse(responseCode = "400", description = "style 무효 / studio 에 제품 이미지 없음 / 슬롯당 4장 초과"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "502", description = "GPT 호출/응답 형식 오류")
	})
	public ResponseEntity<SuccessResponse<RecommendationCreatedResponse>> create(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CreateRecommendationRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, recommendationService.create(userId, request)));
	}

	@GetMapping("/{requestId}")
	@Operation(
		summary = "추천 결과 조회",
		description = "requestId(slug) 로 추천 결과를 조회합니다. 본인 소유만 접근 가능."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스 접근"),
		@ApiResponse(responseCode = "404", description = "requestId 없음")
	})
	public ResponseEntity<SuccessResponse<RecommendationDetailResponse>> get(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "추천 슬러그 (예: bizrec_1700000000_abc123)", example = "bizrec_1700000000_abc123")
		@PathVariable String requestId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, recommendationService.get(userId, requestId)));
	}
}
