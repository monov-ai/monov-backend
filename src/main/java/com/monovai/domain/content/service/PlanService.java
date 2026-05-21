package com.monovai.domain.content.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.monovai.domain.brandkit.service.BrandKitService;
import com.monovai.external.openai.service.OpenAiChatService;
import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

	private final OpenAiChatService openAiChatService;
	private final BrandKitService brandKitService;

	@SuppressWarnings("unchecked")
	public Map<String, Object> generate(Long userId, Map<String, Object> request) {
		Object brandKit = brandKitService.getBrandKit(userId);
		Object weekStart = request.get("weekStart");

		String system = """
			너는 SNS 마케팅 플래너다. 주어진 브랜드 정보와 주 시작일로 한 주(7일) 콘텐츠 플랜을 만든다.
			출력은 { plan: { posts: [{ day, channel, content, hashtags }] } } 형식의 JSON.
			""";
		String user = "브랜드: " + (brandKit != null ? brandKit.toString() : "(없음)")
			+ "\n주 시작일: " + (weekStart != null ? weekStart.toString() : "")
			+ "\n기타: " + request;

		try {
			Map<String, Object> plan = openAiChatService.structured(system, user, Map.class);
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("ok", true);
			body.put("plan", plan.getOrDefault("plan", plan));
			return body;
		} catch (Exception e) {
			log.error("[Plan] 생성 실패", e);
			throw new BusinessException(ErrorCode.AI_CONTENT_FAILED);
		}
	}
}
