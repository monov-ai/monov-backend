package com.monovai.domain.business.history.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.business.history.dto.response.HistoryResponse;
import com.monovai.domain.business.history.service.HistoryService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Business / History", description = "비즈니스 히스토리 통합 조회")
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class HistoryController {

	private final HistoryService historyService;

	@GetMapping("/history")
	@Operation(
		summary = "비즈니스 히스토리 조회",
		description = """
			ImageJob (variants + edits) 과 VideoTemplate(source=business) 을 통합 조회합니다.

			- `limit` 1~200, 기본 50.
			- 응답은 createdAt 내림차순.
			- `kind` 필드로 항목 구분: `"imagejob"` / `"template"`.
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "limit 범위 무효"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<SuccessResponse<HistoryResponse>> history(
		@AuthenticationPrincipal Long userId,
		@Parameter(description = "최대 항목 수 (1~200, 기본 50)", example = "50")
		@RequestParam(value = "limit", required = false, defaultValue = "50") int limit
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, historyService.list(userId, limit)));
	}
}
