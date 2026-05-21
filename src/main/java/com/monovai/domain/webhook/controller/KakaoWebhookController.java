package com.monovai.domain.webhook.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.domain.webhook.entity.WebhookFailure;
import com.monovai.domain.webhook.repository.WebhookFailureRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * C.5 Kakao 탈퇴 webhook. event=user.unlink → 해당 socialId 의 monov 계정 비활성화.
 * 실패 케이스는 webhook_failures 에 적재.
 */
@Tag(name = "Webhooks / Kakao", description = "Kakao 연결끊기 webhook")
@RestController
@RequestMapping("/api/v1/webhooks/kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoWebhookController {

	@Value("${kakao.client_id:}")
	private String kakaoAppId;

	private final UserRepository userRepository;
	private final WebhookFailureRepository webhookFailureRepository;

	@PostMapping
	@Operation(summary = "Kakao webhook 수신")
	@Transactional
	public ResponseEntity<Map<String, Object>> receive(@RequestBody Map<String, Object> payload) {
		String eventType = String.valueOf(payload.get("event"));
		try {
			Object appId = payload.get("app_id");
			if (kakaoAppId != null && !kakaoAppId.isBlank() && appId != null
				&& !kakaoAppId.equals(String.valueOf(appId))) {
				record(eventType, payload, "app_id mismatch");
				return ResponseEntity.ok(Map.of("ok", false, "reason", "app_id_mismatch"));
			}

			if ("user.unlink".equalsIgnoreCase(eventType)) {
				Object userId = payload.get("user_id");
				if (userId == null) {
					record(eventType, payload, "missing user_id");
					return ResponseEntity.ok(Map.of("ok", false));
				}
				User user = userRepository
					.findBySocialTypeAndSocialId(SocialType.KAKAO, String.valueOf(userId))
					.orElse(null);
				if (user == null) {
					record(eventType, payload, "user not found: " + userId);
					return ResponseEntity.ok(Map.of("ok", false));
				}
				user.deactivate();
				log.info("[KakaoWebhook] user.unlink 처리 — userId={} deactivated", user.getId());
			}
			return ResponseEntity.ok(Map.of("ok", true));
		} catch (Exception e) {
			log.error("[KakaoWebhook] 처리 실패", e);
			record(eventType, payload, e.getMessage());
			return ResponseEntity.ok(Map.of("ok", false));
		}
	}

	private void record(String eventType, Map<String, Object> payload, String reason) {
		webhookFailureRepository.save(
			WebhookFailure.of("kakao", eventType, String.valueOf(payload), reason));
	}
}
