package com.monovai.external.nanobanana.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "nanobanana")
public class NanobananaProperties {

	/** Google Generative Language API key (https://aistudio.google.com/apikey). */
	private String apiKey;

	/** Base URL — 기본 Gemini API. Vertex AI 로 옮길 거면 변경. */
	private String baseUrl = "https://generativelanguage.googleapis.com";

	/** 사용 모델. 이미지 생성/편집 가능 모델: gemini-2.5-flash-image-preview 등. */
	private String model = "gemini-2.5-flash-image-preview";
}
