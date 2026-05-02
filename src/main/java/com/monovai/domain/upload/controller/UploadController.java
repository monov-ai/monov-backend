package com.monovai.domain.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.upload.dto.request.IssueUploadUrlRequest;
import com.monovai.domain.upload.dto.response.PresignedUrlResponse;
import com.monovai.domain.upload.service.UploadService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

	private final UploadService uploadService;

	@PostMapping("/presigned-url")
	@Operation(summary = "업로드용 presigned URL 발급",
		description = "프론트는 응답의 url 로 S3 에 직접 PUT. 응답의 key 를 다른 API (예: recommendations) 에 전달.")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> issueUploadUrl(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody IssueUploadUrlRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, uploadService.issueUploadUrl(userId, request)));
	}

	@GetMapping("/presigned-url")
	@Operation(summary = "다운로드용 presigned URL 재발급",
		description = "기존 S3 key 에 대한 새 7일짜리 GET presigned URL. 만료 임박 시 호출.")
	public ResponseEntity<SuccessResponse<PresignedUrlResponse>> getDownloadUrl(
		@AuthenticationPrincipal Long userId,
		@RequestParam @NotBlank String key
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, uploadService.getDownloadUrl(userId, key)));
	}
}