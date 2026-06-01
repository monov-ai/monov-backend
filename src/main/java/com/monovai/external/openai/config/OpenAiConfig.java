package com.monovai.external.openai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

	public static final String DEFAULT_CHAT_MODEL = "gpt-4.1";
	public static final String NEWS_CHAT_MODEL = "gpt-5.4";

	/**
	 * OpenAI ChatClient 빈.
	 * - Spring AI starter 가 application.yml 의 spring.ai.openai.* 키를 읽어
	 *   ChatClient.Builder 를 자동 등록한다.
	 * - 도메인 전반 기본 모델은 gpt-4.1 (functions 와 동기화). 호출 단위로 override 가능.
	 */
	@Bean
	public ChatClient openAiChatClient(ChatClient.Builder builder) {
		return builder
			.defaultOptions(OpenAiChatOptions.builder().model(DEFAULT_CHAT_MODEL).build())
			.build();
	}
}
