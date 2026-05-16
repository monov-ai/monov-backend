package com.monovai.domain.business.imagejob.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.imagejob.dto.request.FavoriteToggleRequest;
import com.monovai.domain.business.imagejob.dto.request.GenerateImageRequest;
import com.monovai.domain.business.imagejob.dto.request.TemplateFavoriteToggleRequest;
import com.monovai.domain.business.imagejob.dto.response.FavoriteToggleResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobCreatedResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobResponse;
import com.monovai.domain.business.imagejob.service.FavoriteService;
import com.monovai.domain.business.imagejob.service.ImageJobService;
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
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Tag(name = "Business / Image Jobs", description = "추천 → 이미지 생성 잡 + 즐겨찾기")
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class ImageJobController {

	private final ImageJobService imageJobService;
	private final FavoriteService favoriteService;

	@PostMapping("/generate-image")
	@Operation(
		summary = "이미지 생성 잡 시작",
		description = """
			선택된 추천(들) + 옵션으로 Nanobanana 이미지 생성 잡을 시작합니다.

			- `recommendationIds` 다중 (1개 이상). `recommendationId` 단수형도 호환.
			- v2.0: `angle` 은 옵셔널 (deprecated, 결과 단계의 angle_change 에서 결정).
			- `ratio` 는 `1:1` / `9:16` / `4:3` / `3:4` 지원.
			- 응답으로 `jobId` 만 반환. 결과 폴링은 `GET /api/v1/business/image-job?jobId=`.
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{
				  "requestId": "bizrec_1700000000_abc123",
				  "recommendationIds": ["warm_wood_studio", "stone_luxury_studio"],
				  "lighting": "natural",
				  "ratio": "1:1"
				}
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "잡 생성 (jobId 반환)"),
		@ApiResponse(responseCode = "400", description = "필수 누락 / enum 검증 실패"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "requestId / recommendationId 없음")
	})
	public ResponseEntity<SuccessResponse<ImageJobCreatedResponse>> generate(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody GenerateImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, imageJobService.create(userId, request)));
	}

	@GetMapping("/image-job")
	@Operation(
		summary = "이미지 잡 상태 조회 (폴링)",
		description = """
			jobId(slug) 로 잡 상태와 연관된 모든 edits 를 함께 반환합니다 (v2.0).

			응답에는:
			- `job.variants[].resultImageUrl` (7d presigned)
			- `job.fetchableImageUrl` / `fetchableProductImageUrls` / `fetchableReferenceImageUrls` (60m presigned, 인페인팅 같은 클라이언트 fetch 용)
			- `edits[]` 의 base / result presigned URL
			- `favoriteVariantIds`, `favoriteEditIds`

			**폴링 종료 조건:** `job.status` ∈ {completed, failed, partial} AND 모든 `edits[*].status` ∈ {completed, failed}.
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "jobId 누락"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "jobId 없음")
	})
	public ResponseEntity<SuccessResponse<ImageJobResponse>> get(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "이미지 잡 슬러그 (예: bizimg_1700000000_xyz789)", example = "bizimg_1700000000_xyz789")
		@RequestParam @NotBlank String jobId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, imageJobService.get(userId, jobId)));
	}

	@PatchMapping("/favorite")
	@Operation(
		summary = "Variant/Edit 즐겨찾기 토글",
		description = "특정 variantId 또는 editId 를 ImageJob 의 favoriteVariantIds/favoriteEditIds 배열에 추가/제거합니다.",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{ "jobId": "bizimg_...", "kind": "variant", "id": "V1", "favorite": true }
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "토글 성공 — 갱신된 즐겨찾기 배열 반환"),
		@ApiResponse(responseCode = "400", description = "kind 무효"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "jobId 없음")
	})
	public ResponseEntity<SuccessResponse<FavoriteToggleResponse>> favorite(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody FavoriteToggleRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_UPDATE, favoriteService.toggleJobFavorite(userId, request)));
	}

	@PatchMapping("/template-favorite")
	@Operation(
		summary = "템플릿 결과물 (영상/이미지) 즐겨찾기 토글",
		description = "VideoTemplate (영상 변환 결과 등) 의 favorite 필드를 토글합니다.",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{ "itemId": "bvid_...", "mediaType": "video", "favorite": true }
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "토글 성공"),
		@ApiResponse(responseCode = "400", description = "mediaType 무효"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "itemId 없음")
	})
	public ResponseEntity<SuccessResponse<FavoriteService.TemplateFavoriteResult>> templateFavorite(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody TemplateFavoriteToggleRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_UPDATE,
				favoriteService.toggleTemplateFavorite(userId, request.itemId(), request.mediaType(), request.favorite())));
	}
}
