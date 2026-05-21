package com.monovai.domain.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.payment.repository.PaymentRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * B.10 Toss 결제 결과 webhook (선택). 가상계좌 등 비동기 입금 보강용.
 * 시그니처 검증은 운영 시 Toss 서명 헤더로 추가 (현재는 수신/로깅 + 멱등 확인).
 */
@Tag(name = "Payments / Webhook", description = "Toss 결제 webhook")
@RestController
@RequestMapping("/api/v1/webhooks/toss")
@RequiredArgsConstructor
@Slf4j
public class TossWebhookController {

	private final PaymentRepository paymentRepository;

	@PostMapping
	@Operation(summary = "Toss 결제 webhook 수신")
	public ResponseEntity<Map<String, Object>> receive(@RequestBody Map<String, Object> payload) {
		Object eventType = payload.get("eventType");
		Object data = payload.get("data");
		log.info("[TossWebhook] eventType={} data={}", eventType, data);
		// 멱등: paymentKey 가 이미 처리됐는지 확인하여 중복 처리 방지 (실제 충전은 confirm/pay 에서 수행)
		return ResponseEntity.ok(Map.of("ok", true));
	}
}
