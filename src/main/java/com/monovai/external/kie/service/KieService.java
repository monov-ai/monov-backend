package com.monovai.external.kie.service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.monovai.external.kie.properties.KieProperties;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * KIE.ai Kling 2.6 image-to-video API 호출.
 *
 * 실제 endpoint:
 *  - POST {baseUrl}/v1/jobs        → { jobId } 생성
 *  - GET  {baseUrl}/v1/jobs/{id}   → { status, resultUrl }
 *
 * 동기 sumbit 후 다른 worker 가 poll. 실패 시 BusinessException(KIE_REQUEST_FAILED).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KieService {

	private final KieProperties properties;
	private final RestClient http = RestClient.create();

	public String submitImageToVideo(String imageUrl, String userPrompt, String templatePrompt, String ratio) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("model", properties.getModel());
		payload.put("image_url", imageUrl);
		payload.put("prompt", composePrompt(userPrompt, templatePrompt));
		payload.put("aspect_ratio", ratio == null ? "9:16" : ratio);

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> res = http.post()
				.uri(URI.create(properties.getBaseUrl() + "/v1/jobs"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + nullSafe(properties.getApiKey()))
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.body(Map.class);
			if (res == null || res.get("jobId") == null) {
				throw new BusinessException(ErrorCode.KIE_REQUEST_FAILED);
			}
			return String.valueOf(res.get("jobId"));
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("[KIE/submit] 실패", e);
			throw new BusinessException(ErrorCode.KIE_REQUEST_FAILED);
		}
	}

	public Map<String, Object> pollJob(String jobId) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> res = http.get()
				.uri(URI.create(properties.getBaseUrl() + "/v1/jobs/" + jobId))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + nullSafe(properties.getApiKey()))
				.retrieve()
				.body(Map.class);
			return res == null ? Map.of() : res;
		} catch (Exception e) {
			log.warn("[KIE/poll] 실패 (재시도 대상): {}", e.getMessage());
			return Map.of();
		}
	}

	private static String composePrompt(String userPrompt, String templatePrompt) {
		StringBuilder sb = new StringBuilder();
		if (templatePrompt != null && !templatePrompt.isBlank()) sb.append(templatePrompt).append(". ");
		if (userPrompt != null && !userPrompt.isBlank()) sb.append(userPrompt);
		if (sb.isEmpty()) sb.append("Cinematic camera movement.");
		return sb.toString();
	}

	private static String nullSafe(String s) {
		return s == null ? "" : s;
	}
}
