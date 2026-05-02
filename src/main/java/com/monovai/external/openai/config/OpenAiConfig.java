package com.monovai.external.openai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

	/**
	 * OpenAI ChatClient 빈.
	 * - Spring AI starter 가 application.yml 의 spring.ai.openai.* 키를 읽어
	 *   ChatClient.Builder 를 자동 등록한다.
	 * - 여기서는 도메인 전반에 공통 적용할 system 프롬프트나 옵션이 있으면 default 로 박아둔다.
	 */
	@Bean
	public ChatClient openAiChatClient(ChatClient.Builder builder) {
		return builder.build();
	}
}
