package com.monovai.domain.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.payment.dto.request.CreatePromotionRequest;
import com.monovai.domain.payment.dto.request.RedeemPromotionRequest;
import com.monovai.domain.payment.service.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Promotions", description = "프로모션 코드")
@RestController
@RequiredArgsConstructor
public class PromotionController {

	private final PromotionService promotionService;

	@PostMapping("/api/v1/admin/promotions")
	@Operation(summary = "프로모션 코드 생성 (관리자)")
	public ResponseEntity<Map<String, Object>> create(
		@Valid @RequestBody CreatePromotionRequest request
	) {
		return ResponseEntity.ok(promotionService.create(request));
	}

	@PostMapping("/api/v1/promotions/redeem")
	@Operation(summary = "프로모션 코드 사용")
	public ResponseEntity<Map<String, Object>> redeem(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody RedeemPromotionRequest request
	) {
		return ResponseEntity.ok(promotionService.redeem(userId, request));
	}
}
