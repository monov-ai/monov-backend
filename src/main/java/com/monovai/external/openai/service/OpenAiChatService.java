package com.monovai.external.openai.service;

import java.net.URI;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import com.monovai.external.openai.dto.GptRecommendationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiChatService {

	private final ChatClient chatClient;

	/**
	 * Phase 1 — GPT 에 추천 생성 요청 (multimodal 지원).
	 * productImageUrl / referenceImageUrl 가 있으면 첨부 이미지로 함께 전달 → GPT-4V 가 실제 이미지를 분석.
	 *
	 * @param systemPrompt     PromptCompileService 가 합성한 시스템 프롬프트
	 * @param userPrompt       사용자 텍스트 입력
	 * @param productImageUrl  제품 이미지 URL (없으면 null)
	 * @param referenceImageUrl 분위기 참고 이미지 URL (없으면 null)
	 */
	public GptRecommendationResponse generateRecommendations(
		String systemPrompt, String userPrompt,
		String productImageUrl, String referenceImageUrl
	) {
		log.info("[OpenAI] generateRecommendations userPromptLen={}, productImage?={}, referenceImage?={}",
			userPrompt.length(), productImageUrl != null, referenceImageUrl != null);

		GptRecommendationResponse response = chatClient.prompt()
			.system(systemPrompt)
			.user(u -> {
				u.text(userPrompt);
				attachImage(u, productImageUrl, "product");
				attachImage(u, referenceImageUrl, "reference");
			})
			.call()
			.entity(GptRecommendationResponse.class);

		log.info("[OpenAI] response received: {} recommendations",
			response.recommendations() != null ? response.recommendations().size() : 0);
		return response;
	}

	private void attachImage(ChatClient.PromptUserSpec userSpec, String url, String label) {
		if (url == null || url.isBlank()) {
			return;
		}
		try {
			UrlResource resource = new UrlResource(URI.create(url));
			userSpec.media(MimeTypeUtils.IMAGE_JPEG, resource);
			log.info("[OpenAI] attached {} image: {}", label, url.length() > 80 ? url.substring(0, 80) + "..." : url);
		} catch (Exception e) {
			log.warn("[OpenAI] {} 이미지 첨부 실패, 무시하고 진행: {}", label, e.getMessage());
		}
	}
}