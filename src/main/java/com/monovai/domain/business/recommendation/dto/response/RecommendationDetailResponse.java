package com.monovai.domain.business.recommendation.dto.response;

import java.time.Instant;
import java.util.List;

import com.monovai.domain.business.recommendation.entity.RecommendationRequest;
import com.monovai.domain.business.recommendation.entity.value.CorePoints;
import com.monovai.domain.business.recommendation.entity.value.RecommendationItem;

public record RecommendationDetailResponse(
	String requestId,
	String style,
	String styleLabel,
	InputView input,
	String status,
	String headline,
	String summary,
	CorePoints corePoints,
	List<RecommendationItem> recommendations,
	Instant createdAt,
	Instant updatedAt
) {
	public record InputView(
		String description,
		String productImageUrl,
		String productImagePath,
		String referenceImageUrl,
		String referenceImagePath
	) {
	}

	public static RecommendationDetailResponse of(RecommendationRequest e) {
		return new RecommendationDetailResponse(
			e.getRequestSlug(),
			e.getStyle().getValue(),
			e.getStyle().getLabel(),
			new InputView(
				e.getDescription(),
				e.getProductImageUrl(),
				e.getProductImagePath(),
				e.getReferenceImageUrl(),
				e.getReferenceImagePath()
			),
			e.getStatus().name().toLowerCase(),
			e.getHeadline(),
			e.getSummary(),
			e.getCorePoints(),
			e.getRecommendations(),
			e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
			e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
		);
	}
}