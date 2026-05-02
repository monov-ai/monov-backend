package com.monovai.external.nanobanana.dto.response;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Gemini generateContent 응답 본문.
 * 필요한 필드만 매핑 — 사용 안 하는 필드 (usageMetadata 등) 는 ignore 로 흡수.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateContentResponse(
	List<Candidate> candidates
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Candidate(
		Content content,
		String finishReason,
		Integer index
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Content(
		String role,
		List<Part> parts
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Part(
		String text,
		InlineData inlineData
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InlineData(
		String mimeType,
		String data
	) {
	}

	/**
	 * candidates[0] 의 parts 중 inlineData 가 있는 첫 part 의 base64 데이터.
	 */
	public Optional<InlineData> firstInlineImage() {
		if (candidates == null || candidates.isEmpty()) {
			return Optional.empty();
		}
		Candidate first = candidates.get(0);
		if (first == null || first.content() == null || first.content().parts() == null) {
			return Optional.empty();
		}
		return first.content().parts().stream()
			.map(Part::inlineData)
			.filter(d -> d != null && d.data() != null)
			.findFirst();
	}
}