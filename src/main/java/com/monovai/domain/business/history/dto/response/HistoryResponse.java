package com.monovai.domain.business.history.dto.response;

import java.time.Instant;
import java.util.List;

import com.monovai.domain.business.recommendation.entity.value.GlobalLock;

public record HistoryResponse(
	boolean ok,
	List<HistoryItem> jobs
) {
	public static HistoryResponse of(List<HistoryItem> items) {
		return new HistoryResponse(true, items);
	}

	public record HistoryItem(
		String jobId,
		String kind,                  // "imagejob" | "template"
		Long userId,
		String requestId,
		String templateId,
		String templateMediaType,
		String resultMediaUrl,
		String style,
		String styleLabel,
		String description,
		String productImageUrl,
		String angle,
		String lighting,
		String ratio,
		String status,
		Instant createdAt,
		Instant updatedAt,
		List<VariantView> variants,
		List<EditView> edits,
		List<String> favoriteVariantIds,
		List<String> favoriteEditIds
	) {
	}

	public record VariantView(
		String variantId,
		String recommendationId,
		String recommendationTitle,
		String recommendationDescription,
		GlobalLock globalLock,
		String resultImageUrl,
		String error
	) {
	}

	public record EditView(
		String editId,
		String mode,
		GlobalLock baseGlobalLock,
		String baseRecommendationTitle,
		String baseRatio,
		String appliedRatio,
		String resultImageUrl,
		String error,
		String status,
		Instant createdAt
	) {
	}
}
