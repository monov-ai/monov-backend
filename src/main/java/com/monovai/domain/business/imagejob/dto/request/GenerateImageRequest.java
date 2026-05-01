package com.monovai.domain.business.imagejob.dto.request;

import jakarta.validation.constraints.NotNull;

public record GenerateImageRequest(
	@NotNull(message = "추천 ID는 필수입니다.")
	Long recommendationId,

	String aspectRatio,

	String additionalPrompt
) {
}