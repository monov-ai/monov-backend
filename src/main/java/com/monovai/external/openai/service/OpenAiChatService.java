package com.monovai.external.openai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.monovai.external.openai.dto.GptRecommendationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiChatService {

	private final ChatClient chatClient;

	/**
	 * Phase 1 — GPT 에 추천 생성 요청.
	 * Spring AI 의 structured output 으로 record 직접 매핑 (.entity 호출).
	 *
	 * @param systemPrompt PromptCompileService 가 합성한 시스템 프롬프트
	 * @param userPrompt   사용자 입력 + 이미지 URL 등을 합친 사용자 메시지
	 * @return 3건의 추천 + corePoints + headline + summary
	 */
	public GptRecommendationResponse generateRecommendations(String systemPrompt, String userPrompt) {
		log.info("[OpenAI] generateRecommendations user prompt length={}", userPrompt.length());

		GptRecommendationResponse response = chatClient.prompt()
			.system(systemPrompt)
			.user(userPrompt)
			.call()
			.entity(GptRecommendationResponse.class);

		log.info("[OpenAI] response received: {} recommendations",
			response.recommendations() != null ? response.recommendations().size() : 0);
		return response;
	}
}
