package com.monovai.external.nanobanana.config;

import org.springframework.context.annotation.Bean;

import com.monovai.external.nanobanana.properties.NanobananaProperties;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;

/**
 * Nanobanana(Gemini) Feign 전용 설정.
 * - 모든 요청에 x-goog-api-key 헤더 자동 추가.
 *
 * 주의: @Configuration 안 붙임. Spring Cloud OpenFeign 의 @FeignClient(configuration=...) 로
 * 명시 지정해야만 활성화 (전역으로 다른 Feign client 에 새지 않게).
 */
@RequiredArgsConstructor
public class NanobananaFeignConfig {

	private final NanobananaProperties properties;

	@Bean
	public RequestInterceptor nanobananaAuthInterceptor() {
		return template -> template.header("x-goog-api-key", properties.getApiKey());
	}
}