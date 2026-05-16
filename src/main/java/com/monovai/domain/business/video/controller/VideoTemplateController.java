package com.monovai.domain.business.video.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.video.dto.request.ImageToVideoRequest;
import com.monovai.domain.business.video.dto.response.VideoJobCreatedResponse;
import com.monovai.domain.business.video.dto.response.VideoJobResponse;
import com.monovai.domain.business.video.service.VideoTemplateService;
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

@Tag(name = "Business / Image → Video", description = "결과 이미지를 Kling 영상으로 변환")
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class VideoTemplateController {

	private final VideoTemplateService service;

	@PostMapping("/image-to-video")
	@Operation(
		summary = "이미지 → 영상 변환 시작 (Kling)",
		description = """
			결과 이미지 URL 을 입력받아 KIE.ai 의 `kling-2.6/image-to-video` 잡을 생성합니다.

			- `ratio`: `9:16` (기본) / `16:9` / `1:1`
			- `sourceJobId`, `sourceItemId`, `sourceKind` 는 트래킹용 메타데이터 (선택).
			- 결과 폴링: `GET /api/v1/business/video-job?videoId=`
			- 크레딧 차감 없음 (비즈니스 흐름은 무료).
			""",
		requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(examples = @ExampleObject(value = """
				{
				  "imageUrl": "https://.../result.png",
				  "imagePath": "business_results/.../result.png",
				  "ratio": "9:16",
				  "userPrompt": "은은한 카메라 줌인",
				  "sourceJobId": "bizimg_...",
				  "sourceItemId": "V1",
				  "sourceKind": "variant"
				}
				"""))
		)
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "영상 잡 생성 (videoId 반환)"),
		@ApiResponse(responseCode = "400", description = "imageUrl 누락"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<SuccessResponse<VideoJobCreatedResponse>> imageToVideo(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody ImageToVideoRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, service.createImageToVideo(userId, request)));
	}

	@GetMapping("/video-job")
	@Operation(
		summary = "영상 변환 결과 폴링",
		description = "videoId 로 Kling 변환 진행 상태를 조회합니다. status: requested / running / completed / failed."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "403", description = "타인 리소스"),
		@ApiResponse(responseCode = "404", description = "videoId 없음")
	})
	public ResponseEntity<SuccessResponse<VideoJobResponse>> getVideoJob(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "영상 슬러그 (예: bvid_1700000000_abc123)") @RequestParam("videoId") String videoId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, service.get(userId, videoId)));
	}
}
