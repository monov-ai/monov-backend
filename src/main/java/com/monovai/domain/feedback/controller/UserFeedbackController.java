package com.monovai.domain.feedback.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.feedback.dto.request.SubmitFeedbackRequest;
import com.monovai.domain.feedback.service.UserFeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * §5: 사용자가 결과물을 다운로드한 뒤 별점/태그/메모를 남기는 endpoint.
 * 응답은 {@code { ok: true }} 형태 (web SDK 호환).
 */
@Tag(name = "Me / Feedback", description = "다운로드 후 피드백")
@RestController
@RequestMapping("/api/v1/me/feedback")
@RequiredArgsConstructor
public class UserFeedbackController {

	private final UserFeedbackService feedbackService;

	@PostMapping
	@Operation(summary = "다운로드 피드백 저장")
	public ResponseEntity<Map<String, Object>> submit(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody SubmitFeedbackRequest request
	) {
		feedbackService.submit(userId, request);
		return ResponseEntity.ok(Map.of("ok", true));
	}
}
