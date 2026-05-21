package com.monovai.external.toss.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.monovai.external.toss.properties.TossProperties;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toss Payments API 호출. 시크릿 키는 Basic Auth (key + ":") 로 인코딩.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossClient {

	private final TossProperties properties;
	private final RestClient http = RestClient.create();

	/** 단건 결제 승인. */
	@SuppressWarnings("unchecked")
	public Map<String, Object> confirmPayment(String paymentKey, String orderId, int amount) {
		Map<String, Object> body = Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount);
		try {
			return http.post()
				.uri(URI.create(properties.getBaseUrl() + "/v1/payments/confirm"))
				.header(HttpHeaders.AUTHORIZATION, basic(properties.getSecretKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			log.error("[Toss] confirm 실패 orderId={}", orderId, e);
			throw new BusinessException(ErrorCode.TOSS_CONFIRM_FAILED);
		}
	}

	/** 빌링키 발급 (authKey + customerKey). */
	@SuppressWarnings("unchecked")
	public Map<String, Object> issueBillingKey(String authKey, String customerKey) {
		Map<String, Object> body = Map.of("authKey", authKey, "customerKey", customerKey);
		try {
			return http.post()
				.uri(URI.create(properties.getBaseUrl() + "/v1/billing/authorizations/issue"))
				.header(HttpHeaders.AUTHORIZATION, basic(properties.getBillingSecretKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			log.error("[Toss] billing issue 실패 customerKey={}", customerKey, e);
			throw new BusinessException(ErrorCode.TOSS_BILLING_FAILED);
		}
	}

	/** 빌링키로 즉시 결제. */
	@SuppressWarnings("unchecked")
	public Map<String, Object> payWithBillingKey(String billingKey, String customerKey, int amount,
		String orderId, String orderName, String customerEmail) {
		Map<String, Object> body = Map.of(
			"customerKey", customerKey,
			"amount", amount,
			"orderId", orderId,
			"orderName", orderName,
			"customerEmail", customerEmail == null ? "" : customerEmail
		);
		try {
			return http.post()
				.uri(URI.create(properties.getBaseUrl() + "/v1/billing/" + billingKey))
				.header(HttpHeaders.AUTHORIZATION, basic(properties.getBillingSecretKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			log.error("[Toss] billing pay 실패 orderId={}", orderId, e);
			throw new BusinessException(ErrorCode.TOSS_BILLING_FAILED);
		}
	}

	private String basic(String secret) {
		String token = Base64.getEncoder()
			.encodeToString(((secret == null ? "" : secret) + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}
}
