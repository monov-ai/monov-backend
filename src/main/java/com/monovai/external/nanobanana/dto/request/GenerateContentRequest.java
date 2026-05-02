package com.monovai.external.nanobanana.dto.request;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Gemini generateContent API 요청 본문.
 * 필드명이 spec 의 camelCase 와 일치하므로 별도 @JsonProperty 불필요.
 *
 * <a href="https://ai.google.dev/api/generate-content">Gemini API 문서</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateContentRequest(
	List<Content> contents,
	GenerationConfig generationConfig
) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Content(
		String role,
		List<Part> parts
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Part(
		String text,
		InlineData inlineData
	) {
		public static Part text(String value) {
			return new Part(value, null);
		}

		public static Part inlineData(byte[] bytes, String mimeType) {
			return new Part(null, new InlineData(mimeType, Base64.getEncoder().encodeToString(bytes)));
		}
	}

	public record InlineData(
		String mimeType,
		String data
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record GenerationConfig(
		List<String> responseModalities
	) {
	}

	// ---------- factory methods ----------

	/**
	 * 텍스트 프롬프트만 — 이미지 생성용.
	 */
	public static GenerateContentRequest textOnly(String prompt) {
		return new GenerateContentRequest(
			List.of(new Content("user", List.of(Part.text(prompt)))),
			new GenerationConfig(List.of("TEXT", "IMAGE"))
		);
	}

	/**
	 * 텍스트 + 입력 이미지 — 이미지 편집/변형용.
	 */
	public static GenerateContentRequest textWithImage(String prompt, byte[] imageBytes, String mimeType) {
		return textWithImages(prompt, List.of(new ImageData(imageBytes, mimeType)));
	}

	/**
	 * 텍스트 + 여러 입력 이미지 — Phase 3 edit 의 base 이미지 + reference 이미지 케이스.
	 */
	public static GenerateContentRequest textWithImages(String prompt, List<ImageData> images) {
		List<Part> parts = new ArrayList<>();
		parts.add(Part.text(prompt));
		for (ImageData img : images) {
			parts.add(Part.inlineData(img.bytes(), img.mimeType()));
		}
		return new GenerateContentRequest(
			List.of(new Content("user", parts)),
			new GenerationConfig(List.of("TEXT", "IMAGE"))
		);
	}

	public record ImageData(byte[] bytes, String mimeType) {
	}
}