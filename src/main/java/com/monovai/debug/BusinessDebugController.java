package com.monovai.debug;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.edit.dto.request.EditImageRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.service.EditService;
import com.monovai.domain.business.imagejob.dto.request.GenerateImageRequest;
import com.monovai.domain.business.imagejob.dto.response.ImageJobCreatedResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobResponse;
import com.monovai.domain.business.imagejob.service.ImageJobService;
import com.monovai.domain.business.recommendation.dto.request.CreateRecommendationRequest;
import com.monovai.domain.business.recommendation.dto.response.RecommendationCreatedResponse;
import com.monovai.domain.business.recommendation.dto.response.RecommendationDetailResponse;
import com.monovai.domain.business.recommendation.service.RecommendationService;
import com.monovai.domain.upload.dto.request.IssueUploadUrlRequest;
import com.monovai.domain.upload.dto.response.PresignedUrlResponse;
import com.monovai.domain.upload.service.UploadService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * 검증용 임시 컨트롤러.
 * @AuthenticationPrincipal 우회 — userId 를 query param 으로 직접 받음.
 *
 * 운영 배포 전 반드시 제거 또는 @Profile("debug") 로 격리.
 */
@RestController
@RequestMapping("/_debug")
@RequiredArgsConstructor
public class BusinessDebugController {

	private final RecommendationService recommendationService;
	private final ImageJobService imageJobService;
	private final EditService editService;
	private final UploadService uploadService;

	@PostMapping("/business/recommendations")
	public ResponseEntity<SuccessResponse<RecommendationCreatedResponse>> createRecommendation(
		@RequestParam Long userId,
		@Valid @RequestBody CreateRecommendationRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, recommendationService.create(userId, request)));
	}

	@GetMapping("/business/recommendations/{requestId}")
	public ResponseEntity<SuccessResponse<RecommendationDetailResponse>> getRecommendation(
		@RequestParam Long userId,
		@PathVariable String requestId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, recommendationService.get(userId, requestId)));
	}

	@PostMapping("/business/generate-image")
	public ResponseEntity<SuccessResponse<ImageJobCreatedResponse>> generateImage(
		@RequestParam Long userId,
		@Valid @RequestBody GenerateImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, imageJobService.create(userId, request)));
	}

	@GetMapping("/business/image-job")
	public ResponseEntity<SuccessResponse<ImageJobResponse>> getImageJob(
		@RequestParam Long userId,
		@RequestParam @NotBlank String jobId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, imageJobService.get(userId, jobId)));
	}

	@PostMapping("/business/edit-image")
	public ResponseEntity<SuccessResponse<EditCreatedResponse>> editImage(
		@RequestParam Long userId,
		@Valid @RequestBody EditImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, editService.create(userId, request)));
	}

	// ---------- 업로드 (프론트가 PUT 할 URL 발급만, 실제 업로드는 프론트→S3 직접) ----------

	@PostMapping("/uploads/presigned-url")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> issueUploadUrl(
		@RequestParam Long userId,
		@Valid @RequestBody IssueUploadUrlRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, uploadService.issueUploadUrl(userId, request)));
	}

	@GetMapping("/uploads/presigned-url")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> getDownloadUrl(
		@RequestParam Long userId,
		@RequestParam @NotBlank String key
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, uploadService.getDownloadUrl(userId, key)));
	}
}