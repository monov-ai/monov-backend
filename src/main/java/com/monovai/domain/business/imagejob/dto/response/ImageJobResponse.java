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
		List<String> productImageUrls,
		List<String> productImagePaths,
		String referenceImageUrl,
		List<String> referenceImageUrls,
		List<String> referenceImagePaths,
		String fetchableImageUrl,
		List<String> fetchableProductImageUrls,
		List<String> fetchableReferenceImageUrls,
		String description,
		CorePoints corePoints,
		String angle,
		String lighting,
		String ratio,
		List<VariantView> variants,
		List<String> favoriteVariantIds,
		List<String> favoriteEditIds,
		String status,
		String error,
		String source,
		Instant createdAt,
		Instant updatedAt
	) {
		public static JobView of(ImageJob j,
			Map<Long, String> variantUrls,
			String fetchableImageUrl,
			List<String> fetchableProductImageUrls,
			List<String> fetchableReferenceImageUrls
		) {
			return new JobView(
				j.getJobSlug(),
				j.getUser().getId(),
				j.getRequest() == null ? null : j.getRequest().getRequestSlug(),
				j.getStyle() == null ? null : j.getStyle().getValue(),
				j.getStyle() == null ? null : j.getStyle().getLabel(),
				j.getProductImageUrl(),
				j.getProductImagePath(),
				j.getProductImageUrls(),
				j.getProductImagePaths(),
				j.getReferenceImageUrl(),
				j.getReferenceImageUrls(),
				j.getReferenceImagePaths(),
				fetchableImageUrl,
				fetchableProductImageUrls,
				fetchableReferenceImageUrls,
				j.getDescription(),
				j.getCorePoints(),
				j.getAngle() == null ? null : j.getAngle().getValue(),
				j.getLighting() == null ? null : j.getLighting().getValue(),
				j.getRatio().getValue(),
				j.getVariants().stream()
					.map(v -> VariantView.of(v, variantUrls.get(v.getId())))
					.toList(),
				j.getFavoriteVariantIds() != null ? j.getFavoriteVariantIds() : List.of(),
				j.getFavoriteEditIds() != null ? j.getFavoriteEditIds() : List.of(),
				j.getStatus().getValue(),
				j.getErrorMessage(),
				j.getSource() != null ? j.getSource() : "business",
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
		Long userId,
		String jobId,
		String requestId,
		String baseId,
		String baseSourceKind,
		String baseImageUrl,
		String baseRatio,
		GlobalLock baseGlobalLock,
		String baseRecommendationTitle,
		String baseFetchableImageUrl,
		String referenceFetchableImageUrl,
		List<String> referenceFetchableImageUrls,
		String mode,
		EditParams params,
		String imagePrompt,
		boolean compiledByGpt,
		String appliedRatio,
		String nanobananaTaskId,
		String resultImageUrl,
		String error,
		String status,
		String provider,
		Instant createdAt,
		Instant updatedAt
	) {
		public static EditView of(ImageEdit e, String jobSlug, Long userId, String requestId,
			String baseImageUrl, String resultImageUrl,
			String baseFetchableImageUrl, String referenceFetchableImageUrl,
			List<String> referenceFetchableImageUrls
		) {
			return new EditView(
				e.getEditSlug(),
				userId,
				jobSlug,
				requestId,
				e.getBaseRef(),
				e.getBaseSourceKind().getValue(),
				baseImageUrl,
				e.getBaseRatio() != null ? e.getBaseRatio().getValue() : null,
				e.getBaseGlobalLock(),
				e.getBaseRecommendationTitle(),
				baseFetchableImageUrl,
				referenceFetchableImageUrl,
				referenceFetchableImageUrls,
				e.getMode().getValue(),
				e.getParams(),
				e.getImagePrompt(),
				e.getMode() == com.monovai.domain.business.edit.entity.enums.EditMode.BACKGROUND_CHANGE
					|| e.getMode() == com.monovai.domain.business.edit.entity.enums.EditMode.OBJECT_ADD
					|| e.getMode() == com.monovai.domain.business.edit.entity.enums.EditMode.TEXT_CREATE,
				e.getAppliedRatio() != null ? e.getAppliedRatio().getValue()
					: (e.getBaseRatio() != null ? e.getBaseRatio().getValue() : null),
				e.getNanobananaTaskId(),
				resultImageUrl,
				e.getErrorMessage(),
				e.getStatus().getValue(),
				e.getMode() == com.monovai.domain.business.edit.entity.enums.EditMode.INPAINT ? "openai" : "nanobanana",
				e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
				e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null
			);
		}
	}

	public static ImageJobResponse of(ImageJob job, List<EditView> editViews,
		Map<Long, String> variantUrls,
		String fetchableImageUrl,
		List<String> fetchableProductImageUrls,
		List<String> fetchableReferenceImageUrls
	) {
		return new ImageJobResponse(
			true,
			JobView.of(job, variantUrls, fetchableImageUrl, fetchableProductImageUrls, fetchableReferenceImageUrls),
			editViews
		);
	}
}
