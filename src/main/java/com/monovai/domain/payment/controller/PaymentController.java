package com.monovai.domain.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.payment.dto.request.ConfirmPaymentRequest;
import com.monovai.domain.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payments", description = "단건 결제 (Toss)")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/confirm")
	@Operation(summary = "단건 결제 승인", description = "Toss confirm → 패키지 매핑 크레딧 충전.")
	public ResponseEntity<Map<String, Object>> confirm(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody ConfirmPaymentRequest request
	) {
		return ResponseEntity.ok(paymentService.confirm(userId, request));
	}
}
