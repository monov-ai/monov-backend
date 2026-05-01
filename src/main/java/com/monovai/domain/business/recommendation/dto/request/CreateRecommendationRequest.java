package com.monovai.domain.business.recommendation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecommendationRequest(
	@NotBlank(message = "스타일은 필수입니다.")
	String style,

	@Size(max = 1000, message = "설명은 최대 1000자까지 입력 가능합니다.")
	String description,

	String productImageUrl,
	String productImagePath,

	String referenceImageUrl,
	String referenceImagePath
) {
}
