package com.monovai.domain.business.recommendation.dto.response;

public record RecommendationCreatedResponse(String requestId) {

	public static RecommendationCreatedResponse of(String requestId) {
		return new RecommendationCreatedResponse(requestId);
	}
}