package com.monovai.domain.business.video.dto.response;

import java.time.Instant;

import com.monovai.domain.business.video.entity.VideoTemplate;

public record VideoJobResponse(
	String videoId,
	Long userId,
	String source,
	String model,
	String mediaType,
	String title,
	String userPrompt,
	String templateId,
	String ratio,
	String sourceImageUrl,
	String sourceImagePath,
	String sourceJobId,
	String sourceItemId,
	String sourceKind,
	String resultMediaUrl,
	String status,
	String error,
	boolean favorite,
	Instant createdAt,
	Instant updatedAt
) {
	public static VideoJobResponse of(VideoTemplate v) {
		return new VideoJobResponse(
			v.getVideoSlug(),
			v.getUser().getId(),
			v.getSource(),
			v.getModel(),
			v.getMediaType().getValue(),
			v.getTitle(),
			v.getUserPrompt(),
			v.getTemplateId(),
			v.getRatio(),
			v.getSourceImageUrl(),
			v.getSourceImagePath(),
			v.getSourceJobId(),
			v.getSourceItemId(),
			v.getSourceKind(),
			v.getResultMediaUrl(),
			v.getStatus().getValue(),
			v.getErrorMessage(),
			v.isFavorite(),
			v.getCreatedAt() != null ? v.getCreatedAt().toInstant() : null,
			v.getUpdatedAt() != null ? v.getUpdatedAt().toInstant() : null
		);
	}
}
