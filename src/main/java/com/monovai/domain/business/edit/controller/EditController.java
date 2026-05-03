package com.monovai.domain.business.edit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.edit.dto.request.EditImageRequest;
import com.monovai.domain.business.edit.dto.response.EditCreatedResponse;
import com.monovai.domain.business.edit.service.EditService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class EditController {

	private final EditService editService;

	@PostMapping("/edit-image")
	@Operation(summary = "빠른 수정 적용", description = "결과 이미지에 빠른 수정을 적용합니다. baseId 로 체이닝 가능. editId 반환.")
	public ResponseEntity<SuccessResponse<EditCreatedResponse>> edit(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody EditImageRequest request
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_CREATE, editService.create(userId, request)));
	}
}