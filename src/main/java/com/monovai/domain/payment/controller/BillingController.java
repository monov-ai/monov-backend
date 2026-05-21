package com.monovai.domain.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.payment.dto.request.IssueBillingRequest;
import com.monovai.domain.payment.dto.request.PayBillingRequest;
import com.monovai.domain.payment.service.BillingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Billing", description = "정기결제 (Toss 빌링)")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

	private final BillingService billingService;

	@PostMapping("/issue")
	@Operation(summary = "빌링키 발급", description = "authKey + customerKey → billingKey 저장.")
	public ResponseEntity<Map<String, Object>> issue(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody IssueBillingRequest request
	) {
		return ResponseEntity.ok(billingService.issue(userId, request));
	}

	@PostMapping("/pay")
	@Operation(summary = "정기결제 즉시 결제", description = "billingKey 로 즉시 결제 + 구독 생성/충전.")
	public ResponseEntity<Map<String, Object>> pay(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody PayBillingRequest request
	) {
		return ResponseEntity.ok(billingService.pay(userId, request));
	}

	@PostMapping("/cancel")
	@Operation(summary = "구독 해지", description = "currentPeriodEnd 까지 유지, 빌링키 비활성화.")
	public ResponseEntity<Map<String, Object>> cancel(
		@AuthenticationPrincipal Long userId
	) {
		return ResponseEntity.ok(billingService.cancel(userId));
	}
}
