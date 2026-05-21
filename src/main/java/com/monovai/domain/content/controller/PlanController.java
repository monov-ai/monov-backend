package com.monovai.domain.content.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.content.service.PlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Content / Plan", description = "주간 콘텐츠 플랜 생성")
@RestController
@RequestMapping("/api/v1/plan")
@RequiredArgsConstructor
public class PlanController {

	private final PlanService planService;

	@PostMapping("/generate")
	@Operation(summary = "주간 플랜 생성", description = "brandKitId + weekStart 기반으로 주간 콘텐츠 플랜을 생성합니다.")
	public ResponseEntity<Map<String, Object>> generate(
		@AuthenticationPrincipal Long userId,
		@RequestBody Map<String, Object> request
	) {
		return ResponseEntity.ok(planService.generate(userId, request));
	}
}
