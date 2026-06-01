package com.monovai.external.openai.service;

import java.net.URI;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
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

	/** 모델 명시 override 용. null/blank 이면 ChatClient default (gpt-4.1) 사용. */
	private static OpenAiChatOptions opts(String model) {
		if (model == null || model.isBlank()) return null;
		return OpenAiChatOptions.builder().model(model).build();
	}

	/**
	 * Phase 1 — GPT 에 추천 생성 요청 (multimodal 지원).
	 * v2.0: 슬롯당 ≤4장 다중 이미지 첨부 가능.
	 */
	public GptRecommendationResponse generateRecommendations(
		String systemPrompt, String userPrompt,
		List<String> productImageUrls, List<String> referenceImageUrls
	) {
		int productCount = productImageUrls == null ? 0 : productImageUrls.size();
		int referenceCount = referenceImageUrls == null ? 0 : referenceImageUrls.size();
		log.info("[OpenAI] generateRecommendations userPromptLen={}, productImages={}, referenceImages={}",
			userPrompt.length(), productCount, referenceCount);

		GptRecommendationResponse response = chatClient.prompt()
			.system(systemPrompt)
			.user(u -> {
				u.text(userPrompt);
				if (productImageUrls != null) {
					int i = 0;
					for (String url : productImageUrls) attachImage(u, url, "product#" + (++i));
				}
				if (referenceImageUrls != null) {
					int i = 0;
					for (String url : referenceImageUrls) attachImage(u, url, "reference#" + (++i));
				}
			})
			.call()
			.entity(GptRecommendationResponse.class);

		log.info("[OpenAI] response received: {} recommendations",
			response.recommendations() != null ? response.recommendations().size() : 0);
		return response;
	}

	/** v1 호환: 단수형 변환해서 위 메서드로 위임. */
	public GptRecommendationResponse generateRecommendations(
		String systemPrompt, String userPrompt,
		String productImageUrl, String referenceImageUrl
	) {
		return generateRecommendations(systemPrompt, userPrompt,
			productImageUrl == null ? List.of() : List.of(productImageUrl),
			referenceImageUrl == null ? List.of() : List.of(referenceImageUrl)
		);
	}

	/**
	 * 범용 구조화 출력. system/user 프롬프트로 호출 후 지정 타입으로 매핑.
	 */
	public <T> T structured(String systemPrompt, String userPrompt, Class<T> type) {
		return structured(systemPrompt, userPrompt, type, null);
	}

	public <T> T structured(String systemPrompt, String userPrompt, Class<T> type, String modelOverride) {
		ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
			.system(systemPrompt)
			.user(userPrompt);
		OpenAiChatOptions o = opts(modelOverride);
		if (o != null) spec = spec.options(o);
		return spec.call().entity(type);
	}

	/** 자유 텍스트 완성. */
	public String complete(String systemPrompt, String userPrompt) {
		return complete(systemPrompt, userPrompt, null);
	}

	public String complete(String systemPrompt, String userPrompt, String modelOverride) {
		ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
			.system(systemPrompt)
			.user(userPrompt);
		OpenAiChatOptions o = opts(modelOverride);
		if (o != null) spec = spec.options(o);
		return spec.call().content();
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
