package com.monovai.domain.business.recommendation.dto.request;

import com.monovai.domain.business.recommendation.entity.enums.Style;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRecommendationRequest(
	@NotNull(message = "스타일은 필수입니다.")
	Style style,

	@Size(max = 1000, message = "설명은 최대 1000자까지 입력 가능합니다.")
	String description,

	String productImageUrl,
	String productImagePath,

	String referenceImageUrl,
	String referenceImagePath
) {
}