package com.monovai.domain.business.imagejob.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.imagejob.dto.request.GenerateImageRequest;
import com.monovai.domain.business.imagejob.dto.response.ImageJobCreatedResponse;
import com.monovai.domain.business.imagejob.dto.response.ImageJobResponse;
import com.monovai.domain.business.imagejob.service.ImageJobService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class ImageJobController {

	private final ImageJobService imageJobService;

	@PostMapping("/generate-image")
	@Operation(summary = "이미지 생성 잡 시작", description = "선택된 추천 + 옵션으로 Nanobanana 이미지 생성 잡을 시작합니다. jobId 반환.")
	public ResponseEntity<SuccessResponse<ImageJobCreatedResponse>> generate(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody GenerateImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, imageJobService.create(userId, request)));
	}

	@GetMapping("/image-job")
	@Operation(summary = "이미지 잡 상태 조회", description = "jobId 로 잡 상태와 연관된 모든 edits 를 함께 반환합니다 (v1.1).")
	public ResponseEntity<SuccessResponse<ImageJobResponse>> get(
		@AuthenticationPrincipal Long userId,
		@RequestParam @NotNull Long jobId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, imageJobService.get(userId, jobId)));
	}
}