package com.monovai.domain.business.imagejob.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.monovai.domain.business.edit.entity.ImageEdit;
import com.monovai.domain.business.edit.entity.value.EditParams;
import com.monovai.domain.business.imagejob.entity.ImageJob;
import com.monovai.domain.business.imagejob.entity.ImageJobVariant;
import com.monovai.domain.business.recommendation.entity.value.CorePoints;
import com.monovai.domain.business.recommendation.entity.value.GlobalLock;

public record ImageJobResponse(
	boolean ok,
	JobView job,
	List<EditView> edits
) {
	public record JobView(
		String jobId,
		Long userId,
		String requestId,
		String style,
		String styleLabel,
		String productImageUrl,
		String productImagePath,
		String referenceImageUrl,
		String description,
		CorePoints corePoints,
		String angle,
		String lighting,
		String ratio,
		List<VariantView> variants,
		String status,
		String error,
		Instant createdAt,
		Instant updatedAt
	) {
		public static JobView of(ImageJob j, Map<Long, String> variantUrls) {
			return new JobView(
				j.getJobSlug(),
				j.getUser().getId(),
				j.getRequest().getRequestSlug(),
				j.getStyle().getValue(),
				j.getStyle().getLabel(),
				j.getProductImageUrl(),
				j.getProductImagePath(),
				j.getReferenceImageUrl(),
				j.getDescription(),
				j.getCorePoints(),
				j.getAngle().getValue(),
				j.getLighting().getValue(),
				j.getRatio().getValue(),
				j.getVariants().stream()
					.map(v -> VariantView.of(v, variantUrls.get(v.getId())))
					.toList(),
				j.getStatus().getValue(),
				j.getErrorMessage(),
				j.getCreatedAt() != null ? j.getCreatedAt().toInstant() : null,
				j.getUpdatedAt() != null ? j.getUpdatedAt().toInstant() : null
			);
		}
	}

	public record VariantView(
		String variantId,
		String recommendationId,
		String recommendationTitle,
		String recommendationDescription,
		GlobalLock globalLock,
		String imagePrompt,
		String nanobananaTaskId,
		String resultImageUrl,
		String error,
		String status
	) {
		public static VariantView of(ImageJobVariant v, String resultImageUrl) {
			return new VariantView(
				v.getVariantId(),
				v.getRecommendationId(),
				v.getRecommendationTitle(),
				v.getRecommendationDescription(),
				v.getGlobalLock(),
				v.getImagePrompt(),
				v.getNanobananaTaskId(),
				resultImageUrl,
				v.getErrorMessage(),
				v.getStatus().getValue()
			);
		}
	}

	public record EditView(
		String editId,
		String jobId,
		String baseId,
		String baseSourceKind,
		String baseImageUrl,
		String baseRatio,
		GlobalLock baseGlobalLock,
		String baseRecommendationTitle,
		String mode,
		EditParams params,
		String imagePrompt,
		String nanobananaTaskId,
		String resultImageUrl,
		String error,
		String status,
		Instant createdAt,
		Instant updatedAt
	) {
		public static EditView of(ImageEdit e, String jobSlug, String baseImageUrl, String resultImageUrl) {
			return new EditView(
				e.getEditSlug(),
				jobSlug,
				e.getBaseRef(),
				e.getBaseSourceKind().getValue(),
				baseImageUrl,
				e.getBaseRatio() != null ? e.getBaseRatio().getValue() : null,
				e.getBaseGlobalLock(),
				e.getBaseRecommendationTitle(),
				e.getMode().getValue(),
				e.getParams(),
				e.getImagePrompt(),
				e.getNanobananaTaskId(),
				resultImageUrl,
				e.getErrorMessage(),
				e.getStatus().getValue(),
				e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
				e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
			);
		}
	}

	public static ImageJobResponse of(ImageJob job, List<EditView> editViews, Map<Long, String> variantUrls) {
		return new ImageJobResponse(
			true,
			JobView.of(job, variantUrls),
			editViews
		);
	}
}